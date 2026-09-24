package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business steps of an endorsement request before posting (OPERATIONS_DESIGN section 7): submit
 * and resubmit (recompute, over-adjustment control, quotation hand-off, pending negative adjustment
 * flag), validation (to approval, or straight to posting for requests without financial effect,
 * ADJID.010) and the team leader's approval (four eyes). Returns and cancellations are generic
 * workflow actions ({@code RequestStageListener}).
 */
@Service
@Transactional
public class RequestWorkflowService {

  /** Port name of the hand-off asking Marketing for a quotation (ADJID.008). */
  public static final String QUOTATION_PORT = "QUOTATION_REQUIRED";

  private static final String SUBMIT = "submit";
  private static final String RESUBMIT = "resubmit";

  private final EndorsementRequestService requests;
  private final RecomputeService recompute;
  private final InvoiceGuard guard;
  private final WorkflowService workflow;
  private final HandoffService handoffs;
  private final AlertService alerts;
  private final RequestNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param recompute recompute
   * @param guard invoice lock and flag
   * @param workflow workflow engine
   * @param handoffs hand-offs (quotation required)
   * @param alerts alerts (over-adjustment)
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RequestWorkflowService(
      EndorsementRequestService requests,
      RecomputeService recompute,
      InvoiceGuard guard,
      WorkflowService workflow,
      HandoffService handoffs,
      AlertService alerts,
      RequestNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.recompute = recompute;
    this.guard = guard;
    this.workflow = workflow;
    this.handoffs = handoffs;
    this.alerts = alerts;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits a draft for validation.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public EndorsementRequest submit(Long id, String comment) {
    return send(id, SUBMIT, comment);
  }

  /**
   * Resubmits a returned request for validation.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public EndorsementRequest resubmit(Long id, String comment) {
    return send(id, RESUBMIT, comment);
  }

  private EndorsementRequest send(Long id, String action, String comment) {
    EndorsementRequest request = requests.get(id);
    requireStage(request, SUBMIT.equals(action) ? RequestStage.DRAFT : RequestStage.RETURNED);
    Recompute result = refresh(request);
    if (result.baseline().exceeded()) {
      requireBaselineOverride(request, result);
    }
    if (result.quotationRequired() && request.getHandoffRef() == null) {
      OpsHandoff handoff = quotationHandoff(request);
      request.quotationRequired(true, String.valueOf(handoff.getId()));
    }
    guard.flagNegative(request);
    request.submitted(currentUser.username(), clock.instant());
    workflow.transition(Adjustments.ENTITY, String.valueOf(id), action, note(comment));
    notifier.team("ADJ_PROCESS", request, "for validation");
    audit.record(Adjustments.ENTITY, request.getRequestNo(), AuditAction.SUBMIT, action);
    return request;
  }

  /**
   * Validates a request (ADJID.005): to the team leader's approval, or straight to posting when it
   * has no financial effect.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public EndorsementRequest validate(Long id, String comment) {
    EndorsementRequest request = requests.get(id);
    requireStage(request, RequestStage.FOR_VALIDATION);
    if (request.isQuotationRequired() && request.getQuotationRef() == null) {
      throw new BusinessRuleException(
          "ADJ_QUOTATION_REQUIRED",
          "The TSI increase exceeds the package limit: link the quotation prepared by Marketing"
              + " first (ADJID.008)");
    }
    refresh(request);
    request.validated(currentUser.username(), clock.instant());
    boolean approval = request.isNeedsApproval();
    workflow.transition(
        Adjustments.ENTITY,
        String.valueOf(id),
        approval ? "validate" : "validate_for_posting",
        note(comment));
    notifier.team(
        approval ? "ADJ_APPROVE" : "ADJ_POST", request, approval ? "for approval" : "for posting");
    audit.record(Adjustments.ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Validated");
    return request;
  }

  /**
   * Approves a validated request (ADJID.010): refused to its requester, submitter and validator.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public EndorsementRequest approve(Long id, String comment) {
    EndorsementRequest request = requests.get(id);
    requireStage(request, RequestStage.FOR_APPROVAL);
    String user = currentUser.username();
    boolean own =
        CurrentUser.sameUser(user, request.getCreatedBy())
            || CurrentUser.sameUser(user, request.trail().submittedBy())
            || CurrentUser.sameUser(user, request.trail().validatedBy());
    if (own) {
      throw new BusinessRuleException(
          "ADJ_FOUR_EYES", "A request is approved by someone who did not raise or validate it");
    }
    request.approved(user, clock.instant());
    workflow.transition(Adjustments.ENTITY, String.valueOf(id), "approve", note(comment));
    notifier.team("ADJ_POST", request, "for posting");
    audit.record(Adjustments.ENTITY, request.getRequestNo(), AuditAction.AUTHORIZE, "Approved");
    return request;
  }

  private Recompute refresh(EndorsementRequest request) {
    Recompute result =
        recompute.compute(
            request.getSubject().invoiceNo(),
            request.getComputation(),
            request.getTerms(),
            request.getAmounts());
    request.recordRecompute(result.components(), result.shares());
    return result;
  }

  private void requireBaselineOverride(EndorsementRequest request, Recompute result) {
    if (request.getBaselineOverride() == null) {
      throw new BusinessRuleException(
          Adjustments.OVER_BASELINE,
          "Cumulative adjustments of "
              + result.baseline().adjustedAfter().toPlainString()
              + " exceed the baseline of "
              + result.baseline().limitPercent().stripTrailingZeros().toPlainString()
              + "% of the original premium "
              + result.baseline().originalPremium().toPlainString()
              + ": review the previous adjustments and give the justification to proceed");
    }
    alerts.raise(
        Adjustments.OVER_BASELINE,
        new AlertFacts(
            request.getCompanyId(),
            request.getBranchId(),
            Adjustments.ENTITY,
            request.getRequestNo(),
            "Request "
                + request.getRequestNo()
                + " exceeds the adjustment baseline of "
                + request.getSubject().invoiceNo()
                + ": "
                + request.getBaselineOverride(),
            result.baseline().adjustedAfter(),
            Adjustments.OVER_BASELINE + ":" + request.getRequestNo()));
  }

  private OpsHandoff quotationHandoff(EndorsementRequest request) {
    return handoffs.record(
        request.getCompanyId(),
        QUOTATION_PORT,
        "QUOTE_MAINTAIN",
        new OpsHandoff.Spec(
            Adjustments.MODULE,
            Adjustments.sourceRef(request.getRequestNo()),
            request.getSubject().arn(),
            request.getTerms().sumInsuredChange(),
            request.getSubject().currency(),
            "Prepare a quotation for the TSI increase of "
                + request.getSubject().arn()
                + " above the package limit (endorsement request "
                + request.getRequestNo()
                + ")",
            null));
  }

  private static void requireStage(EndorsementRequest request, RequestStage expected) {
    if (request.getStage() != expected) {
      throw new BusinessRuleException(
          "ADJ_WRONG_STAGE",
          request.getRequestNo() + " is " + request.getStage() + ", not " + expected);
    }
  }

  private static TransitionNote note(String comment) {
    return TransitionNote.comment(comment == null || comment.isBlank() ? null : comment.strip());
  }
}
