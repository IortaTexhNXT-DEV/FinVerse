package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Origin;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceRef;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marketing hold requests (MKTID.002-007, RMTID.021/031, OPS_HOLD): create, submit, approve (four
 * eyes, HOLD_APPROVE), assign to a remittance processor, extend, cancel with approval, release and
 * expire. A submitted request marks the invoice REQUESTED_FOR_HOLD; an approved one sets the
 * invoice's {@code HOLD} flag so the extraction skips it (RMTID.020/031); release or expiry clears
 * the flag and the invoice becomes eligible again (MKTID.002, RMTID.021).
 */
@Service
@Transactional
public class HoldService {

  /** Entity type in the workflow. */
  public static final String ENTITY = "RemittanceHold";

  /** Approver permission. */
  public static final String APPROVER = "HOLD_APPROVE";

  private static final String WORKFLOW = "OPS_HOLD";
  private static final String REASON_LOV = "HOLD_REASON";
  private static final String HOLD = "Hold ";
  private static final String EXPIRING = "HOLD_EXPIRING";
  private static final Set<HoldStage> LIVE =
      Set.copyOf(Arrays.stream(HoldStage.values()).filter(HoldStage::isLive).toList());
  private static final Set<RemittanceStatus> HOLDABLE =
      Set.of(
          RemittanceStatus.UNPROCESSED,
          RemittanceStatus.WITH_OUTSTANDING_BALANCE,
          RemittanceStatus.PARTIALLY_REMITTED);

  private final HoldRequestRepository holds;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final UserDirectory users;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param holds hold requests
   * @param ledger ledger reads
   * @param writer ledger flags and statuses
   * @param workflow OPS_HOLD workflow
   * @param numbers request numbers
   * @param lovs reasons
   * @param users processors
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public HoldService(
      HoldRequestRepository holds,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      WorkflowService workflow,
      DocumentNumberService numbers,
      LovService lovs,
      UserDirectory users,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.holds = holds;
    this.ledger = ledger;
    this.writer = writer;
    this.workflow = workflow;
    this.numbers = numbers;
    this.lovs = lovs;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public HoldRequest get(Long id) {
    return holds.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a hold request (MKTID.003/005/007), optionally submitting it at once.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param terms reason, remarks and hold-until date
   * @param submit whether to submit it for approval now
   * @param source screen or Collection feed
   * @return the request
   */
  public HoldRequest create(
      Long companyId, String invoiceNo, Terms terms, boolean submit, RequestSource source) {
    validate(terms);
    OpsInvoice invoice = ledger.require(invoiceNo.strip());
    if (invoice.isDpFlag() || !HOLDABLE.contains(invoice.getRemittanceStatus())) {
      throw new BusinessRuleException(
          "HOLD_INVOICE_NOT_HOLDABLE",
          "Invoice "
              + invoice.getInvoiceNo()
              + " cannot be held: remittance status "
              + invoice.getRemittanceStatus());
    }
    holds
        .findFirstByInvoiceInvoiceNoAndStageIn(invoice.getInvoiceNo(), LIVE)
        .ifPresent(
            h -> {
              throw new BusinessRuleException(
                  "HOLD_DUPLICATE",
                  "Invoice " + invoice.getInvoiceNo() + " already has hold " + h.getRequestNo());
            });
    HoldRequest hold =
        holds.save(
            new HoldRequest(
                companyId,
                numbers.next("HLD-" + LocalDate.now(clock).getYear()),
                new InvoiceRef(
                    invoice.getInvoiceNo(),
                    invoice.getArn(),
                    invoice.getInsurerCode(),
                    invoice.getClientCode(),
                    invoice.getAssuredName()),
                terms,
                new Origin(source, currentUser.username())));
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                hold.getId().toString(),
                hold.getRequestNo(),
                invoice.getInvoiceNo() + " - " + invoice.getAssuredName(),
                "/remittance/holds/" + hold.getId(),
                invoice.getClassification().segment()),
            null));
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.CREATE, describe(hold));
    return submit ? submit(hold.getId()) : hold;
  }

  private void validate(Terms terms) {
    lovs.requireValid(REASON_LOV, terms.reasonCode(), LocalDate.now(clock));
    if (!terms.holdUntil().isAfter(LocalDate.now(clock))) {
      throw new BusinessRuleException("HOLD_DATE", "The hold-until date must be in the future");
    }
  }

  /**
   * Changes a draft.
   *
   * @param id request
   * @param terms terms
   * @return the request
   */
  public HoldRequest update(Long id, Terms terms) {
    HoldRequest hold = inStage(id, HoldStage.DRAFT);
    validate(terms);
    hold.update(terms);
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.UPDATE, describe(hold));
    return hold;
  }

  /**
   * Submits a draft for approval; the invoice becomes REQUESTED_FOR_HOLD (RMTID.019).
   *
   * @param id request
   * @return the request
   */
  public HoldRequest submit(Long id) {
    HoldRequest hold = get(id);
    workflow.transition(ENTITY, id.toString(), "submit", TransitionNote.NONE);
    OpsInvoice invoice = ledger.require(hold.getInvoiceNo());
    hold.submitted(invoice.getRemittanceStatus(), clock.instant());
    if (invoice.getRemittanceStatus().isDerived()) {
      writer.setRemittanceStatus(
          hold.getInvoiceNo(),
          RemittanceStatus.REQUESTED_FOR_HOLD,
          RemittanceSettings.MODULE,
          HOLD + hold.getRequestNo() + " requested");
    }
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.SUBMIT, describe(hold));
    notifications.notifyPermission(
        APPROVER, notice(hold, HOLD + hold.getRequestNo() + " for approval"));
    return hold;
  }

  /**
   * Approves a hold (MKTID.006): the invoice gets the HOLD flag.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public HoldRequest approve(Long id, String comment) {
    HoldRequest hold = get(id);
    hold.approved(currentUser.username(), clock.instant());
    workflow.transition(ENTITY, id.toString(), "approve", TransitionNote.comment(comment));
    restoreStatus(hold, "approved");
    writer.setFlag(
        new FlagChange(
            hold.getInvoiceNo(),
            InvoiceFlag.HOLD,
            true,
            RemittanceSettings.MODULE,
            HOLD + hold.getRequestNo()));
    audit.record(
        ENTITY,
        hold.getRequestNo(),
        AuditAction.AUTHORIZE,
        "Invoice on hold until " + hold.getHoldUntil());
    return hold;
  }

  /**
   * Rejects a hold request; the invoice gets its previous status back.
   *
   * @param id request
   * @param comment reason
   * @return the request
   */
  public HoldRequest reject(Long id, String comment) {
    HoldRequest hold = get(id);
    workflow.transition(ENTITY, id.toString(), "reject", TransitionNote.comment(comment));
    restoreStatus(hold, "rejected");
    hold.released(comment, clock.instant());
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.REJECT, comment);
    return hold;
  }

  private void restoreStatus(HoldRequest hold, String what) {
    OpsInvoice invoice = ledger.require(hold.getInvoiceNo());
    if (invoice.getRemittanceStatus() == RemittanceStatus.REQUESTED_FOR_HOLD) {
      RemittanceStatus previous = hold.getPreviousStatus();
      writer.setRemittanceStatus(
          hold.getInvoiceNo(),
          previous == null || previous.isDerived() ? RemittanceStatus.UNPROCESSED : previous,
          RemittanceSettings.MODULE,
          HOLD + hold.getRequestNo() + " " + what);
    }
  }

  /**
   * Cancels a draft.
   *
   * @param id request
   * @return the request
   */
  public HoldRequest cancel(Long id) {
    HoldRequest hold = get(id);
    workflow.transition(ENTITY, id.toString(), "cancel", TransitionNote.NONE);
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.UPDATE, "Draft cancelled");
    return hold;
  }

  /**
   * Asks to extend an active hold (MKTID.005).
   *
   * @param id request
   * @param until new hold-until date
   * @param comment comment
   * @return the request
   */
  public HoldRequest extend(Long id, LocalDate until, String comment) {
    HoldRequest hold = inStage(id, HoldStage.ACTIVE);
    hold.requestExtension(until);
    workflow.transition(ENTITY, id.toString(), "extend", TransitionNote.comment(comment));
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.SUBMIT, "Extension to " + until);
    notifications.notifyPermission(
        APPROVER, notice(hold, HOLD + hold.getRequestNo() + " extension for approval"));
    return hold;
  }

  /**
   * Approves or rejects an extension (MKTID.006, four eyes).
   *
   * @param id request
   * @param granted whether it is approved
   * @param comment comment
   * @return the request
   */
  public HoldRequest decideExtension(Long id, boolean granted, String comment) {
    HoldRequest hold = inStage(id, HoldStage.EXTENSION_FOR_APPROVAL);
    requireOtherUser(hold);
    workflow.transition(
        ENTITY,
        id.toString(),
        granted ? "approve_extension" : "reject_extension",
        TransitionNote.comment(comment));
    hold.extensionDecided(granted);
    audit.record(
        ENTITY,
        hold.getRequestNo(),
        granted ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Extension " + (granted ? "approved" : "rejected"));
    return hold;
  }

  /**
   * Asks to cancel an active hold (MKTID.005); the approver decides (MKTID.006).
   *
   * @param id request
   * @param comment reason
   * @return the request
   */
  public HoldRequest requestCancel(Long id, String comment) {
    HoldRequest hold = inStage(id, HoldStage.ACTIVE);
    workflow.transition(ENTITY, id.toString(), "request_cancel", TransitionNote.comment(comment));
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.SUBMIT, "Cancellation requested");
    notifications.notifyPermission(
        APPROVER, notice(hold, HOLD + hold.getRequestNo() + " cancellation for approval"));
    return hold;
  }

  /**
   * Approves or rejects a cancellation (four eyes); an approved cancellation releases the hold.
   *
   * @param id request
   * @param granted whether it is approved
   * @param comment comment
   * @return the request
   */
  public HoldRequest decideCancel(Long id, boolean granted, String comment) {
    HoldRequest hold = inStage(id, HoldStage.CANCEL_FOR_APPROVAL);
    requireOtherUser(hold);
    workflow.transition(
        ENTITY,
        id.toString(),
        granted ? "approve_cancel" : "reject_cancel",
        TransitionNote.comment(comment));
    if (granted) {
      clearHold(hold, "Cancellation approved");
    }
    return hold;
  }

  /**
   * Releases an active hold (MKTID.002): the invoice becomes eligible for extraction.
   *
   * @param id request
   * @param comment reason
   * @return the request
   */
  public HoldRequest release(Long id, String comment) {
    HoldRequest hold = inStage(id, HoldStage.ACTIVE);
    workflow.transition(ENTITY, id.toString(), "release", TransitionNote.comment(comment));
    clearHold(hold, comment == null ? "Released" : comment);
    return hold;
  }

  /**
   * Expires an active hold whose date has passed (RMTID.021, HOLD_EXPIRY job).
   *
   * @param id request
   * @return the request
   */
  public HoldRequest expire(Long id) {
    HoldRequest hold = inStage(id, HoldStage.ACTIVE);
    workflow.systemTransition(
        ENTITY,
        id.toString(),
        "expire",
        TransitionNote.comment("Hold date " + hold.getHoldUntil() + " passed"));
    clearHold(hold, "Expired on " + hold.getHoldUntil());
    return hold;
  }

  private void clearHold(HoldRequest hold, String note) {
    writer.setFlag(
        new FlagChange(
            hold.getInvoiceNo(),
            InvoiceFlag.HOLD,
            false,
            RemittanceSettings.MODULE,
            HOLD + hold.getRequestNo() + ": " + note));
    hold.released(note, clock.instant());
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.CLOSE, note);
    notifyParties(hold, HOLD + hold.getRequestNo() + " released", null);
  }

  /**
   * Tells the requestor and the assigned processor that a hold expires soon (HOLD_EXPIRING).
   *
   * @param id request
   * @return the request
   */
  public HoldRequest warnExpiring(Long id) {
    HoldRequest hold = get(id);
    notifyParties(
        hold, HOLD + hold.getRequestNo() + " expires on " + hold.getHoldUntil(), EXPIRING);
    hold.expiryNotified();
    return hold;
  }

  /**
   * Assigns an approved hold to a remittance processor (MKTID.004), who is notified.
   *
   * @param id request
   * @param processor user holding REMIT_PROCESS
   * @return the request
   */
  public HoldRequest assign(Long id, String processor) {
    HoldRequest hold = get(id);
    if (!hold.getStage().holdsInvoice()) {
      throw new BusinessRuleException("HOLD_STAGE", HOLD + hold.getRequestNo() + " is not active");
    }
    List<String> processors = users.usersWithPermission(ExtractionService.PROCESSORS);
    if (processors.stream().noneMatch(u -> CurrentUser.sameUser(u, processor))) {
      throw new BusinessRuleException(
          "HOLD_ASSIGNEE_NOT_ELIGIBLE", processor + " is not an active remittance processor");
    }
    hold.assign(processor);
    audit.record(ENTITY, hold.getRequestNo(), AuditAction.UPDATE, "Assigned to " + processor);
    notifications.notifyUser(
        processor, notice(hold, HOLD + hold.getRequestNo() + " assigned to you"));
    return hold;
  }

  /**
   * Hold requests of an invoice (invoice 360).
   *
   * @param invoiceNo invoice
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public List<HoldRequest> ofInvoice(String invoiceNo) {
    return holds.findByInvoiceInvoiceNoOrderByIdDesc(invoiceNo);
  }

  private HoldRequest inStage(Long id, HoldStage stage) {
    HoldRequest hold = get(id);
    if (hold.getStage() != stage) {
      throw new BusinessRuleException(
          "HOLD_STAGE", HOLD + hold.getRequestNo() + " is " + hold.getStage() + ", not " + stage);
    }
    return hold;
  }

  private void requireOtherUser(HoldRequest hold) {
    if (CurrentUser.sameUser(hold.getRequestedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "HOLD_FOUR_EYES", HOLD + hold.getRequestNo() + " must be decided by another user");
    }
  }

  private void notifyParties(HoldRequest hold, String title, String event) {
    Notice notice = notice(hold, title);
    for (String user : parties(hold)) {
      if (event == null) {
        notifications.notifyUser(user, notice);
      } else {
        notifications.notifyUser(user, notice, event);
      }
    }
  }

  private static List<String> parties(HoldRequest hold) {
    return hold.getAssignedProcessor() == null
        ? List.of(hold.getRequestedBy())
        : List.of(hold.getRequestedBy(), hold.getAssignedProcessor());
  }

  private static Notice notice(HoldRequest hold, String title) {
    return new Notice(
        title,
        describe(hold),
        "/remittance/holds/" + hold.getId(),
        ENTITY,
        hold.getId().toString());
  }

  private static String describe(HoldRequest hold) {
    return hold.getInvoiceNo()
        + " ("
        + hold.getAssuredName()
        + ") until "
        + hold.getHoldUntil()
        + ": "
        + hold.getReasonCode()
        + (hold.getRemarks() == null ? "" : " - " + hold.getRemarks());
  }
}
