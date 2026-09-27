package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest.Status;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequestRepository;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.PaymentSnapshot;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDisposition;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionTicket;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The requests Collections sends to Cashiering on a collector disposition (BRCLXN.030/032, 040):
 * each goes through the port {@link UnappliedDispositionRequests} with the idempotency key {@code
 * CLX-UPP-<disposition id>}, and its status follows Cashiering's answers ({@code
 * UnappliedDispositionChanged}: ACCEPTED, REJECTED, APPLIED), or a poll of the port for a request
 * handed over while Cashiering had no adapter. The requester is notified of each decision.
 */
@Service
@Transactional
public class ApplicationRequestService {

  /** Source module on the requests. */
  public static final String SOURCE = "COLLECTIONS";

  /** Prefix of the idempotency key. */
  public static final String KEY_PREFIX = "CLX-UPP-";

  static final String ENTITY = "CollectionsUnappliedRequest";

  private final ApplicationRequestRepository requests;
  private final UnappliedDispositionRequests port;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param port Cashiering disposition requests
   * @param notifications notifications
   * @param audit audit trail
   * @param clock clock
   */
  public ApplicationRequestService(
      ApplicationRequestRepository requests,
      UnappliedDispositionRequests port,
      NotificationService notifications,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.port = port;
    this.notifications = notifications;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends the request of a disposition to Cashiering.
   *
   * @param item unapplied item
   * @param d collector disposition (saved)
   * @param assuredName assured of the target invoice, may be null
   * @return the request with Cashiering's first answer
   */
  public ApplicationRequest send(UnappliedView item, UnappliedDisposition d, String assuredName) {
    ApplicationRequest r =
        requests.save(
            new ApplicationRequest(
                d.getCompanyId(),
                new ApplicationRequest.Spec(
                    item.unappliedRef(),
                    d.getCashieringAction(),
                    d.getInvoiceNo(),
                    d.getAmount(),
                    d.getCreatedBy(),
                    clock.instant(),
                    KEY_PREFIX + d.getId()),
                new PaymentSnapshot(
                    item.paymentDate(),
                    item.paymentFileName(),
                    item.transactionNo(),
                    item.amount(),
                    item.currency(),
                    item.paymentType(),
                    item.payor(),
                    item.reference(),
                    assuredName)));
    DispositionTicket ticket =
        port.request(
            new DispositionRequest(
                d.getCompanyId(),
                item.unappliedRef(),
                Action.valueOf(d.getCashieringAction()),
                d.getInvoiceNo(),
                d.getAmount(),
                d.getCreatedBy(),
                SOURCE,
                r.getSourceRef(),
                d.getRemarks()));
    r.answer(statusOf(ticket), ticket.reference(), ticket.message(), clock.instant());
    audit.record(
        ENTITY,
        r.getSourceRef(),
        AuditAction.SUBMIT,
        r.getAction() + " of " + r.getUnappliedRef() + ": " + r.getStatus());
    return r;
  }

  /**
   * Cashiering decided a request (in its transaction).
   *
   * @param event decision
   */
  @EventListener
  public void on(UnappliedDispositionChanged event) {
    Optional<Status> next = eventStatus(event.status());
    if (SOURCE.equals(event.source()) && next.isPresent()) {
      requests.findBySourceRef(event.sourceRef()).ifPresent(r -> answered(r, next.get(), event));
    }
  }

  private void answered(ApplicationRequest r, Status next, UnappliedDispositionChanged event) {
    r.answer(next, event.cashieringRef(), event.message(), clock.instant());
    audit.record(ENTITY, r.getSourceRef(), AuditAction.UPDATE, "Cashiering: " + next);
    notifications.notifyUser(
        r.getRequestedBy(),
        new Notice(
            r.getUnappliedRef()
                + " "
                + r.getAction().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " "
                + next.name().toLowerCase(Locale.ROOT),
            event.message() == null ? "Cashiering decided the request" : event.message(),
            "/collections/unapplied/" + r.getUnappliedRef(),
            ENTITY,
            r.getId().toString()));
  }

  /**
   * Asks Cashiering again where a request stands (a hand-off done by hand, a missed answer).
   *
   * @param id request
   * @return the request
   */
  public ApplicationRequest refresh(Long id) {
    ApplicationRequest r = get(id);
    if (r.getStatus().isOpen()) {
      port.status(SOURCE, r.getSourceRef())
          .ifPresent(
              t -> {
                Status next = statusOf(t);
                Status kept =
                    next == Status.SENT && r.getStatus() == Status.ACCEPTED
                        ? Status.ACCEPTED
                        : next;
                r.answer(kept, t.reference(), t.message(), clock.instant());
              });
    }
    return r;
  }

  /**
   * Requests in some statuses (the status view).
   *
   * @param companyId company
   * @param statuses statuses
   * @param text item, invoice, Cashiering reference or requester, may be null
   * @param pageable page
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public Page<ApplicationRequest> list(
      Long companyId, Collection<Status> statuses, String text, Pageable pageable) {
    String like =
        text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    return requests.search(companyId, statuses, like, pageable);
  }

  /**
   * Requests of an item.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public List<ApplicationRequest> ofItem(Long companyId, String unappliedRef) {
    return requests.findByCompanyIdAndUnappliedRefOrderByIdDesc(companyId, unappliedRef);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @Transactional(readOnly = true)
  public ApplicationRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private static Status statusOf(DispositionTicket ticket) {
    return switch (ticket.status()) {
      case SUBMITTED -> Status.SENT;
      case DEFERRED -> Status.DEFERRED;
      case REJECTED -> Status.REJECTED;
    };
  }

  private static Optional<Status> eventStatus(String status) {
    return switch (status == null ? "" : status) {
      case "ACCEPTED" -> Optional.of(Status.ACCEPTED);
      case "REJECTED" -> Optional.of(Status.REJECTED);
      case "APPLIED" -> Optional.of(Status.APPLIED);
      default -> Optional.empty();
    };
  }
}
