package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequestRepository;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client's changes relayed to insurers (BRID-012; FR-EB-044): the AO captures the requested
 * changes (a TOR item or free text) and selects the insurers with a validated proposal; each
 * receives the template {@code EB_REVISION_RELAY} by e-mail and stays OPEN until its revised
 * proposal is entered. The cycle moves from WITH_CLIENT to REVISION.
 */
@Service
@Transactional
public class RevisionService {

  private final EbRevisionRequestRepository revisions;
  private final EbProposalRepository proposals;
  private final EbRecords records;
  private final EbParties parties;
  private final EbMailer mailer;
  private final EbTemplates templates;
  private final EbWorkingDays workingDays;
  private final EbParameters parameters;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param revisions revision requests
   * @param proposals proposals
   * @param records cycle look-up
   * @param parties insurers
   * @param mailer e-mails
   * @param templates relay template
   * @param workingDays working-day calendar
   * @param parameters EB parameters
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RevisionService(
      EbRevisionRequestRepository revisions,
      EbProposalRepository proposals,
      EbRecords records,
      EbParties parties,
      EbMailer mailer,
      EbTemplates templates,
      EbWorkingDays workingDays,
      EbParameters parameters,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.revisions = revisions;
    this.proposals = proposals;
    this.records = records;
    this.parties = parties;
    this.mailer = mailer;
    this.templates = templates;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Relays the client's changes to insurers.
   *
   * @param companyId company
   * @param cycleId cycle in stage WITH_CLIENT
   * @param input description, requested changes and insurers
   * @return the revision request
   */
  public EbRevisionRequest request(Long companyId, Long cycleId, RevisionInput input) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getStage() != EbCycleStage.WITH_CLIENT) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " is not with the client");
    }
    List<Change> changes =
        input.changes() == null
            ? List.of()
            : input.changes().stream()
                .filter(c -> c.change() != null && !c.change().isBlank())
                .toList();
    if (changes.isEmpty()) {
      throw new BusinessRuleException("EB_REVISION_EMPTY", "Add at least one requested change");
    }
    Set<String> codes = new LinkedHashSet<>();
    if (input.insurerCodes() != null) {
      input.insurerCodes().stream().filter(c -> c != null && !c.isBlank()).forEach(codes::add);
    }
    if (codes.isEmpty()) {
      throw new BusinessRuleException("EB_INSURER_REQUIRED", "Select the insurers");
    }
    LocalDate due =
        workingDays.plus(companyId, BusinessClock.today(clock), parameters.proposalReplyDays());
    EbRevisionRequest revision =
        new EbRevisionRequest(
            cycle,
            revisions.findByCycleIdOrderByRevisionNoAsc(cycle.getId()).size() + 1,
            input.description(),
            new EbInsurerRequest.Sending(clock.instant(), currentUser.username(), due));
    changes.forEach(c -> revision.addItem(c.torItemId(), c.change().strip()));
    EbProgramme programme = records.programmeOf(cycle);
    for (String code : codes) {
      InsurerProfile insurer = parties.insurer(companyId, code);
      validatedProposal(cycle, insurer);
      revision.addTarget(insurer.getPartyCode());
    }
    EbRevisionRequest saved = revisions.save(revision);
    for (EbRevisionRequest.Target target : saved.getTargets()) {
      InsurerProfile insurer = parties.insurer(companyId, target.getInsurerCode());
      target.sentAs(relay(programme, cycle, saved, insurer, validatedProposal(cycle, insurer)));
    }
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE,
        cycle.getId().toString(),
        "request_revision",
        TransitionNote.comment(
            changes.size() + " change(s) relayed to " + codes.size() + " insurer(s)"));
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.UPDATE,
        "Revision " + saved.getRevisionNo() + " relayed to " + String.join(", ", codes));
    return saved;
  }

  private EbProposal validatedProposal(EbCycle cycle, InsurerProfile insurer) {
    return proposals
        .findFirstByCycleIdAndInsurerCodeOrderByVersionNoDesc(cycle.getId(), insurer.getPartyCode())
        .filter(p -> p.getStatus() == EbProposal.Status.VALIDATED)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_REVISION_NO_PROPOSAL",
                    insurer.getName() + " has no validated proposal to revise"));
  }

  private Long relay(
      EbProgramme programme,
      EbCycle cycle,
      EbRevisionRequest revision,
      InsurerProfile insurer,
      EbProposal proposal) {
    Map<String, Object> values = parties.values(programme, cycle);
    values.put("insurerName", insurer.getName());
    values.put("proposalNo", proposal.getProposalNo());
    values.put("dueDate", EbParties.date(revision.getDueDate()));
    values.put(
        "revisionItems",
        revision.getItems().stream()
            .map(i -> i.getSortOrder() + ". " + i.getRequestedChange())
            .collect(Collectors.joining("\n")));
    MergedText text = templates.merge(EbCodes.TEMPLATE_REVISION_RELAY, values);
    return mailer
        .send(
            programme.getCompanyId(),
            EbCodes.PURPOSE_RFP,
            new Mail(
                EbParties.mailboxes(insurer),
                parties.aoCopy(programme),
                text.title(),
                text.text(),
                List.of()),
            new RecordLink(
                EbCodes.ENTITY_CYCLE,
                cycle.getId().toString(),
                cycle.getCycleNo() + "/R" + revision.getRevisionNo()))
        .messageId();
  }

  /**
   * The revision requests of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return revisions, oldest first
   */
  @Transactional(readOnly = true)
  public List<EbRevisionRequest> ofCycle(Long companyId, Long cycleId) {
    return revisions.findByCycleIdOrderByRevisionNoAsc(records.cycle(companyId, cycleId).getId());
  }

  /**
   * The changes the client asks.
   *
   * @param description description, may be null
   * @param changes requested changes, at least one
   * @param insurerCodes insurers to relay to, at least one
   */
  public record RevisionInput(
      String description, List<Change> changes, List<String> insurerCodes) {}

  /**
   * A requested change.
   *
   * @param torItemId TOR item, may be null (free text)
   * @param change the change
   */
  public record Change(Long torItemId, String change) {}
}
