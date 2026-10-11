package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVote;
import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVoteRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AML Committee decisions (SNSRP-704; FR-SS-064): each member records one decision per round with
 * remarks (APPROVE_STR, NO_STR or COMMITTEE_RETURN); the rule of {@code SCR_COMMITTEE_RULE} and
 * {@code SCR_COMMITTEE_SIZE} ({@link CommitteeRule}) finalises the case to STR preparation, closure
 * or back to Compliance, and Compliance is notified.
 */
@Service
@Transactional
public class CommitteeService {

  /** Committee rule parameter (SQ15). */
  static final String RULE_PARAMETER = "SCR_COMMITTEE_RULE";

  /** Committee size parameter. */
  static final String SIZE_PARAMETER = "SCR_COMMITTEE_SIZE";

  /** Approve the STR. */
  static final String APPROVE_STR = "APPROVE_STR";

  /** No STR. */
  static final String NO_STR = "NO_STR";

  private static final int DEFAULT_SIZE = 5;

  private final ScreeningCaseRepository cases;
  private final CommitteeVoteRepository votes;
  private final CaseAccess access;
  private final CaseValidator validator;
  private final CaseMover mover;
  private final CaseNotifier notifier;
  private final CaseTimeline timeline;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param votes votes
   * @param access case access
   * @param validator dispositions
   * @param mover transitions
   * @param notifier notices
   * @param timeline case timeline
   * @param parameters business parameters
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the committee step
  public CommitteeService(
      ScreeningCaseRepository cases,
      CommitteeVoteRepository votes,
      CaseAccess access,
      CaseValidator validator,
      CaseMover mover,
      CaseNotifier notifier,
      CaseTimeline timeline,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.cases = cases;
    this.votes = votes;
    this.access = access;
    this.validator = validator;
    this.mover = mover;
    this.notifier = notifier;
    this.timeline = timeline;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a member's decision and finalises the round when the rule is met.
   *
   * @param caseId the case
   * @param decision APPROVE_STR, NO_STR or COMMITTEE_RETURN
   * @param remarks the remarks
   * @return the vote
   */
  public CommitteeVote vote(Long caseId, String decision, String remarks) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requireActor(c, "record a decision", EnumSet.of(CaseStage.AML_COMMITTEE));
    String code = CaseChecks.require(decision, "SCR_DECISION_REQUIRED", "Select your decision");
    String text = CaseChecks.require(remarks, "SCR_REMARKS_REQUIRED", "Enter your remarks");
    CaseChecks.requireAllowed(validator, CaseStage.AML_COMMITTEE, code);
    String member = access.user();
    if (votes.existsByCaseIdAndRoundNoAndMemberIgnoreCase(
        c.getId(), c.getCommitteeRound(), member)) {
      throw new BusinessRuleException(
          "SCR_ALREADY_VOTED", "You have already recorded your decision on this case");
    }
    CommitteeVote vote = votes.save(new CommitteeVote(c, member, code, text, clock.instant()));
    timeline.record(c, CaseEventType.COMMITTEE_VOTE, EventFacts.change(null, code, code, text));
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.AUTHORIZE, "Committee vote " + code);
    finalise(c);
    return vote;
  }

  private void finalise(ScreeningCase c) {
    String rule = parameters.text(RULE_PARAMETER, "MAJORITY");
    int size = parameters.intValue(SIZE_PARAMETER, DEFAULT_SIZE);
    List<String> decisions =
        votes.findByCaseIdAndRoundNoOrderByIdAsc(c.getId(), c.getCommitteeRound()).stream()
            .map(CommitteeVote::getDecision)
            .toList();
    Optional<String> result = CommitteeRule.decide(rule, size, decisions);
    if (result.isEmpty()) {
      return;
    }
    String decision = result.get();
    String note =
        CommitteeRule.NO_MAJORITY.equals(decision)
            ? "no majority (" + rule + " of " + size + ")"
            : decision + " (" + rule + " of " + size + ", " + decisions.size() + " vote(s))";
    c.committeeDecided(decision, clock.instant());
    timeline.record(
        c, CaseEventType.COMMITTEE_DECISION, EventFacts.change(null, decision, decision, note));
    if (APPROVE_STR.equals(decision)) {
      c.requireStr(true);
      mover.system(c, "finalise_str", CaseChecks.note(null, note));
    } else if (NO_STR.equals(decision)) {
      mover.system(c, "finalise_no_str", CaseChecks.note(null, note));
    } else {
      mover.system(c, "finalise_return", CaseChecks.note(null, note));
    }
    if (c.isOpen()) {
      mover.assign(
          c,
          null,
          CaseEventType.ASSIGNED,
          CaseMover.AssignFacts.auto("Committee decision: " + note));
    }
    notifier.owner(
        c, CaseCodes.COMPLIANCE_REVIEW, CaseCodes.EVENT_FOR_APPROVAL, "committee decided: " + note);
  }
}
