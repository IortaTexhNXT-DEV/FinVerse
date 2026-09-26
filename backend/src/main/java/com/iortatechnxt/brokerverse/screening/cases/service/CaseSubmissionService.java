package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Validation;
import com.iortatechnxt.brokerverse.screening.config.service.ApprovalMatrix.Route;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The investigator's submission (SNSRP-502, 701, 103, 702; FR-SS-051, 060, 062): the disposition of
 * the stage and the recommendation are mandatory; the case is validated (every result on the
 * timeline), routed by the approval matrix (UNIT_HEAD_APPROVAL with the route's approver, or CLOSED
 * when a route says so) and becomes read only to the investigator. "Need more information" keeps
 * the case with the investigator. A returned case is resubmitted with a response to the stage that
 * returned it.
 */
@Service
@Transactional
public class CaseSubmissionService {

  /** Disposition that keeps the case with the investigator (FR-SS-051 alternate flow). */
  static final String NEED_MORE_INFO = "NEED_MORE_INFO";

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
   * @param validator validation rules
   * @param router approval matrix
   * @param mover transitions and assignment
   * @param reviews case reviews
   * @param notifier notices
   * @param timeline case timeline
   * @param audit audit trail
   */
  @SuppressWarnings("java:S107") // collaborators of the submission
  public CaseSubmissionService(
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
   * Submits a case under investigation.
   *
   * @param caseId the case
   * @param decision disposition, recommendation and STR flag
   * @return the case and the warnings of non-blocking rules
   */
  public Submitted submit(Long caseId, Decision decision) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, "submit", EnumSet.of(CaseStage.INVESTIGATION));
    String disposition =
        CaseChecks.require(
            decision.disposition(), "SCR_DISPOSITION_REQUIRED", "Select the disposition");
    String recommendation =
        CaseChecks.require(
            decision.recommendation(), "SCR_RECOMMENDATION_REQUIRED", "Write the recommendation");
    CaseChecks.requireAllowed(validator, CaseStage.INVESTIGATION, disposition);
    Decision given = new Decision(disposition, recommendation, decision.strRequired());
    if (NEED_MORE_INFO.equals(disposition)) {
      return requestInformation(c, given);
    }
    Validation v = validator.validate(c, CaseStage.INVESTIGATION, given);
    if (!v.passed()) {
      CaseChecks.refuse(c, timeline, CaseStage.INVESTIGATION, v);
    }
    timeline.record(c, CaseEventType.VALIDATED, EventFacts.remarks(v.summary()));
    c.dispose(disposition, recommendation, given.strRequired(), access.user());
    reviews.submitted(c);
    Optional<Route> route = router.route(c, CaseStage.INVESTIGATION, disposition);
    boolean close = route.map(r -> CaseStage.CLOSED.name().equals(r.toStage())).orElse(false);
    if (close) {
      mover.act(c, "close_no_approval", CaseChecks.note(null, recommendation));
      timeline.record(
          c,
          CaseEventType.CLOSED,
          EventFacts.move(
              CaseStage.INVESTIGATION.name(),
              CaseStage.CLOSED.name(),
              disposition,
              recommendation));
    } else {
      mover.act(c, "submit", CaseChecks.note(null, recommendation));
      timeline.record(
          c,
          CaseEventType.SUBMITTED,
          EventFacts.move(
              CaseStage.INVESTIGATION.name(),
              CaseStage.UNIT_HEAD_APPROVAL.name(),
              disposition,
              recommendation));
      toApprover(c, route.orElse(null), CaseStage.UNIT_HEAD_APPROVAL, null);
    }
    audit.record(
        CaseCodes.ENTITY, c.getCaseNo(), AuditAction.SUBMIT, "Submitted with " + disposition);
    return new Submitted(c, v.warnings());
  }

  private Submitted requestInformation(ScreeningCase c, Decision decision) {
    String investigator = access.user();
    mover.act(c, "request_info", CaseChecks.note(null, decision.recommendation()));
    timeline.record(
        c,
        CaseEventType.INFO_REQUESTED,
        EventFacts.move(
            CaseStage.INVESTIGATION.name(),
            CaseStage.INVESTIGATION.name(),
            decision.disposition(),
            decision.recommendation()));
    mover.assign(
        c,
        investigator,
        CaseEventType.ASSIGNED,
        CaseMover.AssignFacts.auto("Information requested; the case stays with " + investigator));
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.UPDATE, "More information requested");
    return new Submitted(c, List.of());
  }

  /**
   * Resubmits a returned case to the stage that returned it (FR-SS-062).
   *
   * @param caseId the case
   * @param response the response to the return
   * @param decision a corrected disposition and recommendation (blank keeps them)
   * @return the case and the warnings
   */
  public Submitted resubmit(Long caseId, String response, Decision decision) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, "resubmit", EnumSet.of(CaseStage.RETURNED));
    String text =
        CaseChecks.require(response, "SCR_RESPONSE_REQUIRED", "Write your response to the return");
    Decision given =
        new Decision(
            blank(decision.disposition()) ? c.getInvestigatorDisposition() : decision.disposition(),
            blank(decision.recommendation()) ? c.getRecommendation() : decision.recommendation(),
            decision.strRequired() || c.isStrRequired());
    CaseChecks.requireAllowed(validator, CaseStage.INVESTIGATION, given.disposition());
    Validation v = validator.validate(c, CaseStage.INVESTIGATION, given);
    if (!v.passed()) {
      CaseChecks.refuse(c, timeline, CaseStage.RETURNED, v);
    }
    timeline.record(c, CaseEventType.VALIDATED, EventFacts.remarks(v.summary()));
    c.dispose(given.disposition(), given.recommendation(), given.strRequired(), access.user());
    reviews.submitted(c);
    CaseStage target =
        c.getReturnedFrom() == CaseStage.COMPLIANCE_REVIEW
            ? CaseStage.COMPLIANCE_REVIEW
            : CaseStage.UNIT_HEAD_APPROVAL;
    mover.act(
        c,
        target == CaseStage.COMPLIANCE_REVIEW ? "resubmit_to_compliance" : "resubmit",
        CaseChecks.note(null, text));
    timeline.record(
        c,
        CaseEventType.RESUBMITTED,
        EventFacts.move(CaseStage.RETURNED.name(), target.name(), given.disposition(), text));
    Route route =
        target == CaseStage.UNIT_HEAD_APPROVAL
            ? router.route(c, CaseStage.INVESTIGATION, given.disposition()).orElse(null)
            : null;
    toApprover(c, route, target, c.getReturnedBy());
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.SUBMIT, "Resubmitted: " + text);
    return new Submitted(c, v.warnings());
  }

  private void toApprover(ScreeningCase c, Route route, CaseStage stage, String preferred) {
    String permission = CaseAccess.ownerOf(stage);
    CaseRouter.Approver approver =
        preferred != null && router.eligible(c, preferred, permission)
            ? new CaseRouter.Approver(preferred, "Back to " + preferred + ", who returned the case")
            : router.approver(c, route, stage);
    mover.assign(
        c, approver.user(), CaseEventType.ASSIGNED, CaseMover.AssignFacts.auto(approver.note()));
    notifier.owner(c, permission, CaseCodes.EVENT_FOR_APPROVAL, "waiting for your approval");
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  /**
   * A submitted case.
   *
   * @param screeningCase the case
   * @param warnings the messages of failed non-blocking rules
   */
  public record Submitted(ScreeningCase screeningCase, List<String> warnings) {

    /** Defensive copy. */
    public Submitted {
      warnings = List.copyOf(warnings);
    }
  }
}
