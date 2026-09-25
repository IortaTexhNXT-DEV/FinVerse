package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decisions on access requests (BRD 2.002.5-6; FR-UA-031, FR-UA-032, FR-UA-034, FR-UA-044): the
 * chosen approver approves (group profiles: each approver in turn), a flagged request then waits
 * for a second approver (UAM-NFR-40), and the last approval applies the change now, schedules it
 * for its effective date (UAM-NFR-14) or hands a group-profile request to the System Administrator
 * (BRD-11 p.6; UQ03). The requester and the user the request is about never decide.
 */
@Service
@Transactional
public class AccessDecisionService {

  /** Alert of a flagged request (UAM-NFR-40). */
  public static final String PRIVILEGED_CHANGE = "UAM_PRIVILEGED_CHANGE";

  private static final String SECOND_APPROVE = "UAM_SECOND_APPROVE";

  private final AccessRequestRepository requests;
  private final AccessRequestValidator validator;
  private final AccessRiskRules risks;
  private final AccessChangeApplier applier;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final AccessSettings settings;
  private final AlertService alerts;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param validator request checks (again at approval)
   * @param risks risk rules
   * @param applier applies approved changes
   * @param history request history
   * @param notifier notifications
   * @param settings parameters
   * @param alerts alerts
   * @param currentUser current user
   * @param clock clock
   */
  public AccessDecisionService(
      AccessRequestRepository requests,
      AccessRequestValidator validator,
      AccessRiskRules risks,
      AccessChangeApplier applier,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      AccessSettings settings,
      AlertService alerts,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.validator = validator;
    this.risks = risks;
    this.applier = applier;
    this.history = history;
    this.notifier = notifier;
    this.settings = settings;
    this.alerts = alerts;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Approves a PENDING request as its current approver (FR-UA-031).
   *
   * @param id request
   * @param comment optional comment
   * @return the decision, with the temporary password of a created user (shown once)
   */
  public Decision approve(Long id, String comment) {
    AccessRequest r = get(id);
    if (r.getStatus() == AccessRequestStatus.PENDING_SECOND) {
      throw new BusinessRuleException(
          "ACCESS_SECOND_APPROVAL_PENDING",
          "Request " + r.getRequestNo() + " waits for the second approval");
    }
    requireMayDecide(r);
    validator.recheck(r.content());
    String approver = currentUser.username();
    String note = blankToNull(comment);
    AccessRequestStatus from = r.getStatus();
    if (r.recordApproval(approver, clock.instant(), note)) {
      history.record(r, AccessRequestAction.APPROVE, from, note);
      notifier.toApprove(r, AccessApprovers.approvalPermission(r.getUserType()));
      return new Decision(r, null);
    }
    r.addRiskFlags(risks.evaluate(r.content()));
    if (r.isSecondApprovalRequired()) {
      r.awaitSecondApproval();
      history.record(r, AccessRequestAction.APPROVE, from, note);
      alerts.raise(
          PRIVILEGED_CHANGE,
          new AlertFacts(
              null,
              null,
              AccessRequestService.ENTITY,
              r.getRequestNo(),
              "Access request "
                  + r.getRequestNo()
                  + " "
                  + r.riskFlags()
                  + ": "
                  + AccessRequestService.describe(r),
              null,
              PRIVILEGED_CHANGE + ":" + r.getRequestNo()));
      notifier.toSecondApprove(r);
      return new Decision(r, null);
    }
    return finish(r, approver, note, AccessRequestAction.APPROVE);
  }

  /**
   * Second approval of a flagged request by a holder of UAM_SECOND_APPROVE other than the first
   * approver (UAM-NFR-40; FR-UA-034).
   *
   * @param id request
   * @param comment optional comment
   * @return the decision
   */
  public Decision secondApprove(Long id, String comment) {
    AccessRequest r = get(id);
    if (r.getStatus() != AccessRequestStatus.PENDING_SECOND) {
      throw new BusinessRuleException(
          "ACCESS_NOT_SECOND_APPROVAL",
          "Request "
              + r.getRequestNo()
              + " is "
              + r.getStatus()
              + ", not waiting for a second approval");
    }
    requireMayDecide(r);
    validator.recheck(r.content());
    return finish(
        r, currentUser.username(), blankToNull(comment), AccessRequestAction.SECOND_APPROVE);
  }

  /**
   * Rejects a request with a reason (BRD 2.002.6; FR-UA-032); it is final.
   *
   * @param id request
   * @param comment reason (mandatory)
   * @return the decision
   */
  public Decision reject(Long id, String comment) {
    if (comment == null || comment.isBlank()) {
      throw new BusinessRuleException("ACCESS_REJECT_REASON", "Enter the reason of the rejection");
    }
    AccessRequest r = get(id);
    requireMayDecide(r);
    AccessRequestStatus from = r.getStatus();
    r.reject(currentUser.username(), clock.instant(), comment.trim());
    history.record(r, AccessRequestAction.REJECT, from, comment.trim());
    notifier.decided(r, "rejected");
    return new Decision(r, null);
  }

  private Decision finish(
      AccessRequest r, String approver, String note, AccessRequestAction action) {
    AccessRequestStatus from = r.getStatus();
    if (r.getRequestType().isGroupProfile() && !settings.roleApplyOnApproval()) {
      r.approve(AccessRequestStatus.FOR_IMPLEMENTATION, approver, clock.instant(), note);
      history.record(r, action, from, note);
      notifier.toImplement(r);
      notifier.decided(r, "approved, for implementation");
      return new Decision(r, null);
    }
    if (r.getEffectiveFrom() != null
        && r.getEffectiveFrom().isAfter(LocalDate.now(clock.withZone(WorkingHours.ZONE)))) {
      r.approve(AccessRequestStatus.SCHEDULED, approver, clock.instant(), note);
      history.record(r, action, from, note);
      notifier.decided(r, "approved, applies on " + r.getEffectiveFrom());
      return new Decision(r, null);
    }
    r.approve(AccessRequestStatus.APPROVED, approver, clock.instant(), note);
    history.record(r, action, from, note);
    String password = applier.apply(r);
    history.record(r, AccessRequestAction.APPLY, AccessRequestStatus.APPROVED, null);
    notifier.decided(r, "approved");
    notifier.accessChanged(r);
    return new Decision(r, password);
  }

  /**
   * Requires that the current user may decide the request now (four eyes; FR-UA-031 R1, FR-UA-034
   * R3): not the requester nor the subject user; the chosen approver of a PENDING request (any
   * approver when UAM_ANY_APPROVER); a second approver other than the first for PENDING_SECOND.
   *
   * @param r request
   */
  void requireMayDecide(AccessRequest r) {
    String me = currentUser.username();
    if (CurrentUser.sameUser(me, r.getCreatedBy())
        || CurrentUser.sameUser(me, AccessRequestNotifier.requester(r))) {
      throw new BusinessRuleException(
          "ACCESS_FOUR_EYES", "A request cannot be decided by the user who submitted it");
    }
    if (r.getUserType() == AccessUserType.INTERNAL && CurrentUser.sameUser(me, r.getUsername())) {
      throw new BusinessRuleException(
          "ACCESS_SUBJECT_DECIDES", "You cannot decide a request about your own access");
    }
    switch (r.getStatus()) {
      case PENDING -> requireChosenApprover(r, me);
      case PENDING_SECOND -> requireSecondApprover(r, me);
      default ->
          throw new BusinessRuleException(
              "ACCESS_REQUEST_DECIDED",
              "Request " + r.getRequestNo() + " is already " + r.getStatus());
    }
  }

  private void requireChosenApprover(AccessRequest r, String me) {
    if (!currentUser.hasAuthority(AccessApprovers.approvalPermission(r.getUserType()))) {
      throw new AccessDeniedException("Not permitted to approve access requests");
    }
    String assigned = r.getAssignedApprover();
    if (assigned != null && !CurrentUser.sameUser(me, assigned) && !settings.anyApprover()) {
      throw new BusinessRuleException(
          "ACCESS_NOT_ASSIGNED",
          "Request " + r.getRequestNo() + " is assigned to " + assigned + " for approval");
    }
  }

  private void requireSecondApprover(AccessRequest r, String me) {
    if (!currentUser.hasAuthority(SECOND_APPROVE)) {
      throw new AccessDeniedException("Not permitted to give the second approval");
    }
    if (r.approvedBy(me)) {
      throw new BusinessRuleException(
          "ACCESS_SECOND_SAME_APPROVER", "The second approval is given by another approver");
    }
  }

  private AccessRequest get(Long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(AccessRequestService.ENTITY, id));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
