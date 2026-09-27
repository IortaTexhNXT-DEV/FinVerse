package com.iortatechnxt.brokerverse.eb.confirmation.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.document.service.CycleDocuments;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmation;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalRepository;
import com.iortatechnxt.brokerverse.eb.placement.service.EbPlacementService;
import com.iortatechnxt.brokerverse.eb.placement.service.LinePlacement;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.RequiredDocumentCheck;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trigger Placement (BRID-017, 019; FR-EB-046): on a CONFIRMED cycle with its active confirmation,
 * refused while a threshold approval is pending, a document required for the placement process
 * (NB_PLACEMENT or RENEWAL_PLACEMENT) is missing or, for a chosen insurer not accredited on the
 * business date, the ISACOM approval is missing (EBQ24). One account per confirmed line is created
 * through {@link EbPlacementService#trigger} from the chosen proposal, the placement documents
 * (confirmation, proposals, TOR, BOR, required documents) are linked to each account and every
 * account is submitted to Processing.
 */
@Service
@Transactional
public class PlacementTrigger {

  private final ConfirmationService confirmations;
  private final EbComparativeRepository comparatives;
  private final EbProposalRepository proposals;
  private final EbRecords records;
  private final EbParties parties;
  private final RequiredDocumentCheck required;
  private final CycleDocuments cycleDocuments;
  private final EbDocumentService documents;
  private final PlacementDrafts drafts;
  private final EbPlacementService placement;
  private final AccountService accounts;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param confirmations client confirmations
   * @param comparatives comparatives (threshold approval)
   * @param proposals chosen proposals
   * @param records cycle look-up
   * @param parties insurer accreditation
   * @param required required documents
   * @param cycleDocuments documents of the cycle
   * @param documents document links
   * @param drafts account drafts of the lines
   * @param placement account creation
   * @param accounts account submission
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public PlacementTrigger(
      ConfirmationService confirmations,
      EbComparativeRepository comparatives,
      EbProposalRepository proposals,
      EbRecords records,
      EbParties parties,
      RequiredDocumentCheck required,
      CycleDocuments cycleDocuments,
      EbDocumentService documents,
      PlacementDrafts drafts,
      EbPlacementService placement,
      AccountService accounts,
      Clock clock) {
    this.confirmations = confirmations;
    this.comparatives = comparatives;
    this.proposals = proposals;
    this.records = records;
    this.parties = parties;
    this.required = required;
    this.cycleDocuments = cycleDocuments;
    this.documents = documents;
    this.drafts = drafts;
    this.placement = placement;
    this.accounts = accounts;
    this.clock = clock;
  }

  /**
   * Triggers the placement of a confirmed cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the accounts created and submitted
   */
  public List<Account> trigger(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    boolean pending =
        cycle.getStage() == EbCycleStage.THRESHOLD_APPROVAL
            || comparatives.findByCycleIdOrderByVersionNoDesc(cycle.getId()).stream()
                .anyMatch(c -> c.getStatus() == EbComparative.Status.THRESHOLD_APPROVAL);
    if (pending) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_PENDING", "Cycle " + cycle.getCycleNo() + " waits for the threshold approval");
    }
    EbClientConfirmation confirmation =
        confirmations
            .active(cycle)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "EB_CONFIRMATION_REQUIRED", "Record the client's confirmation first"));
    EbProgramme programme = records.programmeOf(cycle);
    Map<Long, EbProposal> chosen = new HashMap<>();
    confirmation.getLines().forEach(l -> chosen.put(l.getProposalId(), proposals.findById(l.getProposalId()).orElseThrow()));
    LocalDate today = BusinessClock.today(clock);
    String process = ConfirmationService.placementProcess(cycle);
    List<String> lines = confirmation.getLines().stream().map(EbClientConfirmation.Line::getBenefitLine).distinct().toList();
    Set<String> present = cycleDocuments.presentTypes(cycle, today);
    required.require(companyId, process, lines, present);
    requireIsacom(companyId, confirmation, present, today);
    List<LinePlacement> placements = new ArrayList<>();
    for (EbClientConfirmation.Line line : confirmation.getLines()) {
      placements.add(drafts.placement(programme, cycle, line, chosen.get(line.getProposalId())));
    }
    List<Account> created = placement.trigger(companyId, cycle.getId(), placements);
    List<Long> files = placementFiles(cycle, confirmation, chosen, process, lines, today);
    for (int i = 0; i < created.size(); i++) {
      Account account = created.get(i);
      confirmation.getLines().get(i).placedAs(account.getArn());
      documents.linkAll(
          files, List.of(new AttachmentTarget("Account", account.getId().toString())), process);
      accounts.submit(account.getId(), "Placement of " + cycle.getCycleNo());
    }
    return created;
  }

  private void requireIsacom(
      Long companyId, EbClientConfirmation confirmation, Set<String> present, LocalDate today) {
    for (EbClientConfirmation.Line line : confirmation.getLines()) {
      InsurerProfile insurer = parties.insurer(companyId, line.getInsurerCode());
      if (!insurer.isAccreditedOn(today) && !present.contains(EbDocumentTypes.ISACOM_APPROVAL)) {
        throw new BusinessRuleException(
            "EB_ISACOM_REQUIRED",
            insurer.getName() + " is not accredited: add the ISACOM approval before placement");
      }
    }
  }

  private List<Long> placementFiles(
      EbCycle cycle,
      EbClientConfirmation confirmation,
      Map<Long, EbProposal> chosen,
      String process,
      List<String> lines,
      LocalDate today) {
    Set<String> types = new LinkedHashSet<>(List.of(EbDocumentTypes.TOR, EbDocumentTypes.BOR, EbDocumentTypes.MASTERLIST));
    required.of(cycle.getCompanyId(), process, lines).forEach(r -> types.add(r.getDocumentType()));
    Set<Long> files = new LinkedHashSet<>();
    files.add(confirmation.getEvidenceAttachmentId());
    chosen.values().forEach(p -> files.add(p.getAttachmentId()));
    files.addAll(cycleDocuments.files(cycle, types, today));
    return List.copyOf(files);
  }
}
