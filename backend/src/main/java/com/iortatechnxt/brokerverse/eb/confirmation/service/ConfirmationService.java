package com.iortatechnxt.brokerverse.eb.confirmation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeApproval;
import com.iortatechnxt.brokerverse.eb.comparative.service.ThresholdEvaluator;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmation;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmationRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client's confirmation of the chosen proposals (BRID-017; FR-EB-046): recorded by the AO with
 * its channel (e-mail or signed document) and evidence (stored as {@code EB_CLIENT_CONFIRMATION}),
 * one validated proposal per programme line. The threshold rules are evaluated again on the chosen
 * proposals: a rule the sign-off did not meet sends the cycle to THRESHOLD_APPROVAL ({@code
 * reconfirm_threshold}); otherwise the cycle is CONFIRMED. A confirmation is voided before the
 * placement and recorded again; the placement itself is {@link PlacementTrigger}.
 */
@Service
@Transactional
public class ConfirmationService {

  private static final Set<String> CHANNELS = Set.of("EMAIL", "SIGNED_DOCUMENT");

  private final EbClientConfirmationRepository confirmations;
  private final EbComparativeRepository comparatives;
  private final EbProposalRepository proposals;
  private final EbRecords records;
  private final ThresholdEvaluator thresholds;
  private final ComparativeApproval approval;
  private final EbDocumentService documents;
  private final WorkflowService workflow;
  private final EbActivityLog activity;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param confirmations confirmations
   * @param comparatives comparatives
   * @param proposals proposals
   * @param records cycle look-up
   * @param thresholds threshold rules
   * @param approval threshold approvers
   * @param documents EB document register
   * @param workflow workflow engine
   * @param activity TAT stamps
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ConfirmationService(
      EbClientConfirmationRepository confirmations,
      EbComparativeRepository comparatives,
      EbProposalRepository proposals,
      EbRecords records,
      ThresholdEvaluator thresholds,
      ComparativeApproval approval,
      EbDocumentService documents,
      WorkflowService workflow,
      EbActivityLog activity,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.confirmations = confirmations;
    this.comparatives = comparatives;
    this.proposals = proposals;
    this.records = records;
    this.thresholds = thresholds;
    this.approval = approval;
    this.documents = documents;
    this.workflow = workflow;
    this.activity = activity;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records the client's confirmation.
   *
   * @param companyId company
   * @param cycleId cycle WITH_CLIENT (or CONFIRMED after a voided confirmation)
   * @param input channel, date, choices and evidence
   * @return the confirmation
   */
  public EbClientConfirmation confirm(Long companyId, Long cycleId, ConfirmationInput input) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    boolean again = cycle.getStage() == EbCycleStage.CONFIRMED && active(cycle).isEmpty();
    if (cycle.getStage() != EbCycleStage.WITH_CLIENT && !again) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " is not waiting for the client");
    }
    EbComparative comparative = presented(cycle);
    LocalDate on = checkEvidence(input);
    EbProgramme programme = records.programmeOf(cycle);
    Map<EbProgrammeLine, EbProposal> chosen = chosen(programme, cycle, input);
    Long evidence = storeEvidence(cycle, input);
    EbClientConfirmation confirmation =
        new EbClientConfirmation(
            cycle,
            comparative.getId(),
            new EbClientConfirmation.Evidence(input.channel(), on, evidence, input.remarks()));
    chosen.forEach(
        (line, p) ->
            confirmation.addLine(
                new EbClientConfirmation.Choice(
                    line.getLineNo(),
                    line.getBenefitLine(),
                    p.getId(),
                    p.getInsurerCode(),
                    p.premiumOf(line.getBenefitLine()),
                    p.sumInsuredOf(line.getBenefitLine()))));
    EbClientConfirmation saved = confirmations.save(confirmation);
    decide(cycle, programme, comparative, chosen, again);
    activity.done(
        cycle,
        TatActivity.CONFIRMATION,
        cycle.getCycleNo(),
        currentUser.username(),
        input.channel());
    tellAo(programme, cycle);
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.UPDATE,
        "Client confirmation recorded ("
            + input.channel()
            + ", "
            + on
            + ") for "
            + chosen.size()
            + " line(s)");
    return saved;
  }

  private Long storeEvidence(EbCycle cycle, ConfirmationInput input) {
    return documents
        .store(
            cycle,
            new Registration(
                EbDocumentTypes.CLIENT_CONFIRMATION,
                placementProcess(cycle),
                EbDocumentSource.CLIENT,
                true,
                "Client confirmation"),
            List.of(input.evidence()))
        .get(0)
        .getAttachmentId();
  }

  private EbComparative presented(EbCycle cycle) {
    return comparatives.findByCycleIdOrderByVersionNoDesc(cycle.getId()).stream()
        .filter(c -> c.getStatus() == EbComparative.Status.PRESENTED)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_COMPARATIVE_NOT_PRESENTED", "Present the comparative to the client first"));
  }

  private LocalDate checkEvidence(ConfirmationInput input) {
    if (input.channel() == null || !CHANNELS.contains(input.channel())) {
      throw new BusinessRuleException(
          "EB_CONFIRMATION_CHANNEL", "Select how the client confirmed: e-mail or signed document");
    }
    if (input.evidence() == null) {
      throw new BusinessRuleException(
          "EB_CONFIRMATION_EVIDENCE", "Attach the client's confirmation");
    }
    LocalDate today = BusinessClock.today(clock);
    LocalDate on = input.confirmedOn() == null ? today : input.confirmedOn();
    if (on.isAfter(today)) {
      throw new BusinessRuleException(
          "EB_CONFIRMATION_DATE_FUTURE", "The confirmation date cannot be after today");
    }
    return on;
  }

  private Map<EbProgrammeLine, EbProposal> chosen(
      EbProgramme programme, EbCycle cycle, ConfirmationInput input) {
    Map<Integer, Long> byLine = new HashMap<>();
    input.choices().forEach(c -> byLine.put(c.lineNo(), c.proposalId()));
    Map<EbProgrammeLine, EbProposal> chosen = new LinkedHashMap<>();
    for (EbProgrammeLine line : programme.getLines()) {
      if (!line.isActive()) {
        continue;
      }
      Long id = byLine.get(line.getLineNo());
      EbProposal proposal =
          Optional.ofNullable(id)
              .flatMap(i -> proposals.findByIdAndCompanyId(i, cycle.getCompanyId()))
              .filter(p -> p.getCycleId().equals(cycle.getId()))
              .filter(p -> p.getStatus() == EbProposal.Status.VALIDATED)
              .filter(p -> p.offers(line.getBenefitLine()))
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "EB_CONFIRMATION_CHOICE",
                          "Select the chosen proposal of line "
                              + line.getLineNo()
                              + " ("
                              + line.getBenefitLine()
                              + ")"));
      chosen.put(line, proposal);
    }
    return chosen;
  }

  private void decide(
      EbCycle cycle,
      EbProgramme programme,
      EbComparative comparative,
      Map<EbProgrammeLine, EbProposal> chosen,
      boolean again) {
    Map<String, ThresholdEvaluator.Measures> measures = new LinkedHashMap<>();
    chosen.forEach(
        (line, p) ->
            measures.put(
                line.getBenefitLine(),
                new ThresholdEvaluator.Measures(
                    p.sumInsuredOf(line.getBenefitLine()), p.premiumOf(line.getBenefitLine()))));
    ThresholdEvaluator.Result result =
        thresholds.evaluate(cycle.getCompanyId(), measures, BusinessClock.today(clock));
    List<String> signedOff =
        comparative.getThresholdRules() == null
            ? List.of()
            : List.of(comparative.getThresholdRules().split("; "));
    boolean newRule = result.rules().stream().anyMatch(r -> !signedOff.contains(r));
    if (newRule && again) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_PENDING",
          "Cycle " + cycle.getCycleNo() + " waits for the threshold approval");
    }
    if (newRule) {
      comparative.needsThresholdApproval(result.text(), result.approver());
      workflow.systemTransition(
          EbCodes.ENTITY_CYCLE,
          cycle.getId().toString(),
          "reconfirm_threshold",
          TransitionNote.comment("The chosen proposals meet " + result.text()));
      approval.askApprovers(comparative, programme, result);
    } else if (!again) {
      workflow.systemTransition(
          EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "confirm", TransitionNote.NONE);
    }
  }

  /**
   * Voids the active confirmation before the placement.
   *
   * @param companyId company
   * @param cycleId cycle
   * @param reason why
   * @return the voided confirmation
   */
  public EbClientConfirmation voidConfirmation(Long companyId, Long cycleId, String reason) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getStage() != EbCycleStage.CONFIRMED
        && cycle.getStage() != EbCycleStage.THRESHOLD_APPROVAL) {
      throw new BusinessRuleException(
          "EB_CONFIRMATION_PLACED",
          "The confirmation of cycle " + cycle.getCycleNo() + " can no longer be voided");
    }
    EbClientConfirmation confirmation =
        active(cycle)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "EB_CONFIRMATION_REQUIRED", "Record the client's confirmation first"));
    confirmation.voidWith(reason);
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.REVERSE,
        "Client confirmation voided: " + confirmation.getVoidReason());
    return confirmation;
  }

  /**
   * The active confirmation of a cycle.
   *
   * @param cycle cycle
   * @return confirmation
   */
  @Transactional(readOnly = true)
  public Optional<EbClientConfirmation> active(EbCycle cycle) {
    return confirmations.findFirstByCycleIdAndStatusOrderByIdDesc(
        cycle.getId(), EbClientConfirmation.ACTIVE);
  }

  /**
   * The confirmations of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return confirmations, latest first
   */
  @Transactional(readOnly = true)
  public List<EbClientConfirmation> ofCycle(Long companyId, Long cycleId) {
    return confirmations.findByCycleIdOrderByIdDesc(records.cycle(companyId, cycleId).getId());
  }

  /**
   * The process of the placement documents of a cycle.
   *
   * @param cycle cycle
   * @return RENEWAL_PLACEMENT or NB_PLACEMENT
   */
  static String placementProcess(EbCycle cycle) {
    return EbDocumentService.placementProcess(cycle);
  }

  private void tellAo(EbProgramme programme, EbCycle cycle) {
    if (CurrentUser.sameUser(programme.getAccountOfficer(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            cycle.getCycleNo() + ": client confirmation recorded",
            programme.getName(),
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=confirmation",
            EbCodes.ENTITY_CYCLE,
            cycle.getId().toString()),
        EbCodes.EVENT_CLIENT_CONFIRMED);
  }
}
