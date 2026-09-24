package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequestRepository;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The in-app Disbursement queue (default {@code DisbursementGateway}, OQ02): Operations modules
 * send payment requests; Disbursement users ({@code DISB_PROCESS}) acknowledge them, assign the DV
 * number, mark them paid or return them with a reason (RMTID.019, DBMID.001). Each change is
 * audited and published as {@link DisbursementStatusChanged} for the source module.
 */
@Service
@Transactional
public class DisbursementQueueService {

  /** Permission of the Disbursement users. */
  public static final String PERMISSION = "DISB_PROCESS";

  private static final String ENTITY = "DisbursementRequest";
  private static final String LINK = "/operations/disbursements";

  private final DisbursementRequestRepository requests;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param numbers request numbers
   * @param notifications notifications
   * @param events event publisher
   * @param audit audit trail
   * @param clock clock
   */
  public DisbursementQueueService(
      DisbursementRequestRepository requests,
      DocumentNumberService numbers,
      NotificationService notifications,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.numbers = numbers;
    this.notifications = notifications;
    this.events = events;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Queues a payment request, idempotent on (source module, source reference).
   *
   * @param companyId company
   * @param spec what to pay
   * @return the request
   */
  public DisbursementRequest send(Long companyId, DisbursementRequest.Spec spec) {
    Optional<DisbursementRequest> earlier =
        requests.findBySourceModuleAndSourceRef(spec.sourceModule(), spec.sourceRef());
    if (earlier.isPresent()) {
      return earlier.get();
    }
    if (!Money.isPositive(spec.amount())) {
      throw new BusinessRuleException(
          "DISBURSEMENT_AMOUNT", "A payment request needs a positive amount");
    }
    DisbursementRequest saved =
        requests.save(
            new DisbursementRequest(
                companyId,
                numbers.next("DSQ-" + LocalDate.now(clock).getYear()),
                spec,
                clock.instant()));
    audit.record(
        ENTITY,
        saved.getRequestNo(),
        AuditAction.CREATE,
        spec.type()
            + " "
            + spec.currency()
            + " "
            + spec.amount()
            + " to "
            + spec.payeeCode()
            + " from "
            + spec.sourceModule()
            + " "
            + spec.sourceRef());
    notifications.notifyPermission(
        PERMISSION,
        new Notice(
            "Payment request " + saved.getRequestNo(),
            spec.description(),
            LINK,
            ENTITY,
            saved.getRequestNo()));
    return saved;
  }

  /**
   * Acknowledges receipt.
   *
   * @param id request
   * @return the request
   */
  public DisbursementRequest acknowledge(Long id) {
    DisbursementRequest r = get(id);
    r.acknowledge(clock.instant());
    return changed(r, "Acknowledged");
  }

  /**
   * Assigns the disbursement voucher number.
   *
   * @param id request
   * @param dvNo DV number
   * @return the request
   */
  public DisbursementRequest assignDv(Long id, String dvNo) {
    DisbursementRequest r = get(id);
    r.assignDv(dvNo.strip(), clock.instant());
    return changed(r, "DV " + r.getDvNo() + " assigned");
  }

  /**
   * Marks the payment released.
   *
   * @param id request
   * @return the request
   */
  public DisbursementRequest markPaid(Long id) {
    DisbursementRequest r = get(id);
    r.markPaid(clock.instant());
    return changed(r, "Paid");
  }

  /**
   * Returns a request to its source with a reason.
   *
   * @param id request
   * @param reason reason
   * @return the request
   */
  public DisbursementRequest returnToSource(Long id, String reason) {
    DisbursementRequest r = get(id);
    r.returnToSource(reason.strip(), clock.instant());
    return changed(r, "Returned: " + r.getReturnReason());
  }

  private DisbursementRequest changed(DisbursementRequest r, String summary) {
    audit.record(ENTITY, r.getRequestNo(), AuditAction.UPDATE, summary);
    events.publishEvent(
        new DisbursementStatusChanged(
            r.getCompanyId(),
            r.getRequestNo(),
            r.getRequestType(),
            r.getSourceModule(),
            r.getSourceRef(),
            r.getStatus(),
            r.getDvNo(),
            r.getReturnReason()));
    notifications.notifyUser(
        r.getCreatedBy(),
        new Notice(
            "Payment request " + r.getRequestNo() + " " + r.getStatus(),
            summary,
            LINK,
            ENTITY,
            r.getRequestNo()),
        "OPS_DISBURSEMENT_STATUS");
    return r;
  }

  /**
   * A request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public DisbursementRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * The request of a source transaction.
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return request
   */
  @Transactional(readOnly = true)
  public Optional<DisbursementRequest> find(String sourceModule, String sourceRef) {
    return requests.findBySourceModuleAndSourceRef(sourceModule, sourceRef);
  }

  /**
   * Requests of a company with some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses (empty = all)
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<DisbursementRequest> list(
      Long companyId, List<DisbursementRequest.Status> statuses, Pageable pageable) {
    List<DisbursementRequest.Status> filter =
        statuses == null || statuses.isEmpty()
            ? List.of(DisbursementRequest.Status.values())
            : statuses;
    return requests.findByCompanyIdAndStatusInOrderByIdDesc(companyId, filter, pageable);
  }
}
