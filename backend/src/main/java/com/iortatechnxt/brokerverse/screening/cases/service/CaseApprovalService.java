package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Validation;
import com.iortatechnxt.brokerverse.screening.config.service.ApprovalMatrix.Route;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The approval steps of a case (SNSRP-702, 703, 401; FR-SS-061, 063, 040 R2): the unit head concurs
 * (routed by the matrix to Compliance or closed) or disapproves with a return reason and a
 * rationale (RETURNED to the investigator); Compliance records the outcome of the BU escalation and
 * the escalation matrix routes the case to the AML Committee, STR preparation or closure, or back
 * to the investigator for rework; a closed case is re-opened by Compliance with a reason. The
 * investigator of a case never approves it.
 */
@Service
@Transactional
public class CaseApprovalService {

  /** Unit head: do not concur. */
  static final String NOT_CONCUR = "NOT_CONCUR";

  private static final Map<String, CaseStage> COMPLIANCE_DEFAULTS =
      Map.of(
          "CLOSE_NO_ACTION", CaseStage.CLOSED,
          "ESCALATE_COMMITTEE", CaseStage.AML_COMMITTEE,
          "FOR_STR", CaseStage.STR_PREPARATION,
          "RETURN", CaseStage.RETURNED);

  private final ScreeningCaseRepository cases;
  private final CaseAccess access;
  private final CaseValidator validator;
  private final CaseRouter router;
  private final CaseMover mover;
  private final CaseReviewService reviews;
  private final CaseNotifier notifier;
  private final CaseTimeline timeline;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param access case access
   * @param validator dispositions and validation rules
   * @param router approval and escalation matrix
   * @param mover transitions and assignment
   * @param reviews case reviews
   * @param notifier notices
   * @param timeline case timeline
   * @param audit audit trail
   */
  @SuppressWarnings("java:S107") // collaborators of the approval steps
  public CaseApprovalService(
      ScreeningCaseRepository cases,
      CaseAccess access,
      CaseValidator validator,
      CaseRouter router,
      CaseMover mover,
      CaseReviewService reviews,
      CaseNotifier notifier,
      CaseTimeline timeline,
      AuditTrailService audit) {
    this.cases = cases;
    this.access = access;
    this.validator = validator;
    this.router = router;
    this.mover = mover;
    this.reviews = reviews;
    this.notifier = notifier;
    this.timeline = timeline;
    this.audit = audit;
  }

  /**
   * The unit head's decision (FR-SS-061).
   *
   * @param caseId the case
   * @param step decision (UNIT_HEAD_APPROVAL disposition), return reason and rationale
   * @return the case
   */
  public ScreeningCase unitHead(Long caseId, Step step) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, "approve", EnumSet.of(CaseStage.UNIT_HEAD_APPROVAL));
    if (CurrentUser.sameUser(c.getInvestigator(), access.user())) {
      throw new BusinessRuleException(
          "SCR_APPROVER_IS_INVESTIGATOR",
          "A case is approved by someone other than its Investigator");
    }
    String decision =
        CaseChecks.require(step.disposition(), "SCR_DECISION_REQUIRED", "Select the decision");
    CaseChecks.requireAllowed(validator, CaseStage.UNIT_HEAD_APPROVAL, decision);
    Optional<Route> route = router.route(c, CaseStage.UNIT_HEAD_APPROVAL, decision);
    CaseStage target =
        route
            .map(r -> CaseStage.valueOf(r.toStage()))
            .orElse(NOT_CONCUR.equals(decision) ? CaseStage.RETURNED : CaseStage.COMPLIANCE_REVIEW);
    c.decide(decision);
    if (target == CaseStage.RETURNED) {
      String rationale =
          CaseChecks.require(
              step.remarks(), "SCR_RATIONALE_REQUIRED", "Write the rationale for disapproving");
      returnToInvestigator(
          c, "disapprove", CaseEventType.DISAPPROVED, step.reasonCode(), rationale);
    } else if (target == CaseStage.CLOSED) {
      mover.act(c, "approve_close", CaseChecks.note(null, step.remarks()));
      record(c, CaseEventType.APPROVED, CaseStage.UNIT_HEAD_APPROVAL, decision, step.remarks());
    } else {
      mover.act(c, "approve", CaseChecks.note(null, step.remarks()));
      record(c, CaseEventType.APPROVED, CaseStage.UNIT_HEAD_APPROVAL, decision, step.remarks());
      toOwner(c, router.approver(c, route.orElse(null), CaseStage.COMPLIANCE_REVIEW));
    }
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.AUTHORIZE, "Unit head: " + decision);
    return c;
  }

  /**
   * Compliance's outcome of the BU escalation (FR-SS-063).
   *
   * @param caseId the case
   * @param step outcome (COMPLIANCE_REVIEW disposition), return reason and remarks
   * @return the case and the warnings of non-blocking rules
   */
  public CaseSubmissionService.Submitted compliance(Long caseId, Step step) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, "record the outcome", EnumSet.of(CaseStage.COMPLIANCE_REVIEW));
    String outcome =
        CaseChecks.require(step.disposition(), "SCR_OUTCOME_REQUIRED", "Select the outcome");
    CaseChecks.requireAllowed(validator, CaseStage.COMPLIANCE_REVIEW, outcome);
    Optional<Route> route = router.route(c, CaseStage.COMPLIANCE_REVIEW, outcome);
    CaseStage target =
        route
            .map(r -> CaseStage.valueOf(r.toStage()))
            .orElse(COMPLIANCE_DEFAULTS.getOrDefault(outcome, CaseStage.CLOSED));
    String remarks =
        target == CaseStage.RETURNED
            ? CaseChecks.require(
                step.remarks(), "SCR_REMARKS_REQUIRED", "Enter the items to correct")
            : step.remarks();
    Validation v =
        validator.validate(
            c,
            CaseStage.COMPLIANCE_REVIEW,
            new Decision(
                outcome, remarks, c.isStrRequired() || target == CaseStage.STR_PREPARATION));
    if (!v.passed()) {
      CaseChecks.refuse(c, timeline, CaseStage.COMPLIANCE_REVIEW, v);
    }
    c.decide(outcome);
    route(c, target, route.orElse(null), step.reasonCode(), remarks);
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.AUTHORIZE, "Compliance: " + outcome);
    return new CaseSubmissionService.Submitted(c, v.warnings());
  }

  private void route(
      ScreeningCase c, CaseStage target, Route route, String reasonCode, String remarks) {
    String outcome = c.getDisposition();
    switch (target) {
      case RETURNED ->
          returnToInvestigator(c, "return_for_rework", CaseEventType.RETURNED, reasonCode, remarks);
      case AML_COMMITTEE -> {
        mover.act(c, "escalate_committee", CaseChecks.note(null, remarks));
        c.newCommitteeRound();
        record(c, CaseEventType.ESCALATED, CaseStage.COMPLIANCE_REVIEW, outcome, remarks);
        mover.assign(
            c, null, CaseEventType.ASSIGNED, CaseMover.AssignFacts.auto("Every committee member"));
        notifier.owner(
            c,
            CaseCodes.COMMITTEE,
            CaseCodes.EVENT_COMMITTEE,
            "waiting for the AML Committee decision");
      }
      case STR_PREPARATION -> {
        mover.act(c, "prepare_str", CaseChecks.note(null, remarks));
        c.requireStr(true);
        record(c, CaseEventType.ESCALATED, CaseStage.COMPLIANCE_REVIEW, outcome, remarks);
        toOwner(c, router.approver(c, route, CaseStage.STR_PREPARATION));
      }
      default -> {
        mover.act(c, "close", CaseChecks.note(null, remarks));
        record(c, CaseEventType.CLOSED, CaseStage.COMPLIANCE_REVIEW, outcome, remarks);
      }
    }
  }

  private void returnToInvestigator(
      ScreeningCase c, String action, CaseEventType event, String reasonCode, String text) {
    CaseStage from = c.getStage();
    mover.act(c, action, CaseChecks.note(reasonCode, text));
    timeline.record(
        c, event, EventFacts.move(from.name(), CaseStage.RETURNED.name(), reasonCode, text));
    c.returned(from, access.user());
    reviews.reopen(c);
    String investigator =
        router.eligible(c, c.getInvestigator(), CaseCodes.INVESTIGATE) ? c.getInvestigator() : null;
    mover.assign(
        c,
        investigator,
        CaseEventType.ASSIGNED,
        CaseMover.AssignFacts.auto("Returned to the investigator"));
    notifier.owner(c, CaseCodes.INVESTIGATE, CaseCodes.EVENT_RETURNED, "returned: " + text);
  }

  /**
   * Re-opens a closed case with a reason (FR-SS-040 R2).
   *
   * @param caseId the case
   * @param step return reason and remarks
   * @return the case
   */
  public ScreeningCase reopen(Long caseId, Step step) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    if (c.getStage() != CaseStage.CLOSED) {
      throw new BusinessRuleException(
          CaseAccess.NOT_ALLOWED, "The action reopen is not allowed in stage " + c.getStage());
    }
    access.requirePermission(CaseCodes.COMPLIANCE_REVIEW);
    cases
        .findByClientIdAndCaseTypeAndStatus(c.getClientId(), c.getCaseType(), CaseStatus.OPEN)
        .ifPresent(
            open -> {
              throw new BusinessRuleException(
                  "SCR_CASE_ALREADY_OPEN",
                  "The client already has the open "
                      + c.getCaseType()
                      + " case "
                      + open.getCaseNo());
            });
    String remarks =
        CaseChecks.require(
            step.remarks(), "SCR_REMARKS_REQUIRED", "Enter the reason for re-opening");
    mover.act(c, "reopen", CaseChecks.note(step.reasonCode(), remarks));
    timeline.record(
        c,
        CaseEventType.REOPENED,
        EventFacts.move(
            CaseStage.CLOSED.name(), CaseStage.INVESTIGATION.name(), step.reasonCode(), remarks));
    c.returned(CaseStage.CLOSED, access.user());
    reviews.reopen(c);
    String investigator =
        router.eligible(c, c.getInvestigator(), CaseCodes.INVESTIGATE) ? c.getInvestigator() : null;
    mover.assign(
        c, investigator, CaseEventType.ASSIGNED, CaseMover.AssignFacts.auto("Case re-opened"));
    notifier.owner(c, CaseCodes.INVESTIGATE, CaseCodes.EVENT_RETURNED, "re-opened: " + remarks);
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.REOPEN, remarks);
    return c;
  }

  private void toOwner(ScreeningCase c, CaseRouter.Approver approver) {
    mover.assign(
        c, approver.user(), CaseEventType.ASSIGNED, CaseMover.AssignFacts.auto(approver.note()));
    notifier.owner(
        c,
        CaseAccess.ownerOf(c.getStage()),
        CaseCodes.EVENT_FOR_APPROVAL,
        "waiting for your review");
  }

  private void record(
      ScreeningCase c, CaseEventType event, CaseStage from, String disposition, String remarks) {
    timeline.record(
        c, event, EventFacts.move(from.name(), c.getStage().name(), disposition, remarks));
  }

  /**
   * A decision step.
   *
   * @param disposition the disposition of the stage
   * @param reasonCode the return reason (RETURN_REASON), for a return
   * @param remarks the rationale, remarks or comment
   */
  public record Step(String disposition, String reasonCode, String remarks) {}
}
