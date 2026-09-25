package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherPosting.Posted;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The workflow actions of a voucher (DIS 2.7.11, 2.9.0, 2.13-2.21; workflow {@code DISB_VOUCHER}):
 * submit for review, route to the approver, submit for approval, approve (post the entry, issue the
 * instrument, give the DV number to the source), reject, and cancel in process, for review or after
 * approval (reverse the entry, cancel the instrument and tell the source, which restores its
 * records). Four eyes: the reviewer is not the processor and the approver is neither.
 */
@Service
@Transactional
public class VoucherActions {

  private static final String CANCEL_LOV = "DISB_CANCEL_REASON";
  private static final String FOUR_EYES = "DV_FOUR_EYES";

  /** Error code of an approval whose entry could not be posted (DIS 3.27.0). */
  public static final String POSTING_FAILED = "DV_POSTING_FAILED";

  private final VoucherService vouchers;
  private final VoucherPosting posting;
  private final InstrumentService instruments;
  private final IntakeRequestRepository requests;
  private final GatewaySync gateway;
  private final WorkflowService workflow;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the actions.
   *
   * @param vouchers voucher processing
   * @param posting approval posting
   * @param instruments instruments
   * @param requests payment requests
   * @param gateway Operations request progress
   * @param workflow voucher workflow
   * @param lovs reasons
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public VoucherActions(
      VoucherService vouchers,
      VoucherPosting posting,
      InstrumentService instruments,
      IntakeRequestRepository requests,
      GatewaySync gateway,
      WorkflowService workflow,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.vouchers = vouchers;
    this.posting = posting;
    this.instruments = instruments;
    this.requests = requests;
    this.gateway = gateway;
    this.workflow = workflow;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits a complete voucher for review (DIS 2.7.11).
   *
   * @param id voucher
   * @param comment comment
   * @return voucher
   */
  public Voucher submit(Long id, String comment) {
    return fromProcessor(id, "submit", comment);
  }

  /**
   * Routes a complete voucher straight to the approver (DIS 3.25.0).
   *
   * @param id voucher
   * @param comment comment
   * @return voucher
   */
  public Voucher routeToApprover(Long id, String comment) {
    return fromProcessor(id, "route_to_approver", comment);
  }

  private Voucher fromProcessor(Long id, String action, String comment) {
    Voucher v = vouchers.get(id);
    vouchers.requireComplete(v);
    v.submittedBy(currentUser.username());
    return moved(v, action, TransitionNote.comment(comment));
  }

  /**
   * The team leader submits the checked voucher for approval (DIS 2.13.0, 2.15.0).
   *
   * @param id voucher
   * @param comment comment
   * @return voucher
   */
  public Voucher submitForApproval(Long id, String comment) {
    Voucher v = vouchers.get(id);
    vouchers.requireComplete(v);
    if (CurrentUser.sameUser(currentUser.username(), v.getSubmittedBy())) {
      throw new BusinessRuleException(FOUR_EYES, "The processor cannot check their own voucher");
    }
    v.reviewedBy(currentUser.username());
    return moved(v, "submit_for_approval", TransitionNote.comment(comment));
  }

  /**
   * Approves a voucher (DIS 2.19.0): the entry is posted, the instrument issued and the source is
   * told the DV number (Operations status DV_ASSIGNED).
   *
   * @param id voucher
   * @param comment comment
   * @return voucher
   */
  public Voucher approve(Long id, String comment) {
    Voucher v = vouchers.get(id);
    if (v.getStage() != VoucherStage.FOR_APPROVAL) {
      throw new BusinessRuleException(
          "DV_NOT_FOR_APPROVAL", "DV " + v.getDvNo() + " is " + v.getStage());
    }
    vouchers.requireComplete(v);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, v.getSubmittedBy())
        || CurrentUser.sameUser(user, v.getReviewedBy())) {
      throw new BusinessRuleException(
          FOUR_EYES, "The approver of a voucher cannot be its processor or checker");
    }
    Posted posted;
    try {
      posted = posting.post(v);
    } catch (BusinessRuleException ex) {
      throw new BusinessRuleException(
          POSTING_FAILED, "DV " + v.getDvNo() + " could not be posted: " + ex.getMessage(), ex);
    }
    v.posted(posted.batchNo(), posted.rate(), user, clock.instant());
    moved(v, "approve", TransitionNote.comment(comment));
    Instrument instrument = instruments.issue(v);
    IntakeRequest request = request(v);
    gateway.approved(request, v.getDvNo());
    gateway.track(request, null, instrument.getStatus().name());
    notifyProcessor(v, "approved and posted (" + posted.batchNo() + ")");
    return v;
  }

  /**
   * Rejects a voucher (DIS 2.21.0): terminal; the request goes back to its source.
   *
   * @param id voucher
   * @param reasonCode reason (LOV {@code DISB_RETURN_REASON})
   * @param comment comment
   * @return voucher
   */
  public Voucher reject(Long id, String reasonCode, String comment) {
    Voucher v = vouchers.get(id);
    moved(v, "reject", new TransitionNote(reasonCode, comment));
    IntakeRequest request = request(v);
    String reason = reasonText(reasonCode, comment);
    request.returned(reason);
    gateway.returned(request, reason);
    notifyProcessor(v, "rejected: " + reason);
    return v;
  }

  /**
   * Cancels a voucher in process, for review, or approved (DIS 2.9.0, 2.18.0, 2.20.0). An approved
   * voucher's entry is reversed and its instrument cancelled; the source is told CANCELLED and
   * restores its records (remittance lines extractable again, refunds re-opened).
   *
   * @param id voucher
   * @param reasonCode reason (LOV {@code DISB_CANCEL_REASON})
   * @param comment comment
   * @return voucher
   */
  public Voucher cancel(Long id, String reasonCode, String comment) {
    lovs.requireValid(CANCEL_LOV, reasonCode, LocalDate.now(clock));
    Voucher v = vouchers.get(id);
    boolean approved = v.getStage() == VoucherStage.APPROVED;
    String reason = reasonText(reasonCode, comment);
    moved(v, "cancel", new TransitionNote(reasonCode, comment));
    if (approved) {
      instruments.cancelFor(v);
      v.reversed(posting.reverse(v));
    }
    v.cancelled(reason, currentUser.username(), clock.instant());
    IntakeRequest request = request(v);
    request.cancelled(reason);
    gateway.cancelled(request, reason);
    notifyProcessor(v, "cancelled: " + reason);
    return v;
  }

  private Voucher moved(Voucher v, String action, TransitionNote note) {
    workflow.transition(DisbursementSettings.VOUCHER, v.getId().toString(), action, note);
    IntakeRequest request = request(v);
    gateway.track(request, v.getStage().name(), null);
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        action + " -> " + v.getStage() + (note.comment() == null ? "" : ": " + note.comment()));
    return v;
  }

  private IntakeRequest request(Voucher v) {
    return requests
        .findById(v.getRequestId())
        .orElseThrow(
            () -> new ResourceNotFoundException(DisbursementSettings.REQUEST, v.getRequestId()));
  }

  private void notifyProcessor(Voucher v, String what) {
    String processor = v.getSubmittedBy() != null ? v.getSubmittedBy() : v.getCreatedBy();
    if (!CurrentUser.sameUser(processor, currentUser.username())) {
      notifications.notifyUser(
          processor,
          new Notice(
              "DV " + v.getDvNo() + " " + what,
              v.getPayeeName() + " " + v.getCurrency() + " " + v.getNet(),
              DisbursementSettings.voucherLink(v.getId()),
              DisbursementSettings.VOUCHER,
              v.getId().toString()));
    }
  }

  private static String reasonText(String code, String comment) {
    return comment == null || comment.isBlank() ? code : code + ": " + comment;
  }
}
