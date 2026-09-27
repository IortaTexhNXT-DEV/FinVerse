package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTorRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer proposals of a cycle (BRID-005.02, 005.03, 010, 015; FR-EB-040, 045). Without the portal
 * the AO enters each proposal from the insurer's e-mail, with the insurer's document attached
 * (stored as {@code EB_PROPOSAL}, or {@code EB_INDICATIVE_PROPOSAL} for the incumbent's indicative
 * terms, source INSURER). The kind follows from the cycle: the incumbent's indicative terms on a
 * renewal that stays, a proposal answering the insurer's open request, a revised proposal answering
 * an open revision (every requested change on a TOR item must be answered). A new version
 * supersedes the insurer's earlier one. Proposals are SUBMITTED until the AO validates them; only
 * validated proposals enter the comparative.
 */
@Service
@Transactional
public class ProposalService {

  private static final Set<EbCycleStage> ENTRY_STAGES =
      Set.of(EbCycleStage.PROPOSALS, EbCycleStage.INCUMBENT_TERMS, EbCycleStage.REVISION);

  private static final String DEFAULT_CURRENCY = "PHP";

  private final EbProposalRepository proposals;
  private final EbInsurerRequestRepository requests;
  private final EbRevisionRequestRepository revisions;
  private final EbTorRepository tors;
  private final EbRecords records;
  private final EbParties parties;
  private final ProposalRules rules;
  private final EbDocumentService documents;
  private final EbMailer mailer;
  private final DocumentNumberService numbers;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals proposals
   * @param requests insurer requests
   * @param revisions revision requests
   * @param tors TOR versions
   * @param records cycle look-up
   * @param parties insurers
   * @param rules input checks
   * @param documents EB document register
   * @param mailer e-mails
   * @param numbers document numbers
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ProposalService(
      EbProposalRepository proposals,
      EbInsurerRequestRepository requests,
      EbRevisionRequestRepository revisions,
      EbTorRepository tors,
      EbRecords records,
      EbParties parties,
      ProposalRules rules,
      EbDocumentService documents,
      EbMailer mailer,
      DocumentNumberService numbers,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.requests = requests;
    this.revisions = revisions;
    this.tors = tors;
    this.records = records;
    this.parties = parties;
    this.rules = rules;
    this.documents = documents;
    this.mailer = mailer;
    this.numbers = numbers;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a proposal entered by the AO.
   *
   * @param companyId company
   * @param cycleId cycle in stage PROPOSALS, INCUMBENT_TERMS or REVISION
   * @param input proposal
   * @return the proposal, SUBMITTED
   */
  public EbProposal record(Long companyId, Long cycleId, ProposalInput input) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (!ENTRY_STAGES.contains(cycle.getStage())) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " does not take proposals now");
    }
    EbProgramme programme = records.programmeOf(cycle);
    InsurerProfile insurer = parties.insurer(companyId, input.insurerCode());
    LocalDate today = BusinessClock.today(clock);
    rules.check(input, torItemIds(cycle), today);
    Origin origin = origin(programme, cycle, insurer, input);
    Long attachment = storeDocument(cycle, insurer, origin.kind(), input);
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_PROPOSAL, BusinessClock.currentYear(clock).getValue()));
    Optional<EbProposal> previous =
        proposals.findFirstByCycleIdAndInsurerCodeOrderByVersionNoDesc(
            cycle.getId(), insurer.getPartyCode());
    previous.ifPresent(EbProposal::supersede);
    EbProposal proposal =
        new EbProposal(
            cycle,
            number,
            new EbProposal.Origin(
                insurer.getPartyCode(),
                origin.kind(),
                previous.map(p -> p.getVersionNo() + 1).orElse(1),
                origin.request().map(EbInsurerRequest::getId).orElse(null),
                origin.revision().map(EbRevisionRequest::getId).orElse(null)),
            new EbProposal.Content(
                input.receivedOn() == null ? today : input.receivedOn(),
                input.validUntil(),
                input.currency() == null || input.currency().isBlank()
                    ? DEFAULT_CURRENCY
                    : input.currency().strip(),
                input.terms(),
                input.exclusions(),
                attachment));
    input.lines().forEach(proposal::addLine);
    input.items().forEach(proposal::addItem);
    input.factors().forEach(proposal::addFactor);
    EbProposal saved = proposals.save(proposal);
    answered(saved, origin);
    audit.record(
        EbCodes.ENTITY_PROPOSAL,
        number,
        AuditAction.CREATE,
        origin.kind() + " of " + insurer.getName() + " version " + saved.getVersionNo()
            + " entered on " + cycle.getCycleNo());
    return saved;
  }

  private Set<Long> torItemIds(EbCycle cycle) {
    return tors.findFirstByCycleIdAndStatusOrderByVersionNoDesc(cycle.getId(), EbTor.Status.RELEASED)
        .map(t -> t.getItems().stream().map(EbTorItem::getId).collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  private Origin origin(
      EbProgramme programme, EbCycle cycle, InsurerProfile insurer, ProposalInput input) {
    String code = insurer.getPartyCode();
    Optional<EbRevisionRequest> revision =
        revisions.findByCycleIdOrderByRevisionNoAsc(cycle.getId()).stream()
            .filter(r -> r.openTarget(code).isPresent())
            .reduce((a, b) -> b);
    if (revision.isPresent()) {
      ProposalRules.requireRevisionAnswered(revision.get(), input.items());
      return new Origin(EbProposal.Kind.REVISED, Optional.empty(), revision);
    }
    Optional<EbInsurerRequest> request =
        requests.findFirstByCycleIdAndInsurerCodeAndStatus(
            cycle.getId(), code, EbInsurerRequest.Status.OPEN);
    if (request.isPresent()) {
      return new Origin(EbProposal.Kind.PROPOSAL, request, Optional.empty());
    }
    boolean incumbent =
        programme.getLines().stream()
            .filter(EbProgrammeLine::isActive)
            .anyMatch(l -> code.equals(l.getIncumbentInsurer()));
    if (incumbent) {
      return new Origin(EbProposal.Kind.INCUMBENT_INDICATIVE, Optional.empty(), Optional.empty());
    }
    throw new BusinessRuleException(
        "EB_PROPOSAL_NOT_REQUESTED",
        insurer.getName() + " has no open request for proposal or revision on this cycle");
  }

  private Long storeDocument(
      EbCycle cycle, InsurerProfile insurer, EbProposal.Kind kind, ProposalInput input) {
    String type =
        kind == EbProposal.Kind.INCUMBENT_INDICATIVE
            ? EbDocumentTypes.INDICATIVE_PROPOSAL
            : EbDocumentTypes.PROPOSAL;
    return documents
        .store(
            cycle,
            new Registration(
                type,
                EbDocumentTypes.PROPOSAL_PROCESS,
                EbDocumentSource.INSURER,
                false,
                "Proposal of " + insurer.getName()),
            List.of(input.document()))
        .get(0)
        .getAttachmentId();
  }

  private void answered(EbProposal proposal, Origin origin) {
    origin
        .request()
        .ifPresent(
            r -> {
              r.responded();
              activity.released(TatActivity.QUOTATION, r.getRequestNo(), proposal.getProposalNo());
            });
    origin
        .revision()
        .ifPresent(
            r ->
                r.openTarget(proposal.getInsurerCode())
                    .ifPresent(t -> r.answer(t, proposal.getId())));
  }

  /**
   * Validates a proposal: it enters the comparative.
   *
   * @param companyId company
   * @param proposalId proposal
   * @return the proposal
   */
  public EbProposal validate(Long companyId, Long proposalId) {
    EbProposal proposal = require(companyId, proposalId);
    records.openCycle(companyId, proposal.getCycleId());
    proposal.validate(currentUser.username(), clock.instant());
    audit.record(
        EbCodes.ENTITY_PROPOSAL, proposal.getProposalNo(), AuditAction.AUTHORIZE, "Validated");
    return proposal;
  }

  /**
   * Rejects a proposal with a reason and tells the insurer by e-mail.
   *
   * @param companyId company
   * @param proposalId proposal
   * @param reason why
   * @return the proposal
   */
  public EbProposal reject(Long companyId, Long proposalId, String reason) {
    EbProposal proposal = require(companyId, proposalId);
    EbCycle cycle = records.openCycle(companyId, proposal.getCycleId());
    proposal.reject(reason, currentUser.username(), clock.instant());
    EbProgramme programme = records.programmeOf(cycle);
    InsurerProfile insurer = parties.insurer(companyId, proposal.getInsurerCode());
    mailer.send(
        companyId,
        EbCodes.PURPOSE_RFP,
        new Mail(
            EbParties.mailboxes(insurer),
            parties.aoCopy(programme),
            "Proposal " + proposal.getProposalNo() + " - " + programme.getClientName(),
            "Dear "
                + insurer.getName()
                + ",\n\nWe cannot accept your proposal for "
                + programme.getClientName()
                + " as submitted: "
                + proposal.getRejectReason()
                + "\n\nPlease send us a corrected proposal.\n\n"
                + parties.aoName(programme),
            List.of()),
        new RecordLink(
            EbCodes.ENTITY_PROPOSAL, proposal.getId().toString(), proposal.getProposalNo()));
    audit.record(
        EbCodes.ENTITY_PROPOSAL,
        proposal.getProposalNo(),
        AuditAction.REJECT,
        "Rejected: " + proposal.getRejectReason());
    return proposal;
  }

  /**
   * The proposals of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return proposals, oldest first
   */
  @Transactional(readOnly = true)
  public List<EbProposal> ofCycle(Long companyId, Long cycleId) {
    return proposals.findByCycleIdOrderByIdAsc(records.cycle(companyId, cycleId).getId());
  }

  /**
   * A proposal of a company.
   *
   * @param companyId company
   * @param proposalId proposal
   * @return proposal
   */
  @Transactional(readOnly = true)
  public EbProposal require(Long companyId, Long proposalId) {
    return proposals
        .findByIdAndCompanyId(proposalId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_PROPOSAL, proposalId));
  }

  private record Origin(
      EbProposal.Kind kind,
      Optional<EbInsurerRequest> request,
      Optional<EbRevisionRequest> revision) {}
}
