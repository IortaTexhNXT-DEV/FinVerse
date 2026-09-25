package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRule;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRuleRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The unapplied payments workbench (CSHID.024/025, OQ15): assign a disposition to an unapplied
 * item, update it before it is processed, submit it (types that need the team leader's approval go
 * to For Approval, the others are processed at once), approve it with four eyes, and mark a
 * completed disposition for reversal with a reason. Tabs follow the stages of the workflow {@code
 * OPS_DISPOSITION}.
 */
@Service
@Transactional
public class DispositionService {

  /** Unapplied tab. */
  public static final List<String> TAB_UNAPPLIED = List.of(Unapplied.STAGE_INITIAL);

  /** Monitoring tab. */
  private static final String MONITORING = "MONITORING";

  /** Stages of the Monitoring tab. */
  public static final List<String> TAB_MONITORING = List.of(MONITORING, "IN_PROCESS");

  /** For Approval tab. */
  public static final List<String> TAB_FOR_APPROVAL = List.of("FOR_APPROVAL");

  /** For Reversal tab. */
  public static final List<String> TAB_FOR_REVERSAL = List.of("FOR_REVERSAL");

  /** Completed and closed items. */
  public static final List<String> TAB_DONE = List.of("COMPLETED", "CLOSED");

  private static final String LOV = "DISPOSITION_TYPE";
  private static final String APPROVE = "CASH_DISPOSITION_APPROVE";
  private static final Collection<DispositionStatus> CURRENT =
      EnumSet.of(
          DispositionStatus.MONITORING,
          DispositionStatus.FOR_APPROVAL,
          DispositionStatus.IN_PROCESS,
          DispositionStatus.COMPLETED,
          DispositionStatus.FOR_REVERSAL);

  private final UnappliedRepository items;
  private final DispositionRepository dispositions;
  private final DispositionTypeRuleRepository rules;
  private final DispositionExecutor executor;
  private final WorkflowService workflow;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items unapplied items
   * @param dispositions dispositions
   * @param rules disposition type rules
   * @param executor execution of dispositions
   * @param workflow workflow
   * @param lovs lists of values
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DispositionService(
      UnappliedRepository items,
      DispositionRepository dispositions,
      DispositionTypeRuleRepository rules,
      DispositionExecutor executor,
      WorkflowService workflow,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.items = items;
    this.dispositions = dispositions;
    this.rules = rules;
    this.executor = executor;
    this.workflow = workflow;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Items of a tab.
   *
   * @param companyId company
   * @param stages stages of the tab
   * @param text search text, may be null
   * @param pageable page
   * @return items, newest first
   */
  @Transactional(readOnly = true)
  public Page<Unapplied> list(Long companyId, List<String> stages, String text, Pageable pageable) {
    String like =
        text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    return items.search(companyId, stages, like, pageable);
  }

  /**
   * Dispositions of an item.
   *
   * @param unappliedId item
   * @return dispositions, oldest first
   */
  @Transactional(readOnly = true)
  public List<Disposition> history(Long unappliedId) {
    return dispositions.findByUnappliedIdOrderByIdAsc(unappliedId);
  }

  /**
   * The disposition types with their action and approval rule.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<DispositionTypeRule> types() {
    return rules.findAll();
  }

  /**
   * Assigns a disposition (Unapplied to Monitoring).
   *
   * @param unappliedId item
   * @param typeCode disposition type
   * @param details amount and target fields
   * @return the disposition
   */
  public Disposition assign(Long unappliedId, String typeCode, DispositionDetails details) {
    Unapplied item = item(unappliedId);
    requireStage(item, Unapplied.STAGE_INITIAL);
    DispositionTypeRule rule = rule(typeCode);
    executor.validate(item, rule, details);
    Disposition d = dispositions.save(new Disposition(item.getId(), rule, details));
    move(item, "assign_disposition", rule.getTypeCode());
    audit.record(
        UnappliedService.ENTITY,
        item.getReference(),
        AuditAction.UPDATE,
        "Disposition " + typeCode);
    return d;
  }

  /**
   * Changes the disposition before it is processed.
   *
   * @param unappliedId item
   * @param typeCode disposition type
   * @param details amount and target fields
   * @return the disposition
   */
  public Disposition update(Long unappliedId, String typeCode, DispositionDetails details) {
    Unapplied item = item(unappliedId);
    requireStage(item, MONITORING);
    DispositionTypeRule rule = rule(typeCode);
    executor.validate(item, rule, details);
    Disposition d = current(item);
    d.update(rule, details);
    move(item, "update", rule.getTypeCode());
    return d;
  }

  /**
   * Submits the disposition: to For Approval when its type needs approval, else processed now.
   *
   * @param unappliedId item
   * @return the disposition
   */
  public Disposition submit(Long unappliedId) {
    Unapplied item = item(unappliedId);
    requireStage(item, MONITORING);
    Disposition d = current(item);
    if (rule(d.getDispositionType()).isRequiresApproval()) {
      d.markStatus(DispositionStatus.FOR_APPROVAL);
      move(item, "submit", null);
      notifications.notifyPermission(
          APPROVE,
          new Notice(
              item.getReference() + " disposition for approval",
              d.getDispositionType() + " " + item.getCurrency() + " " + d.getAmount(),
              "/cashiering/unapplied/" + item.getId(),
              UnappliedService.ENTITY,
              item.getId().toString()),
          "CASH_APPROVAL_REQUEST");
      return d;
    }
    executor.execute(item, d);
    workflow.systemTransition(
        UnappliedService.ENTITY, item.getId().toString(), "complete", TransitionNote.NONE);
    reopenIfBalanceLeft(item);
    return d;
  }

  /**
   * Approves and processes a disposition (four eyes).
   *
   * @param unappliedId item
   * @return the disposition
   */
  public Disposition approve(Long unappliedId) {
    Unapplied item = item(unappliedId);
    requireStage(item, "FOR_APPROVAL");
    Disposition d = current(item);
    requireChecker(d.getCreatedBy(), d.getUpdatedBy());
    d.approve(currentUser.username(), clock.instant());
    move(item, "approve", null);
    executor.execute(item, d);
    workflow.systemTransition(
        UnappliedService.ENTITY, item.getId().toString(), "complete", TransitionNote.NONE);
    reopenIfBalanceLeft(item);
    return d;
  }

  /**
   * Withdraws a disposition not yet submitted (back to Unapplied).
   *
   * @param unappliedId item
   * @return the item
   */
  public Unapplied withdraw(Long unappliedId) {
    Unapplied item = item(unappliedId);
    requireStage(item, MONITORING);
    current(item).markStatus(DispositionStatus.WITHDRAWN);
    move(item, "withdraw", null);
    return item;
  }

  /**
   * Marks a completed disposition for reversal (CSHID.025 For Reversal tab).
   *
   * @param unappliedId item
   * @param reason reason
   * @return the disposition
   */
  public Disposition markReversal(Long unappliedId, String reason) {
    Unapplied item = item(unappliedId);
    requireStage(item, "COMPLETED");
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "REVERSAL_REASON_REQUIRED", "Enter the reason for the reversal");
    }
    Disposition d = current(item);
    d.requestReversal(reason, currentUser.username());
    workflow.transition(
        UnappliedService.ENTITY,
        item.getId().toString(),
        "mark_reversal",
        TransitionNote.comment(reason));
    return d;
  }

  /**
   * Approves a reversal: the disposition is undone and the item is back in Unapplied.
   *
   * @param unappliedId item
   * @return the disposition
   */
  public Disposition approveReversal(Long unappliedId) {
    Unapplied item = item(unappliedId);
    requireStage(item, "FOR_REVERSAL");
    Disposition d = current(item);
    requireChecker(d.getReversalRequestedBy(), null);
    executor.reverse(item, d);
    d.markStatus(DispositionStatus.REVERSED);
    move(item, "approve_reversal", null);
    return d;
  }

  private void reopenIfBalanceLeft(Unapplied item) {
    if (item.getBalance().signum() > 0) {
      workflow.systemTransition(
          UnappliedService.ENTITY,
          item.getId().toString(),
          "reopen",
          TransitionNote.comment("Balance " + item.getBalance() + " back to Unapplied"));
    }
  }

  private void move(Unapplied item, String action, String comment) {
    workflow.transition(
        UnappliedService.ENTITY, item.getId().toString(), action, TransitionNote.comment(comment));
  }

  private void requireChecker(String maker, String lastMaker) {
    String me = currentUser.username();
    if (CurrentUser.sameUser(me, maker) || CurrentUser.sameUser(me, lastMaker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "The requester cannot approve the disposition");
    }
  }

  private DispositionTypeRule rule(String typeCode) {
    lovs.requireValid(LOV, typeCode, LocalDate.now(clock));
    return rules
        .findById(typeCode)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "DISPOSITION_TYPE_NOT_CONFIGURED",
                    "Disposition type " + typeCode + " has no processing rule (OQ15)"));
  }

  private Disposition current(Unapplied item) {
    return dispositions
        .findFirstByUnappliedIdAndStatusInOrderByIdDesc(item.getId(), CURRENT)
        .orElseThrow(() -> new ResourceNotFoundException("Disposition of", item.getReference()));
  }

  private Unapplied item(Long id) {
    return items
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(UnappliedService.ENTITY, id));
  }

  private static void requireStage(Unapplied item, String stage) {
    if (!stage.equals(item.getStage())) {
      throw new BusinessRuleException(
          "UNAPPLIED_WRONG_STAGE",
          item.getReference() + " is " + item.getStage() + ", not " + stage);
    }
  }
}
