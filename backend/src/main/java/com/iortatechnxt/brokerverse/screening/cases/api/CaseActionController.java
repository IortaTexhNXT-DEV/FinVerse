package com.iortatechnxt.brokerverse.screening.cases.api;

import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseDetail;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseRequests;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.VoteDto;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseActions;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseApprovalService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseAssignmentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseMatchService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseQueries;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSubmissionService;
import com.iortatechnxt.brokerverse.screening.cases.service.CommitteeService;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.MatchRow;
import com.iortatechnxt.brokerverse.screening.risk.api.dto.RiskProfileRow;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The business actions of a screening case (SNSRP-304, 404, 502, 701-704; FR-SS-035, 040, 043, 051,
 * 060-064): submit, resubmit, the unit head's decision, Compliance's outcome, the committee vote,
 * re-open, re-assignment, the decisions on the case's matches and "Update Risk Tag". Each returns
 * the case with the user's actions after the step (and the warnings of non-blocking validation
 * rules).
 */
@RestController
@RequestMapping("/api/v1/screening/cases/{id}")
public class CaseActionController {

  private static final String HAS_INVESTIGATE = CaseController.HAS_INVESTIGATE;

  private final CaseSubmissionService submissions;
  private final CaseApprovalService approvals;
  private final CommitteeService committee;
  private final CaseAssignmentService assignments;
  private final CaseMatchService matches;
  private final CaseQueries queries;
  private final CaseActions actions;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param submissions submissions
   * @param approvals approval steps
   * @param committee committee votes
   * @param assignments re-assignment
   * @param matches case matches and risk tag
   * @param queries case reads
   * @param actions the user's actions
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // one collaborator per action family
  public CaseActionController(
      CaseSubmissionService submissions,
      CaseApprovalService approvals,
      CommitteeService committee,
      CaseAssignmentService assignments,
      CaseMatchService matches,
      CaseQueries queries,
      CaseActions actions,
      Clock clock) {
    this.submissions = submissions;
    this.approvals = approvals;
    this.committee = committee;
    this.assignments = assignments;
    this.matches = matches;
    this.queries = queries;
    this.actions = actions;
    this.clock = clock;
  }

  private Outcome outcome(ScreeningCase c, List<String> warnings) {
    return new Outcome(CaseDetail.from(c, clock.instant(), actions.of(c)), warnings);
  }

  /**
   * Submits with a disposition (FR-SS-051, 060).
   *
   * @param id the case
   * @param request disposition, recommendation, STR flag
   * @return the case and the warnings
   */
  @PostMapping("/submit")
  @PreAuthorize(HAS_INVESTIGATE)
  public Outcome submit(@PathVariable Long id, @Valid @RequestBody CaseRequests.Submit request) {
    CaseSubmissionService.Submitted s = submissions.submit(id, request.decision());
    return outcome(s.screeningCase(), s.warnings());
  }

  /**
   * Resubmits a returned case (FR-SS-062).
   *
   * @param id the case
   * @param request response and corrections
   * @return the case and the warnings
   */
  @PostMapping("/resubmit")
  @PreAuthorize(HAS_INVESTIGATE)
  public Outcome resubmit(
      @PathVariable Long id, @Valid @RequestBody CaseRequests.Resubmit request) {
    CaseSubmissionService.Submitted s =
        submissions.resubmit(id, request.response(), request.decision());
    return outcome(s.screeningCase(), s.warnings());
  }

  /**
   * The unit head's decision (FR-SS-061).
   *
   * @param id the case
   * @param request decision, return reason and rationale
   * @return the case
   */
  @PostMapping("/decision")
  @PreAuthorize("hasAuthority('SCR_CASE_APPROVE')")
  public Outcome decide(@PathVariable Long id, @Valid @RequestBody CaseRequests.Step request) {
    return outcome(approvals.unitHead(id, step(request)), List.of());
  }

  /**
   * Compliance's outcome (FR-SS-063).
   *
   * @param id the case
   * @param request outcome, return reason and remarks
   * @return the case and the warnings
   */
  @PostMapping("/outcome")
  @PreAuthorize("hasAuthority('SCR_COMPLIANCE_REVIEW')")
  public Outcome outcome(@PathVariable Long id, @Valid @RequestBody CaseRequests.Step request) {
    CaseSubmissionService.Submitted s = approvals.compliance(id, step(request));
    return outcome(s.screeningCase(), s.warnings());
  }

  /**
   * Records a committee member's decision (FR-SS-064).
   *
   * @param id the case
   * @param request decision and remarks
   * @return the vote
   */
  @PostMapping("/votes")
  @PreAuthorize("hasAuthority('SCR_COMMITTEE')")
  public VoteDto vote(@PathVariable Long id, @Valid @RequestBody CaseRequests.Vote request) {
    return VoteDto.from(committee.vote(id, request.decision(), request.remarks()));
  }

  /**
   * Re-opens a closed case (FR-SS-040 R2).
   *
   * @param id the case
   * @param request return reason and remarks
   * @return the case
   */
  @PostMapping("/reopen")
  @PreAuthorize("hasAuthority('SCR_COMPLIANCE_REVIEW')")
  public Outcome reopen(@PathVariable Long id, @Valid @RequestBody CaseRequests.Step request) {
    return outcome(approvals.reopen(id, step(request)), List.of());
  }

  /**
   * The users the case can be re-assigned to (FR-SS-043).
   *
   * @param id the case
   * @return user names
   */
  @GetMapping("/eligible-assignees")
  @PreAuthorize("hasAuthority('SCR_CASE_ASSIGN')")
  public List<String> eligible(@PathVariable Long id) {
    return assignments.eligible(id);
  }

  /**
   * Re-assigns the case (FR-SS-043).
   *
   * @param id the case
   * @param request assignee, reason and comment
   * @return the case
   */
  @PostMapping("/reassign")
  @PreAuthorize("hasAuthority('SCR_CASE_ASSIGN')")
  public Outcome reassign(
      @PathVariable Long id, @Valid @RequestBody CaseRequests.Reassign request) {
    return outcome(
        assignments.reassign(id, request.assignee(), request.reasonCode(), request.comment()),
        List.of());
  }

  /**
   * Confirms a match of the case (FR-SS-032).
   *
   * @param id the case
   * @param matchId the match
   * @param request remarks
   * @return the match
   */
  @PostMapping("/matches/{matchId}/confirm")
  @PreAuthorize(HAS_INVESTIGATE)
  public MatchRow confirm(
      @PathVariable Long id,
      @PathVariable Long matchId,
      @Valid @RequestBody CaseRequests.Confirm request) {
    matches.confirm(id, matchId, request.remarks());
    return match(id, matchId);
  }

  /**
   * Clears a match of the case as a false positive (FR-SS-035).
   *
   * @param id the case
   * @param matchId the match
   * @param request justification and evidence
   * @return the match
   */
  @PostMapping("/matches/{matchId}/false-positive")
  @PreAuthorize(HAS_INVESTIGATE)
  public MatchRow clear(
      @PathVariable Long id,
      @PathVariable Long matchId,
      @Valid @RequestBody CaseRequests.ClearMatch request) {
    matches.falsePositive(id, matchId, request.toRequest(id));
    return match(id, matchId);
  }

  /**
   * "Update Risk Tag" on the case (FR-SS-035).
   *
   * @param id the case
   * @param request rating, tags, justification and evidence
   * @return the risk-profile history row
   */
  @PostMapping("/risk-tag")
  @PreAuthorize("hasAuthority('SCR_RISK_TAG')")
  public RiskProfileRow riskTag(
      @PathVariable Long id, @Valid @RequestBody CaseRequests.RiskTag request) {
    return RiskProfileRow.from(matches.riskTag(id, request.toChange()));
  }

  private MatchRow match(Long id, Long matchId) {
    return matches.of(queries.get(id)).stream()
        .filter(m -> m.getId().equals(matchId))
        .findFirst()
        .map(MatchRow::from)
        .orElseThrow();
  }

  private static CaseApprovalService.Step step(CaseRequests.Step request) {
    return new CaseApprovalService.Step(
        request.disposition(), request.reasonCode(), request.remarks());
  }

  /**
   * The case after an action, with the warnings of non-blocking validation rules.
   *
   * @param screeningCase the case
   * @param warnings warnings
   */
  public record Outcome(CaseDetail screeningCase, List<String> warnings) {}
}
