package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptActionType;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.Reason;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.ReinstatementFields;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancellation and reinstatement of receipts (CSHID.001-005): a transaction with its own number
 * ({@code CAN-} / {@code RIN-}), a mandatory reason from the maintained lists (Others needs the
 * text), the encoded reinstatement fields per group, and the checker's approval (workflow {@code
 * OPS_RECEIPT_ACTION}, OQ06) that posts it. The checker cannot be the requester.
 */
@Service
@Transactional
public class ReceiptActionService {

  /** Entity type of the work case. */
  public static final String ENTITY = "ReceiptAction";

  private static final String WORKFLOW = "OPS_RECEIPT_ACTION";
  private static final String CANCEL_REASON = "RECEIPT_CANCEL_REASON";
  private static final String REINSTATE_REASON = "REINSTATEMENT_REASON";
  private static final String BOUNCED_CHECK = "GEN_BOUNCED_CHECK";
  private static final String FOR_APPROVAL = "FOR_APPROVAL";
  private static final List<String> OPEN_STAGES = List.of("REQUESTED", FOR_APPROVAL);

  private final ReceiptActionRepository actions;
  private final CashReceiptRepository receipts;
  private final ReceiptReversalService reversal;
  private final LovService lovs;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param actions actions
   * @param receipts receipts
   * @param reversal cancellation and reinstatement postings
   * @param lovs lists of values
   * @param workflow workflow
   * @param numbers document numbers
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptActionService(
      ReceiptActionRepository actions,
      CashReceiptRepository receipts,
      ReceiptReversalService reversal,
      LovService lovs,
      WorkflowService workflow,
      DocumentNumberService numbers,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.actions = actions;
    this.receipts = receipts;
    this.reversal = reversal;
    this.lovs = lovs;
    this.workflow = workflow;
    this.numbers = numbers;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests a cancellation and submits it for approval (CSHID.001/003).
   *
   * @param receiptId receipt
   * @param reason reason code and text
   * @return the action
   */
  public ReceiptAction requestCancel(Long receiptId, Reason reason) {
    Receipt receipt = receipt(receiptId);
    if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_ALREADY_CANCELLED",
          "Receipt " + receipt.getReceiptNo() + " is already cancelled");
    }
    requireNoOpenAction(receipt);
    requireReason(CANCEL_REASON, reason);
    reversal.requireCancellable(receipt);
    ReceiptAction action =
        actions.save(
            new ReceiptAction(
                receipt,
                numbers.next("CAN-" + LocalDate.now(clock).getYear()),
                ReceiptActionType.CANCEL,
                reason,
                receipt.liveAmount()));
    return open(action, receipt, "Cancellation");
  }

  /**
   * Requests a full or partial reinstatement and submits it for approval (CSHID.001/004/005).
   *
   * @param receiptId cancelled receipt
   * @param request full or partial, amount, reason and encoded fields
   * @return the action
   */
  public ReceiptAction requestReinstatement(Long receiptId, ReinstateRequest request) {
    Receipt receipt = receipt(receiptId);
    if (receipt.getStatus() != ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_NOT_CANCELLED", "Only a cancelled receipt can be reinstated");
    }
    requireNoOpenAction(receipt);
    LovValue reason = requireReason(REINSTATE_REASON, request.reason());
    BigDecimal amount = ReinstatementRules.amount(receipt, request.full(), request.amount());
    if (!bouncedCheck(receipt)) {
      ReinstatementRules.requireFields(reason.getParentCode(), request.fields(), receipt);
    }
    ReceiptAction action =
        new ReceiptAction(
            receipt,
            numbers.next("RIN-" + LocalDate.now(clock).getYear()),
            request.full() ? ReceiptActionType.REINSTATE_FULL : ReceiptActionType.REINSTATE_PARTIAL,
            request.reason(),
            amount);
    if (request.fields() != null) {
      action.encode(request.fields());
    }
    return open(actions.save(action), receipt, "Reinstatement");
  }

  /**
   * Submits a returned action again.
   *
   * @param id action
   * @return the action
   */
  public ReceiptAction resubmit(Long id) {
    ReceiptAction action = get(id);
    workflow.transition(ENTITY, id.toString(), "submit", TransitionNote.NONE);
    notifyApprovers(action);
    return action;
  }

  /**
   * Approves and posts an action (checker, not the requester).
   *
   * @param id action
   * @return the action
   */
  public ReceiptAction approve(Long id) {
    ReceiptAction action = get(id);
    if (!FOR_APPROVAL.equals(action.getStage())) {
      throw new BusinessRuleException(
          "RECEIPT_ACTION_NOT_FOR_APPROVAL",
          action.getTransactionNo() + " is " + action.getStage());
    }
    String approver = currentUser.username();
    if (CurrentUser.sameUser(approver, action.getCreatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "The requester cannot approve " + action.getTransactionNo());
    }
    Receipt receipt = receipt(action.getReceiptId());
    String batch =
        action.isReinstatement()
            ? reversal.reinstate(receipt, action)
            : reversal.cancel(receipt, action);
    action.approved(approver, clock.instant(), batch);
    workflow.transition(ENTITY, id.toString(), "approve", TransitionNote.NONE);
    audit.record(
        CashReceiptService.ENTITY,
        receipt.getReceiptNo(),
        AuditAction.POST,
        action.getAction() + " " + action.getTransactionNo() + " approved by " + approver);
    return action;
  }

  /**
   * One action.
   *
   * @param id id
   * @return action
   */
  @Transactional(readOnly = true)
  public ReceiptAction get(Long id) {
    return actions.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Actions of a receipt.
   *
   * @param receiptId receipt
   * @return actions, oldest first
   */
  @Transactional(readOnly = true)
  public List<ReceiptAction> ofReceipt(Long receiptId) {
    return actions.findByReceiptIdOrderByIdAsc(receiptId);
  }

  /**
   * Actions of a company in some stages (approval queue).
   *
   * @param companyId company
   * @param stages stages
   * @param pageable page
   * @return actions, newest first
   */
  @Transactional(readOnly = true)
  public Page<ReceiptAction> list(Long companyId, List<String> stages, Pageable pageable) {
    return actions.findByCompanyIdAndStageInOrderByIdDesc(companyId, stages, pageable);
  }

  private ReceiptAction open(ReceiptAction action, Receipt receipt, String what) {
    workflow.start(
        new StartCase(
            receipt.getCompanyId(),
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                action.getId().toString(),
                action.getTransactionNo(),
                what + " of " + receipt.getReceiptNo() + " - " + receipt.getPayorName(),
                "/cashiering/receipts/" + receipt.getId(),
                null),
            null));
    workflow.transition(ENTITY, action.getId().toString(), "submit", TransitionNote.NONE);
    audit.record(
        CashReceiptService.ENTITY,
        receipt.getReceiptNo(),
        AuditAction.SUBMIT,
        what + " " + action.getTransactionNo() + " requested: " + action.getReasonCode());
    notifyApprovers(action);
    return action;
  }

  private void notifyApprovers(ReceiptAction action) {
    notifications.notifyPermission(
        "CASH_APPROVE",
        new Notice(
            action.getTransactionNo() + " for approval",
            action.getAction() + " of receipt, reason " + action.getReasonCode(),
            "/cashiering/receipts/" + action.getReceiptId(),
            ENTITY,
            action.getId().toString()),
        "CASH_APPROVAL_REQUEST");
  }

  private LovValue requireReason(String lov, Reason reason) {
    if (reason == null || reason.code() == null || reason.code().isBlank()) {
      throw new BusinessRuleException("RECEIPT_REASON_REQUIRED", "Select a reason");
    }
    LovValue value = lovs.requireValid(lov, reason.code(), LocalDate.now(clock));
    if (reason.code().endsWith("_OTHERS") && (reason.text() == null || reason.text().isBlank())) {
      throw new BusinessRuleException(
          "RECEIPT_REASON_TEXT_REQUIRED", "Specify the reason when 'Others' is selected");
    }
    return value;
  }

  private void requireNoOpenAction(Receipt receipt) {
    if (actions.existsByReceiptIdAndStageIn(receipt.getId(), OPEN_STAGES)) {
      throw new BusinessRuleException(
          "RECEIPT_ACTION_PENDING",
          "Receipt " + receipt.getReceiptNo() + " already has a request waiting for approval");
    }
  }

  private boolean bouncedCheck(Receipt receipt) {
    List<ReceiptAction> history = actions.findByReceiptIdOrderByIdAsc(receipt.getId());
    return history.stream()
        .filter(a -> a.getAction() == ReceiptActionType.CANCEL)
        .reduce((a, b) -> b)
        .map(a -> BOUNCED_CHECK.equals(a.getReasonCode()))
        .orElse(false);
  }

  private Receipt receipt(Long id) {
    return receipts
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(CashReceiptService.ENTITY, id));
  }

  /**
   * A reinstatement request.
   *
   * @param full full reinstatement (the whole receipt amount)
   * @param amount amount of a partial reinstatement
   * @param reason reason code and text (LOV REINSTATEMENT_REASON)
   * @param fields encoded fields (CSHID.005)
   */
  public record ReinstateRequest(
      boolean full, BigDecimal amount, Reason reason, ReinstatementFields fields) {}
}
