package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest.Spec;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the port {@link UnappliedDispositionRequests} (BRCLXN.030-033;
 * COLLECTIONS_DESIGN 9): a collector's request to apply, refund, reclass or transfer an unapplied
 * item is checked against the item and queued ({@code CRQ-<yyyy>}) for the cashiers ({@code
 * CASH_DISPOSITION}), who act on it through {@link CollectorRequestService}. Refused at once when
 * the item is unknown or has no balance, the amount is above the balance or an application has no
 * invoice. Idempotent on (source, source reference).
 */
@Service
@Transactional
public class CashieringDispositionRequests implements UnappliedDispositionRequests {

  private final CollectorRequestRepository requests;
  private final UnappliedRepository items;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the adapter.
   *
   * @param requests collector requests
   * @param items unapplied items
   * @param numbers document numbers
   * @param notifications notifications
   * @param audit audit trail
   * @param clock clock
   */
  public CashieringDispositionRequests(
      CollectorRequestRepository requests,
      UnappliedRepository items,
      DocumentNumberService numbers,
      NotificationService notifications,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.items = items;
    this.numbers = numbers;
    this.notifications = notifications;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public DispositionTicket request(DispositionRequest request) {
    Optional<CollectorRequest> earlier =
        requests.findBySourceAndSourceRef(request.source(), request.sourceRef());
    if (earlier.isPresent()) {
      return ticket(earlier.get());
    }
    Optional<Unapplied> found =
        items
            .findByReference(request.unappliedRef())
            .filter(u -> u.getCompanyId().equals(request.companyId()));
    Optional<String> refusal = refusal(found, request);
    if (refusal.isPresent()) {
      return new DispositionTicket(Status.REJECTED, null, refusal.get());
    }
    Unapplied item = found.orElseThrow();
    CollectorRequest saved =
        requests.save(
            new CollectorRequest(
                numbers.next("CRQ-" + LocalDate.now(clock).getYear()),
                item.getId(),
                new Spec(
                    request.companyId(),
                    request.action().name(),
                    blankToNull(request.invoiceNo()),
                    request.amount(),
                    request.requestedBy(),
                    request.source(),
                    request.sourceRef(),
                    blankToNull(request.remarks()))));
    audit.record(
        CollectorRequestService.ENTITY,
        saved.getRequestNo(),
        AuditAction.CREATE,
        request.action() + " of " + item.getReference() + " requested by " + request.requestedBy());
    notifications.notifyPermission(
        "CASH_DISPOSITION",
        new Notice(
            saved.getRequestNo() + ": " + request.action() + " requested by a collector",
            item.getReference()
                + (saved.getInvoiceNo() == null ? "" : " to invoice " + saved.getInvoiceNo()),
            "/cashiering/requests",
            CollectorRequestService.ENTITY,
            saved.getId().toString()));
    return ticket(saved);
  }

  private static Optional<String> refusal(Optional<Unapplied> found, DispositionRequest request) {
    if (found.isEmpty()) {
      return Optional.of("Unknown unapplied item " + request.unappliedRef());
    }
    Unapplied item = found.get();
    String refusal = null;
    if (item.getBalance().signum() <= 0) {
      refusal = item.getReference() + " has no unapplied balance left";
    } else if (request.amount() != null
        && (request.amount().signum() <= 0 || request.amount().compareTo(item.getBalance()) > 0)) {
      refusal = "The amount must be above zero and at most the balance " + item.getBalance();
    } else if (request.action() == Action.APPLY_TO_INVOICE
        && blankToNull(request.invoiceNo()) == null) {
      refusal = "An application request needs the invoice number";
    }
    return Optional.ofNullable(refusal);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DispositionTicket> status(String source, String sourceRef) {
    return requests
        .findBySourceAndSourceRef(source, sourceRef)
        .map(CashieringDispositionRequests::ticket);
  }

  private static DispositionTicket ticket(CollectorRequest r) {
    String message =
        switch (r.getStatus()) {
          case QUEUED -> "Queued for Cashiering as " + r.getRequestNo();
          case ACCEPTED -> "Accepted by Cashiering (" + r.getDecidedBy() + ")";
          case APPLIED -> text(r.getDecisionNote(), "Executed by Cashiering");
          case REJECTED -> "Rejected by Cashiering: " + text(r.getDecisionNote(), "no reason");
        };
    Status status =
        r.getStatus() == CollectorRequest.Status.REJECTED ? Status.REJECTED : Status.SUBMITTED;
    return new DispositionTicket(status, r.getRequestNo(), message);
  }

  private static String text(String value, String fallback) {
    return value == null ? fallback : value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
