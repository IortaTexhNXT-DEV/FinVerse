package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck.Answer;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheckRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RefundValidationCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's answer to the port {@link RefundValidationSource} (validator {@code CASHIERING}; MKT
 * 1.11.0, ACSL 2.5.5): a refund of a cancelled account opens a validation task for the cashiers
 * ({@code CASH_DISPOSITION}), idempotent on the requester's reference. A cashier confirms it with
 * the unapplied item that holds the returned premium (its AR number is the new AR number unless
 * another is given) or rejects it; the answer goes back as {@code RefundValidationCompleted}.
 */
@Service
@Transactional
public class CashieringRefundValidationSource implements RefundValidationSource {

  /** Entity of the audit trail. */
  public static final String ENTITY = "CashRefundValidation";

  private static final int CANDIDATES = 20;

  private final RefundCheckRepository checks;
  private final UnappliedRepository items;
  private final CashReceiptRepository receipts;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the adapter.
   *
   * @param checks validation tasks
   * @param items unapplied items
   * @param receipts receipts
   * @param numbers document numbers
   * @param notifications notifications
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CashieringRefundValidationSource(
      RefundCheckRepository checks,
      UnappliedRepository items,
      CashReceiptRepository receipts,
      DocumentNumberService numbers,
      NotificationService notifications,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.checks = checks;
    this.items = items;
    this.receipts = receipts;
    this.numbers = numbers;
    this.notifications = notifications;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public String validator() {
    return CASHIERING;
  }

  @Override
  public ValidationTicket open(ValidationRequest request) {
    RefundCheck task =
        checks
            .findBySourceModuleAndSourceRef(request.source().module(), request.source().reference())
            .orElseGet(() -> create(request));
    return new ValidationTicket(
        CASHIERING,
        Status.OPENED,
        task.getTaskNo(),
        "Cashiering validation " + task.getTaskNo() + " opened");
  }

  private RefundCheck create(ValidationRequest request) {
    RefundCheck task =
        checks.save(
            new RefundCheck(
                numbers.next("RVL-" + LocalDate.now(clock).getYear()),
                new RefundCheck.Spec(
                    request.companyId(),
                    request.source().module(),
                    request.source().reference(),
                    request.invoiceNo(),
                    request.arNo(),
                    request.clientCode(),
                    request.currency(),
                    request.amount(),
                    request.source().requestedBy(),
                    request.source().remarks())));
    audit.record(
        ENTITY,
        task.getTaskNo(),
        AuditAction.CREATE,
        "Refund validation for " + task.getSourceModule() + " " + task.getSourceRef());
    notifications.notifyPermission(
        "CASH_DISPOSITION",
        new Notice(
            task.getTaskNo() + ": refund validation requested",
            "Confirm that the premium of "
                + (task.getInvoiceNo() == null ? "the cancelled account" : task.getInvoiceNo())
                + " is back in the unapplied list",
            "/cashiering/requests?tab=VALIDATIONS",
            ENTITY,
            String.valueOf(task.getId())));
    return task;
  }

  /**
   * Validation tasks in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return tasks, newest first
   */
  @Transactional(readOnly = true)
  public Page<RefundCheck> list(
      Long companyId, Collection<RefundCheck.Status> statuses, Pageable pageable) {
    return checks.findByCompanyIdAndStatusInOrderByIdDesc(companyId, statuses, pageable);
  }

  /**
   * Validations waiting for a cashier.
   *
   * @param companyId company
   * @return count
   */
  @Transactional(readOnly = true)
  public long openCount(Long companyId) {
    return checks.countByCompanyIdAndStatus(companyId, RefundCheck.Status.OPEN);
  }

  /**
   * The unapplied items that may hold the returned premium: open items of the invoice or client.
   *
   * @param id task
   * @return items, newest first
   */
  @Transactional(readOnly = true)
  public List<Unapplied> candidates(Long id) {
    RefundCheck task = get(id);
    return items.withBalanceFor(
        task.getCompanyId(),
        task.getInvoiceNo(),
        task.getClientCode(),
        PageRequest.of(0, CANDIDATES));
  }

  /**
   * Confirms a validation: the premium is in the unapplied list.
   *
   * @param id task
   * @param unappliedId unapplied item holding the premium
   * @param newArNo new AR number, null for the AR of the item
   * @param remarks remarks, may be null
   * @return the task
   */
  public RefundCheck confirm(Long id, Long unappliedId, String newArNo, String remarks) {
    RefundCheck task = requireOpen(id);
    Unapplied item =
        Optional.ofNullable(unappliedId)
            .flatMap(items::findById)
            .filter(u -> u.getCompanyId().equals(task.getCompanyId()))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "REFUND_VALIDATION_ITEM_REQUIRED",
                        "Choose the unapplied item that holds the returned premium"));
    String arNo = blankToNull(newArNo);
    if (arNo == null && item.getReceiptId() != null) {
      arNo = receipts.findById(item.getReceiptId()).map(Receipt::getReceiptNo).orElse(null);
    }
    String note = remarks(remarks, "Premium in unapplied item " + item.getReference());
    return decide(task, new Answer(true, item.getId(), arNo, note));
  }

  /**
   * Rejects a validation.
   *
   * @param id task
   * @param remarks reason
   * @return the task
   */
  public RefundCheck reject(Long id, String remarks) {
    if (blankToNull(remarks) == null) {
      throw new BusinessRuleException("REJECT_REASON_REQUIRED", "Enter the reason for rejecting");
    }
    return decide(requireOpen(id), new Answer(false, null, null, remarks.strip()));
  }

  private RefundCheck decide(RefundCheck task, Answer answer) {
    task.decide(answer, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        task.getTaskNo(),
        answer.confirmed() ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        task.getStatus() + ": " + answer.remarks());
    events.publishEvent(
        new RefundValidationCompleted(
            task.getCompanyId(),
            CASHIERING,
            task.getSourceModule(),
            task.getSourceRef(),
            answer.confirmed(),
            answer.newArNo(),
            answer.remarks()));
    return task;
  }

  /**
   * One task.
   *
   * @param id task
   * @return task
   */
  @Transactional(readOnly = true)
  public RefundCheck get(Long id) {
    return checks.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private RefundCheck requireOpen(Long id) {
    RefundCheck task = get(id);
    if (task.getStatus() != RefundCheck.Status.OPEN) {
      throw new BusinessRuleException(
          "REFUND_VALIDATION_DONE", task.getTaskNo() + " is already " + task.getStatus());
    }
    return task;
  }

  private static String remarks(String value, String fallback) {
    String clean = blankToNull(value);
    return clean == null ? fallback : clean;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
