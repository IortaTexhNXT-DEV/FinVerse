package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.Clock;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business steps of a request (MKT 1.9.0, 1.11.0, 1.14.0-1.16.3, 2.24.0): assignment of a
 * refund to a preparer, submission (to ACSL and Cashiering first for a refund of a cancelled
 * policy), the review, the Marketing approval and, for a cash advance, the HR approval. The final
 * approval sends the request to Disbursement (or hands a check cancellation over). Each check-point
 * is four-eyes: nobody approves or endorses a request they raised or moved before. Returns and
 * cancellations are generic workflow actions ({@code PayRequestStageListener}).
 */
@Service
@Transactional
public class PayRequestWorkflowService {

  private static final String APPROVE = "approve";

  private final PaymentRequestRepository requests;
  private final RefundValidationService validations;
  private final DisbursementLink disbursement;
  private final WorkflowService workflow;
  private final WorkflowViewService views;
  private final WorkAssignmentService assignments;
  private final UserDirectory users;
  private final PayRequestNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param validations ACSL and Cashiering validations
   * @param disbursement gateway, payout and hand-off
   * @param workflow workflow engine
   * @param views work cases
   * @param assignments work assignment
   * @param users user directory (eligible preparers)
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PayRequestWorkflowService(
      PaymentRequestRepository requests,
      RefundValidationService validations,
      DisbursementLink disbursement,
      WorkflowService workflow,
      WorkflowViewService views,
      WorkAssignmentService assignments,
      UserDirectory users,
      PayRequestNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.validations = validations;
    this.disbursement = disbursement;
    this.workflow = workflow;
    this.views = views;
    this.assignments = assignments;
    this.users = users;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Assigns a draft refund to a preparer (MKT 1.9.0).
   *
   * @param id request
   * @param preparer user holding PRQ_CREATE
   * @param comment comment
   * @return the request
   */
  public PaymentRequest assign(Long id, String preparer, String comment) {
    PaymentRequest request = get(id);
    PayRequests.requireStage(request, RequestStage.DRAFT, RequestStage.PREPARING);
    if (request.getStage() == RequestStage.DRAFT) {
      workflow.transition(PayRequests.ENTITY, key(request), "assign", PayRequests.note(comment));
    }
    Long caseId =
        views
            .view(PayRequests.ENTITY, key(request))
            .map(v -> v.workCase().getId())
            .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, id));
    assignments.assign(caseId, preparer, users.usersWithPermission("PRQ_CREATE"));
    audit.record(
        PayRequests.ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Assigned to " + preparer);
    return request;
  }

  /**
   * Submits a request (MKT 1.14.0): a refund with a cancelled policy goes to ACSL and Cashiering
   * for validation first (MKT 1.11.0), every other request to the reviewer.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PaymentRequest submit(Long id, String comment) {
    PaymentRequest request = get(id);
    PayRequests.requireStage(
        request, RequestStage.DRAFT, RequestStage.PREPARING, RequestStage.REQUESTED);
    boolean validate = request.getKind() == RequestKind.REFUND && request.isValidationRequired();
    workflow.transition(
        PayRequests.ENTITY,
        key(request),
        validate ? "submit_for_validation" : "submit",
        PayRequests.note(comment));
    request.record(request.trailOrNone().submitted(currentUser.username(), clock.instant()));
    if (validate) {
      validations.open(request);
      notifier.team("PRQ_ASSIGN", request, "sent for validation to ACSL and Cashiering");
    } else {
      notifier.team("PRQ_REVIEW", request, "for review");
    }
    audit.record(PayRequests.ENTITY, request.getRequestNo(), AuditAction.SUBMIT, "Submitted");
    return request;
  }

  /**
   * Endorses a reviewed request for approval (MKT 1.15.0); the reviewer did not raise or submit it.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PaymentRequest endorse(Long id, String comment) {
    PaymentRequest request = get(id);
    PayRequests.requireStage(request, RequestStage.FOR_REVIEW);
    RequestTrail trail = request.trailOrNone();
    requireNotOwn(request, request.getCreatedBy(), trail.submittedBy());
    workflow.transition(PayRequests.ENTITY, key(request), "endorse", PayRequests.note(comment));
    request.record(trail.reviewed(currentUser.username(), clock.instant()));
    notifier.team("PRQ_APPROVE", request, "for approval");
    audit.record(PayRequests.ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Endorsed");
    return request;
  }

  /**
   * Approves a request (MKT 1.16.0-1.16.3, 2.24.0): the Marketing approval sends a refund to
   * Disbursement, a cash advance to HR and a check cancellation to Disbursement; the HR approval
   * sends a cash advance to Disbursement.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PaymentRequest approve(Long id, String comment) {
    PaymentRequest request = get(id);
    PayRequests.requireStage(request, RequestStage.FOR_APPROVAL, RequestStage.HR_APPROVAL);
    RequestTrail trail = request.trailOrNone();
    requireNotOwn(request, request.getCreatedBy(), trail.submittedBy(), trail.reviewedBy());
    boolean hr = request.getStage() == RequestStage.HR_APPROVAL;
    if (hr) {
      requireNotOwn(request, trail.approvedBy());
    }
    workflow.transition(PayRequests.ENTITY, key(request), APPROVE, PayRequests.note(comment));
    String user = currentUser.username();
    request.record(
        hr ? trail.hrApproved(user, clock.instant()) : trail.approved(user, clock.instant()));
    afterApproval(request, hr);
    audit.record(
        PayRequests.ENTITY,
        request.getRequestNo(),
        AuditAction.AUTHORIZE,
        hr ? "Approved by HR" : "Approved by Marketing");
    return request;
  }

  private void afterApproval(PaymentRequest request, boolean hr) {
    switch (request.getKind()) {
      case REFUND -> {
        disbursement.send(request);
        notifier.requester(request, "approved and sent to Disbursement");
      }
      case CASH_ADVANCE -> {
        if (hr) {
          disbursement.send(request);
          notifier.requester(request, "approved by HR and sent to Disbursement");
        } else {
          notifier.team("PRQ_HR_APPROVE", request, "for HR approval");
        }
      }
      default -> {
        disbursement.handOffCancellation(request);
        notifier.requester(request, "approved and sent to Disbursement for cancellation");
      }
    }
  }

  private void requireNotOwn(PaymentRequest request, String... earlier) {
    String user = currentUser.username();
    if (Stream.of(earlier).anyMatch(u -> CurrentUser.sameUser(user, u))) {
      throw new BusinessRuleException(
          PayRequests.FOUR_EYES,
          "You raised or already moved " + request.getRequestNo() + ": another user decides");
    }
  }

  private PaymentRequest get(Long id) {
    return requests
        .findLoaded(id)
        .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, id));
  }

  private static String key(PaymentRequest request) {
    return String.valueOf(request.getId());
  }
}
