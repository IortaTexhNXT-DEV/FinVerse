package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestApprover;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Return of an access request to its requester, its correction and resubmission, and its
 * cancellation (BRD 1.006, 1.007, 2.002.7; BASAU 2.4.1, 2.6.0 - 2.6.3; FR-UA-016, FR-UA-017,
 * FR-UA-033).
 */
@Service
@Transactional
public class AccessRequestReturnService {

  private final AccessRequestService requests;
  private final AccessDecisionService decisions;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests access requests
   * @param decisions decision rules (who may decide)
   * @param history request history
   * @param notifier notifications
   * @param currentUser current user
   * @param clock clock
   */
  public AccessRequestReturnService(
      AccessRequestService requests,
      AccessDecisionService decisions,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.decisions = decisions;
    this.history = history;
    this.notifier = notifier;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Returns a request to its requester with remarks (BRD 2.002.7): the remarks are mandatory, the
   * requester is notified, and a group-profile request restarts with its first approver when it is
   * resubmitted.
   *
   * @param id request
   * @param comment remarks
   * @return the returned request
   */
  public AccessRequest returnRequest(Long id, String comment) {
    if (comment == null || comment.isBlank()) {
      throw new BusinessRuleException(
          "ACCESS_RETURN_REASON", "Enter the remarks for the requester");
    }
    AccessRequest r = requests.get(id);
    decisions.requireMayDecide(r);
    AccessRequestStatus from = r.getStatus();
    r.returnToRequester(currentUser.username(), clock.instant(), comment.trim());
    history.record(r, AccessRequestAction.RETURN, from, comment.trim());
    notifier.returned(r);
    return r;
  }

  /**
   * Resubmits a returned request with a corrected justification to the same approvers (the
   * compatible correction of BASAU 2.6.x; requester only).
   *
   * @param id request
   * @param justification justification answering the remarks
   * @return the pending request
   */
  public AccessRequest resubmit(Long id, String justification) {
    AccessRequest r = requests.get(id);
    if (!CurrentUser.sameUser(currentUser.username(), r.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_NOT_REQUESTER", "Only " + r.getCreatedBy() + " can resubmit this request");
    }
    if (r.getStatus() != AccessRequestStatus.RETURNED) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_NOT_RETURNED",
          "Request " + r.getRequestNo() + " is " + r.getStatus() + ", not returned");
    }
    if (justification == null || justification.isBlank()) {
      throw new BusinessRuleException("ACCESS_JUSTIFICATION", "Enter the justification");
    }
    r.edit(r.content().withJustification(justification.trim()));
    List<String> approvers =
        r.getApprovers().stream().map(AccessRequestApprover::getApprover).toList();
    return requests.submit(id, approvers.isEmpty() ? null : approvers, justification.trim());
  }

  /**
   * Cancels a request with a reason (BRD 1.007; FR-UA-017): its creator (UAM_CANCEL) while it is a
   * draft, pending, returned or scheduled; the approver of a scheduled request too (UQ06). The
   * approver of a pending request is told.
   *
   * @param id request
   * @param reason reason (mandatory)
   * @return the cancelled request
   */
  public AccessRequest cancel(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "ACCESS_CANCEL_REASON", "Enter the reason for the cancellation");
    }
    AccessRequest r = requests.get(id);
    String me = currentUser.username();
    requireCanceller(r, me);
    String approver = r.getAssignedApprover() == null ? r.getDecidedBy() : r.getAssignedApprover();
    AccessRequestStatus from = r.getStatus();
    r.cancel(reason.trim(), me, clock.instant());
    history.record(r, AccessRequestAction.CANCEL, from, reason.trim());
    if (from != AccessRequestStatus.DRAFT && !CurrentUser.sameUser(me, approver)) {
      notifier.cancelled(r, approver);
    }
    return r;
  }

  private void requireCanceller(AccessRequest r, String me) {
    boolean creator = CurrentUser.sameUser(me, r.getCreatedBy());
    boolean scheduledApprover =
        r.getStatus() == AccessRequestStatus.SCHEDULED
            && CurrentUser.sameUser(me, r.getDecidedBy());
    if (!creator && !scheduledApprover) {
      throw new BusinessRuleException(
          "ACCESS_NOT_REQUESTER", "Only the creator can cancel request " + r.getRequestNo());
    }
    if (creator
        && !currentUser.hasAuthority("UAM_CANCEL")
        && !currentUser.hasAuthority("ACCESS_REQUEST")) {
      throw new AccessDeniedException("Not permitted to cancel access requests");
    }
  }
}
