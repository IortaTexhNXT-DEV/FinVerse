package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmation;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmationRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeSignoff;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeSignoffRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-off and threshold approval of a comparative (BRID-010, 016; FR-EB-041, 042). The authorised
 * signatory (never its maker) signs it off; the threshold rules are evaluated on the recommended
 * proposals and, when one is met, the cycle waits in THRESHOLD_APPROVAL for an approver holding the
 * rule's permission who is neither the maker nor the programme's account officer. Either may return
 * the comparative to the AO with a reason. The approvers see the items in My Approvals ({@link
 * ComparativeApprovalSource}).
 */
@Service
@Transactional
public class ComparativeApproval {

  private final ComparativeService comparatives;
  private final EbComparativeSignoffRepository signoffs;
  private final EbProposalRepository proposals;
  private final EbClientConfirmationRepository confirmations;
  private final ThresholdEvaluator thresholds;
  private final EbRecords records;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param comparatives comparatives
   * @param signoffs sign-off decisions
   * @param proposals proposals (measures)
   * @param confirmations client confirmations
   * @param thresholds threshold rules
   * @param records cycle look-up
   * @param workflow workflow engine
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ComparativeApproval(
      ComparativeService comparatives,
      EbComparativeSignoffRepository signoffs,
      EbProposalRepository proposals,
      EbClientConfirmationRepository confirmations,
      ThresholdEvaluator thresholds,
      EbRecords records,
      WorkflowService workflow,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.comparatives = comparatives;
    this.signoffs = signoffs;
    this.proposals = proposals;
    this.confirmations = confirmations;
    this.thresholds = thresholds;
    this.records = records;
    this.workflow = workflow;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Signs the comparative off; a threshold rule met sends it to the threshold approval.
   *
   * @param companyId company
   * @param comparativeId comparative for approval
   * @param remarks remarks, may be null
   * @return the comparative
   */
  public EbComparative signOff(Long companyId, Long comparativeId, String remarks) {
    EbComparative comparative = comparatives.require(companyId, comparativeId);
    EbCycle cycle = records.openCycle(companyId, comparative.getCycleId());
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, comparative.maker())) {
      throw new BusinessRuleException(
          "EB_SIGNOFF_MAKER", "A comparative is signed off by someone other than its maker");
    }
    ThresholdEvaluator.Result result =
        thresholds.evaluate(companyId, recommended(comparative), BusinessClock.today(clock));
    comparative.signedOff(result.text(), result.met() ? result.approver() : null);
    record(comparative, EbComparativeSignoff.SIGNOFF, EbComparativeSignoff.APPROVED, remarks);
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE,
        cycle.getId().toString(),
        result.met() ? "approve_to_threshold" : "approve",
        TransitionNote.comment(remarks));
    EbProgramme programme = records.programmeOf(cycle);
    if (result.met()) {
      askApprovers(comparative, programme, result);
    }
    tellAo(comparative, programme, result.met() ? "signed off (threshold approval needed)" : "signed off");
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.AUTHORIZE,
        "Signed off" + (result.met() ? "; threshold rules met: " + result.text() : ""));
    return comparative;
  }

  /**
   * Approves a comparative above the threshold. When the client had already confirmed (the rule
   * was met at confirmation) the cycle returns to CONFIRMED.
   *
   * @param companyId company
   * @param comparativeId comparative waiting for the threshold approval
   * @param remarks remarks, may be null
   * @return the comparative
   */
  public EbComparative approveThreshold(Long companyId, Long comparativeId, String remarks) {
    EbComparative comparative = comparatives.require(companyId, comparativeId);
    EbCycle cycle = records.openCycle(companyId, comparative.getCycleId());
    EbProgramme programme = records.programmeOf(cycle);
    String user = currentUser.username();
    String permission = comparative.getApproverPermission();
    if (permission != null && !currentUser.hasAuthority(permission)) {
      throw new BusinessRuleException(
          "WORKFLOW_ACTION_NOT_PERMITTED", "You are not allowed to approve this threshold");
    }
    if (CurrentUser.sameUser(user, programme.getAccountOfficer())
        || CurrentUser.sameUser(user, comparative.maker())) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_OWN", "The account officer or maker of the comparative cannot approve it");
    }
    comparative.thresholdApproved();
    record(comparative, EbComparativeSignoff.THRESHOLD, EbComparativeSignoff.APPROVED, remarks);
    String key = cycle.getId().toString();
    workflow.systemTransition(EbCodes.ENTITY_CYCLE, key, "approve", TransitionNote.comment(remarks));
    Optional<EbClientConfirmation> confirmed =
        confirmations.findFirstByCycleIdAndStatusOrderByIdDesc(
            cycle.getId(), EbClientConfirmation.ACTIVE);
    if (confirmed.isPresent()) {
      comparative.presented(clock.instant(), user, comparative.getAttachmentId());
      TransitionNote note = TransitionNote.comment("Confirmed earlier by the client");
      workflow.systemTransition(EbCodes.ENTITY_CYCLE, key, "present", note);
      workflow.systemTransition(EbCodes.ENTITY_CYCLE, key, "confirm", note);
    }
    tellAo(comparative, programme, "approved above the threshold");
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.AUTHORIZE,
        "Threshold approved");
    return comparative;
  }

  /**
   * Returns the comparative to the AO with a reason.
   *
   * @param companyId company
   * @param comparativeId comparative for sign-off or threshold approval
   * @param reason why
   * @return the comparative, DRAFT
   */
  public EbComparative returnToAo(Long companyId, Long comparativeId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for 'return'");
    }
    EbComparative comparative = comparatives.require(companyId, comparativeId);
    EbCycle cycle = records.openCycle(companyId, comparative.getCycleId());
    String role =
        comparative.getStatus() == EbComparative.Status.THRESHOLD_APPROVAL
            ? EbComparativeSignoff.THRESHOLD
            : EbComparativeSignoff.SIGNOFF;
    comparative.returned();
    record(comparative, role, EbComparativeSignoff.RETURNED, reason.strip());
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "return", TransitionNote.comment(reason));
    tellAo(comparative, records.programmeOf(cycle), "returned: " + reason.strip());
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.REJECT,
        "Returned to the AO: " + reason.strip());
    return comparative;
  }

  /**
   * The decisions on a comparative.
   *
   * @param comparative comparative
   * @return decisions, oldest first
   */
  @Transactional(readOnly = true)
  public List<EbComparativeSignoff> decisions(EbComparative comparative) {
    return signoffs.findByComparativeIdOrderByIdAsc(comparative.getId());
  }

  /**
   * Tells the threshold approvers that a comparative waits for them.
   *
   * @param comparative comparative
   * @param programme programme
   * @param result rules met
   */
  public void askApprovers(
      EbComparative comparative, EbProgramme programme, ThresholdEvaluator.Result result) {
    notifications.notifyPermission(
        result.approver(),
        new Notice(
            comparative.getComparativeNo() + ": threshold approval",
            programme.getName() + " - " + result.text(),
            EbCodes.COMPARATIVE_LINK + comparative.getId(),
            EbCodes.ENTITY_COMPARATIVE,
            comparative.getId().toString()),
        EbCodes.EVENT_THRESHOLD_APPROVAL);
  }

  private Map<String, ThresholdEvaluator.Measures> recommended(EbComparative comparative) {
    Map<String, ThresholdEvaluator.Measures> measures = new LinkedHashMap<>();
    for (EbComparative.Line line : comparative.getLines()) {
      if (line.getRecommendedProposalId() == null) {
        continue;
      }
      proposals
          .findById(line.getRecommendedProposalId())
          .ifPresent(p -> measures.put(line.getBenefitLine(), measures(p, line.getBenefitLine())));
    }
    return measures;
  }

  /**
   * The TSI and premium of a proposal's benefit line.
   *
   * @param proposal proposal
   * @param line benefit line
   * @return measures
   */
  static ThresholdEvaluator.Measures measures(EbProposal proposal, String line) {
    BigDecimal tsi = proposal.sumInsuredOf(line);
    return new ThresholdEvaluator.Measures(tsi, proposal.premiumOf(line));
  }

  private void record(EbComparative comparative, String role, String decision, String remarks) {
    signoffs.save(
        new EbComparativeSignoff(
            comparative.getId(),
            role,
            currentUser.username(),
            decision,
            remarks,
            clock.instant()));
  }

  private void tellAo(EbComparative comparative, EbProgramme programme, String what) {
    if (CurrentUser.sameUser(programme.getAccountOfficer(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            comparative.getComparativeNo() + ": comparative " + what,
            programme.getName(),
            EbCodes.COMPARATIVE_LINK + comparative.getId(),
            EbCodes.ENTITY_COMPARATIVE,
            comparative.getId().toString()),
        EbCodes.EVENT_COMPARATIVE_DECIDED);
  }
}
