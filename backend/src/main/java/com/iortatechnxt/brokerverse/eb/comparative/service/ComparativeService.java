package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComment;
import com.iortatechnxt.brokerverse.eb.domain.EbCommentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTorRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The comparative analysis of a cycle (BRID-010; FR-EB-041, 043): built from the validated
 * proposals (the latest version of each insurer) once no request is open, stored as a numbered
 * snapshot (EBCA number) with the lowest premium per benefit line as the default recommendation.
 * The AO marks the recommendation and submits it for sign-off ({@link ComparativeApproval}); the
 * comment thread holds internal notes and the client's comments received by e-mail.
 */
@Service
@Transactional
public class ComparativeService {

  private static final Set<EbCycleStage> BUILD_STAGES =
      Set.of(EbCycleStage.INCUMBENT_TERMS, EbCycleStage.PROPOSALS, EbCycleStage.REVISION);

  private final EbComparativeRepository comparatives;
  private final EbCommentRepository comments;
  private final EbProposalRepository proposals;
  private final EbInsurerRequestRepository requests;
  private final EbTorRepository tors;
  private final EbRecords records;
  private final EbParties parties;
  private final LovService lovs;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final EbWorkingDays workingDays;
  private final EbParameters parameters;
  private final EbActivityLog activity;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final ObjectMapper json;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param comparatives comparatives
   * @param comments comments
   * @param proposals proposals
   * @param requests insurer requests
   * @param tors TOR versions
   * @param records cycle look-up
   * @param parties insurer names
   * @param lovs labels
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param workingDays working-day calendar
   * @param parameters EB parameters
   * @param activity TAT stamps
   * @param notifications in-app notices
   * @param audit audit trail
   * @param json JSON mapper (snapshot)
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ComparativeService(
      EbComparativeRepository comparatives,
      EbCommentRepository comments,
      EbProposalRepository proposals,
      EbInsurerRequestRepository requests,
      EbTorRepository tors,
      EbRecords records,
      EbParties parties,
      LovService lovs,
      WorkflowService workflow,
      DocumentNumberService numbers,
      EbWorkingDays workingDays,
      EbParameters parameters,
      EbActivityLog activity,
      NotificationService notifications,
      AuditTrailService audit,
      ObjectMapper json,
      CurrentUser currentUser,
      Clock clock) {
    this.comparatives = comparatives;
    this.comments = comments;
    this.proposals = proposals;
    this.requests = requests;
    this.tors = tors;
    this.records = records;
    this.parties = parties;
    this.lovs = lovs;
    this.workflow = workflow;
    this.numbers = numbers;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.activity = activity;
    this.notifications = notifications;
    this.audit = audit;
    this.json = json;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Builds a new comparative version from the validated proposals.
   *
   * @param companyId company
   * @param cycleId cycle in stage INCUMBENT_TERMS, PROPOSALS or REVISION
   * @return the draft comparative
   */
  public EbComparative build(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (!BUILD_STAGES.contains(cycle.getStage())) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " is not ready for a comparative");
    }
    int open = requests.findByCycleIdAndStatus(cycle.getId(), EbInsurerRequest.Status.OPEN).size();
    if (open > 0) {
      throw new BusinessRuleException(
          "EB_REQUESTS_OPEN",
          open + " insurer requests are still open. Close them or wait for the proposals");
    }
    List<EbProposal> validated = validated(cycle);
    if (validated.isEmpty()) {
      throw new BusinessRuleException(
          "EB_COMPARATIVE_NO_PROPOSAL", "Validate at least one proposal first");
    }
    EbProgramme programme = records.programmeOf(cycle);
    List<String> lines = benefitLines(programme);
    ComparativeMatrix matrix =
        ComparativeMatrix.of(validated, lines, torItems(cycle), labels(companyId));
    List<EbComparative> earlier = comparatives.findByCycleIdOrderByVersionNoDesc(cycle.getId());
    earlier.stream()
        .filter(c -> c.getStatus() != EbComparative.Status.SUPERSEDED)
        .forEach(EbComparative::supersede);
    LocalDate lastProposal =
        validated.stream().map(EbProposal::getReceivedOn).max(Comparator.naturalOrder()).orElseThrow();
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_COMPARATIVE, BusinessClock.currentYear(clock).getValue()));
    EbComparative comparative =
        new EbComparative(
            cycle,
            number,
            earlier.isEmpty() ? 1 : earlier.get(0).getVersionNo() + 1,
            write(matrix),
            workingDays.plus(companyId, lastProposal, parameters.comparativeDays()));
    matrix.lines().forEach(l -> comparative.addLine(l.benefitLine(), l.lowestPremium(), l.lowestProposalId()));
    EbComparative saved = comparatives.save(comparative);
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE,
        cycle.getId().toString(),
        "build_comparative",
        TransitionNote.comment("Comparative " + number + " of " + validated.size() + " proposal(s)"));
    activity.received(cycle, TatActivity.PROPOSAL_TO_CLIENT, number, programme.getAccountOfficer());
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        number,
        AuditAction.CREATE,
        "Version " + saved.getVersionNo() + " built from " + validated.size() + " proposal(s)");
    return saved;
  }

  private List<EbProposal> validated(EbCycle cycle) {
    return proposals
        .findByCycleIdAndStatusInOrderByIdAsc(cycle.getId(), Set.of(EbProposal.Status.VALIDATED));
  }

  private static List<String> benefitLines(EbProgramme programme) {
    return programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .map(EbProgrammeLine::getBenefitLine)
        .distinct()
        .toList();
  }

  private List<EbTorItem> torItems(EbCycle cycle) {
    return tors.findFirstByCycleIdAndStatusOrderByVersionNoDesc(cycle.getId(), EbTor.Status.RELEASED)
        .map(EbTor::getItems)
        .orElse(List.of());
  }

  private ComparativeMatrix.Labels labels(Long companyId) {
    return new ComparativeMatrix.Labels(
        code -> parties.insurerName(companyId, code),
        code -> lovs.label(EbCodes.LOV_BENEFIT_LINE, code),
        code -> lovs.label(EbCodes.LOV_CAPABILITY_FACTOR, code));
  }

  private String write(ComparativeMatrix matrix) {
    try {
      return json.writeValueAsString(matrix);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Comparative snapshot not written", e);
    }
  }

  /**
   * The rows of a comparative.
   *
   * @param comparative comparative
   * @return matrix
   */
  @Transactional(readOnly = true)
  public ComparativeMatrix matrix(EbComparative comparative) {
    try {
      return json.readValue(comparative.getSnapshot(), ComparativeMatrix.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Comparative snapshot not readable", e);
    }
  }

  /**
   * Marks the recommended proposal of the lines and keeps the AO's summary.
   *
   * @param companyId company
   * @param comparativeId draft comparative
   * @param recommendation proposal per benefit line
   * @param summary summary, may be null
   * @return the comparative
   */
  public EbComparative recommend(
      Long companyId, Long comparativeId, Map<String, Long> recommendation, String summary) {
    EbComparative comparative = require(companyId, comparativeId);
    Set<Long> compared =
        Set.copyOf(matrix(comparative).proposals().stream().map(ComparativeMatrix.Column::proposalId).toList());
    recommendation.forEach(
        (line, proposalId) -> {
          EbProposal proposal =
              proposals
                  .findByIdAndCompanyId(proposalId, companyId)
                  .filter(p -> compared.contains(p.getId()) && p.offers(line))
                  .orElseThrow(
                      () ->
                          new BusinessRuleException(
                              "EB_RECOMMENDATION_INVALID",
                              "Recommend a compared proposal that offers " + line));
          comparative.recommend(line, proposal.getId());
        });
    comparative.summarise(summary);
    return comparative;
  }

  /**
   * Submits the comparative for sign-off.
   *
   * @param companyId company
   * @param comparativeId draft comparative
   * @return the comparative
   */
  public EbComparative submit(Long companyId, Long comparativeId) {
    EbComparative comparative = require(companyId, comparativeId);
    EbCycle cycle = records.openCycle(companyId, comparative.getCycleId());
    comparative.submit(clock.instant(), currentUser.username());
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "submit", TransitionNote.NONE);
    notifications.notifyPermission(
        EbCodes.PERMISSION_COMPARATIVE_APPROVE,
        new Notice(
            comparative.getComparativeNo() + ": comparative to sign off",
            records.programmeOf(cycle).getName() + " - " + cycle.getCycleNo(),
            EbCodes.COMPARATIVE_LINK + comparative.getId(),
            EbCodes.ENTITY_COMPARATIVE,
            comparative.getId().toString()),
        EbCodes.EVENT_COMPARATIVE_SIGNOFF);
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.SUBMIT,
        "Submitted for sign-off");
    return comparative;
  }

  /**
   * Adds a comment: an internal note or reply, or the client's comment received by e-mail.
   *
   * @param companyId company
   * @param comparativeId comparative
   * @param client whether it is the client's comment
   * @param text text
   * @param replyTo comment answered, may be null
   * @return the comment
   */
  public EbComment comment(
      Long companyId, Long comparativeId, boolean client, String text, Long replyTo) {
    EbComparative comparative = require(companyId, comparativeId);
    if (text == null || text.isBlank()) {
      throw new BusinessRuleException("EB_COMMENT_EMPTY", "Enter your comment");
    }
    EbComment saved =
        comments.save(
            new EbComment(
                comparative.getId(),
                client ? EbComment.CLIENT : EbComment.INTERNAL,
                text.strip(),
                replyTo));
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.UPDATE,
        client ? "Client comment recorded" : "Comment added");
    return saved;
  }

  /**
   * The comments of a comparative.
   *
   * @param comparative comparative
   * @return comments, oldest first
   */
  @Transactional(readOnly = true)
  public List<EbComment> comments(EbComparative comparative) {
    return comments.findByComparativeIdOrderByIdAsc(comparative.getId());
  }

  /**
   * The comparatives of a programme.
   *
   * @param companyId company
   * @param programmeId programme
   * @return comparatives, latest first
   */
  @Transactional(readOnly = true)
  public List<EbComparative> ofProgramme(Long companyId, Long programmeId) {
    return comparatives.findByProgrammeIdOrderByIdDesc(
        records.programme(companyId, programmeId).getId());
  }

  /**
   * A comparative of a company.
   *
   * @param companyId company
   * @param comparativeId comparative
   * @return comparative
   */
  @Transactional(readOnly = true)
  public EbComparative require(Long companyId, Long comparativeId) {
    return comparatives
        .findByIdAndCompanyId(comparativeId, companyId)
        .orElseThrow(
            () -> new ResourceNotFoundException(EbCodes.ENTITY_COMPARATIVE, comparativeId));
  }
}
