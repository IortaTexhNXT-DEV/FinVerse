package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.realPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeApproval;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeExport;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativePresenter;
import com.iortatechnxt.brokerverse.eb.comparative.service.EbComparativeService;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationInput;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationService;
import com.iortatechnxt.brokerverse.eb.confirmation.service.PlacementTrigger;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.franchise.service.FranchiseService;
import com.iortatechnxt.brokerverse.eb.market.service.EbProposalService;
import com.iortatechnxt.brokerverse.eb.market.service.InsurerRequestService;
import com.iortatechnxt.brokerverse.eb.market.service.ProposalInput;
import com.iortatechnxt.brokerverse.eb.market.service.RevisionService;
import com.iortatechnxt.brokerverse.eb.market.service.TorService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The marketing steps of a new-business cycle end to end (FR-EB-031 to 046; waves E1-B / E2):
 * franchise requests and decisions, the TOR and the insurer requests, proposals entered by the AO,
 * the comparative with its sign-off (four eyes) and threshold approval, the presentation, the
 * client's revision and confirmation, and Trigger Placement creating and submitting one account per
 * line with the EB products of the seed catalogue.
 */
@IntegrationTest
class EbMarketingIT {

  private static final String TL = "ebtl";
  private static final String MANAGEMENT = "ebmgmt";
  private static final String MGIC = "INS-MGIC";
  private static final String LAC = "INS-LAC";

  @Autowired private EbFixtures fx;
  @Autowired private CycleService cycles;
  @Autowired private BorService bor;
  @Autowired private FranchiseService franchises;
  @Autowired private TorService tors;
  @Autowired private InsurerRequestService requests;
  @Autowired private EbProposalService proposals;
  @Autowired private RevisionService revisions;
  @Autowired private EbComparativeService comparatives;
  @Autowired private ComparativeApproval approval;
  @Autowired private ComparativePresenter presenter;
  @Autowired private ComparativeExport export;
  @Autowired private ConfirmationService confirmations;
  @Autowired private PlacementTrigger trigger;
  @Autowired private EbCycleRepository cycleRepository;
  @Autowired private AccountRepository accounts;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;
  @Autowired private Clock clock;

  private static EbProgrammeLine.Data line(String benefitLine) {
    return new EbProgrammeLine.Data(benefitLine, null, null, null, null, null, null, 80);
  }

  private EbProgramme programme() {
    return fx.programme(false, List.of(line("HMO"), line("GLI")));
  }

  private EbCycleStage stage(EbCycle cycle) {
    return cycleRepository.findById(cycle.getId()).orElseThrow().getStage();
  }

  /** A new-business cycle in FRANCHISE with a validated BOR. */
  private EbCycle toFranchise(EbProgramme p) {
    EbCycle cycle = fx.cycle(p, BusinessType.NEW_BUSINESS, 2027);
    as.run(AO, () -> cycles.start(fx.company(), cycle.getId()));
    EbBor version = as.run(AO, () -> bor.upload(fx.company(), cycle.getId(), realPdf("bor.pdf")));
    LocalDate today = BusinessClock.today(clock);
    as.run(
        TL,
        () ->
            bor.validate(
                fx.company(),
                version.getId(),
                new EbBor.Checklist(true, true, true, today.minusDays(1), today.plusYears(1))));
    as.run(AO, () -> cycles.remarket(fx.company(), cycle.getId()));
    return cycle;
  }

  private EbFranchiseRequest approve(EbFranchiseRequest request) {
    return as.run(
        AO,
        () ->
            franchises.decide(
                fx.company(),
                request.getId(),
                new FranchiseService.DecisionInput(
                    true, null, null, "Approved by e-mail", realPdf("reply.pdf"))));
  }

  private EbTor releaseTor(EbCycle cycle) {
    as.run(
        AO,
        () ->
            tors.saveItems(
                fx.company(),
                cycle.getId(),
                List.of(
                    new EbTorItem.Data(
                        "HMO", "PLAN-A", "Annual benefit limit", "PHP 150,000 per illness"),
                    new EbTorItem.Data("GLI", null, "Life cover", "36 times the monthly salary"))));
    return as.run(AO, () -> tors.release(fx.company(), cycle.getId()));
  }

  private EbProposal propose(
      EbCycle cycle, String insurer, BigDecimal hmoPremium, List<EbProposalItem.Data> items) {
    ProposalInput input =
        new ProposalInput(
            insurer,
            null,
            LocalDate.of(2026, 12, 31),
            null,
            "Standard HMO terms",
            null,
            List.of(
                new EbProposalLine.Data("HMO", "PLAN-A", "Plan A", 80, null, hmoPremium, null),
                new EbProposalLine.Data(
                    "GLI",
                    "LIFE",
                    "Basic life",
                    80,
                    null,
                    new BigDecimal("250000"),
                    new BigDecimal("80000000"))),
            items,
            List.of(new EbProposalFactor.Data("HOSPITAL_NETWORK", "1,200 hospitals", 4)),
            realPdf("proposal-" + insurer + ".pdf"));
    EbProposal proposal = as.run(AO, () -> proposals.record(fx.company(), cycle.getId(), input));
    return as.run(AO, () -> proposals.validate(fx.company(), proposal.getId()));
  }

  private List<EbProposalItem.Data> answers(EbTor tor) {
    return tor.getItems().stream()
        .map(i -> new EbProposalItem.Data(i.getId(), "As required", false, null))
        .toList();
  }

  /** A cycle in PROPOSALS with one validated proposal of MGIC; returns the proposal. */
  private EbProposal toProposals(EbCycle cycle, BigDecimal hmoPremium) {
    List<EbFranchiseRequest> sent =
        as.run(AO, () -> franchises.request(fx.company(), cycle.getId(), List.of(MGIC)));
    approve(sent.get(0));
    EbTor tor = releaseTor(cycle);
    as.run(AO, () -> requests.send(fx.company(), cycle.getId(), List.of(MGIC)));
    List<EbProposalItem.Data> items =
        tx.execute(s -> answers(tors.ofCycle(fx.company(), cycle.getId()).get(0)));
    assertThat(tor.getVersionNo()).isEqualTo(1);
    return propose(cycle, MGIC, hmoPremium, items);
  }

  private EbComparative signedOff(EbCycle cycle) {
    EbComparative built = as.run(AO, () -> comparatives.build(fx.company(), cycle.getId()));
    as.run(AO, () -> comparatives.submit(fx.company(), built.getId()));
    return as.run(TL, () -> approval.signOff(fx.company(), built.getId(), "Looks right"));
  }

  @Test
  void franchiseRequestsAreDecidedWithEvidenceAndOnlyApprovedInsurersGetTheTor() {
    EbProgramme p = programme();
    EbCycle cycle = toFranchise(p);
    List<EbFranchiseRequest> sent =
        as.run(AO, () -> franchises.request(fx.company(), cycle.getId(), List.of(MGIC, LAC)));
    assertThat(sent).hasSize(2).allMatch(r -> r.getStatus() == EbFranchiseRequest.Status.SUBMITTED);
    assertThat(sent.get(0).getFranchiseNo()).startsWith("EBF-");
    assertThat(sent.get(0).getDueDate()).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = 'EB_FRANCHISE' and entity_id = ?",
                Integer.class,
                sent.get(0).getId().toString()))
        .isEqualTo(1);
    assertThatThrownBy(
            () -> as.run(AO, () -> franchises.request(fx.company(), cycle.getId(), List.of(MGIC))))
        .extracting("code")
        .isEqualTo("EB_FRANCHISE_EXISTS");

    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        franchises.decide(
                            fx.company(),
                            sent.get(1).getId(),
                            new FranchiseService.DecisionInput(
                                false, null, null, null, realPdf("r.pdf")))))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        franchises.decide(
                            fx.company(),
                            sent.get(0).getId(),
                            new FranchiseService.DecisionInput(true, null, null, null, null))))
        .extracting("code")
        .isEqualTo("EB_FRANCHISE_EVIDENCE_REQUIRED");
    EbFranchiseRequest approved = approve(sent.get(0));
    assertThat(approved.getStatus()).isEqualTo(EbFranchiseRequest.Status.APPROVED);
    assertThat(approved.getAdviceDueDate()).isNotNull();
    EbFranchiseRequest rejected =
        as.run(
            AO,
            () ->
                franchises.decide(
                    fx.company(),
                    sent.get(1).getId(),
                    new FranchiseService.DecisionInput(
                        false,
                        null,
                        "EXISTING_BROKER",
                        "Held by another broker",
                        realPdf("no.pdf"))));
    assertThat(rejected.getStatus()).isEqualTo(EbFranchiseRequest.Status.REJECTED);
    EbFranchiseRequest advised =
        as.run(AO, () -> franchises.advise(fx.company(), rejected.getId()));
    assertThat(advised.getStatus()).isEqualTo(EbFranchiseRequest.Status.ADVISED);
    assertThat(advised.isApproved()).isFalse();

    assertThatThrownBy(
            () -> as.run(AO, () -> requests.send(fx.company(), cycle.getId(), List.of(MGIC))))
        .extracting("code")
        .isEqualTo("EB_TOR_NOT_RELEASED");
    assertThatThrownBy(() -> as.run(AO, () -> tors.release(fx.company(), cycle.getId())))
        .extracting("code")
        .isEqualTo("EB_TOR_EMPTY");
    releaseTor(cycle);
    assertThatThrownBy(
            () -> as.run(AO, () -> requests.send(fx.company(), cycle.getId(), List.of(LAC))))
        .extracting("code")
        .isEqualTo("EB_FRANCHISE_NOT_APPROVED");
    List<EbInsurerRequest> rfps =
        as.run(AO, () -> requests.send(fx.company(), cycle.getId(), List.of(MGIC)));
    assertThat(rfps).hasSize(1);
    assertThat(rfps.get(0).getRequestNo()).startsWith("EBR-");
    assertThat(rfps.get(0).getTorVersion()).isEqualTo(1);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.PROPOSALS);

    as.run(
        AO,
        () ->
            tors.saveItems(
                fx.company(),
                cycle.getId(),
                List.of(new EbTorItem.Data("HMO", null, "Room and board", "Semi-private room"))));
    as.run(AO, () -> tors.release(fx.company(), cycle.getId()));
    List<EbInsurerRequest> open = as.run(AO, () -> requests.ofCycle(fx.company(), cycle.getId()));
    assertThat(open.get(0).getTorVersion()).isEqualTo(2);
    List<EbTor> versions = as.run(AO, () -> tors.ofCycle(fx.company(), cycle.getId()));
    assertThat(versions)
        .extracting(EbTor::getStatus)
        .containsExactly(EbTor.Status.RELEASED, EbTor.Status.SUPERSEDED);
  }

  @Test
  void theComparativeIsSignedOffByAnotherUserPresentedConfirmedAndPlaced() {
    EbProgramme p = programme();
    EbCycle cycle = toFranchise(p);
    assertThatThrownBy(() -> as.run(AO, () -> comparatives.build(fx.company(), cycle.getId())))
        .extracting("code")
        .isEqualTo("EB_CYCLE_STAGE");
    EbProposal proposal = toProposals(cycle, new BigDecimal("1200000"));
    assertThat(proposal.getKind()).isEqualTo(EbProposal.Kind.PROPOSAL);
    assertThat(proposal.getProposalNo()).startsWith("EBPR-");

    EbComparative built = as.run(AO, () -> comparatives.build(fx.company(), cycle.getId()));
    assertThat(built.getComparativeNo()).startsWith("EBCA-");
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.COMPARATIVE);
    assertThat(
            as.run(
                AO,
                () ->
                    comparatives.matrix(comparatives.require(fx.company(), built.getId())).lines()))
        .extracting(l -> l.lowestProposalId())
        .containsOnly(proposal.getId());
    as.run(
        AO,
        () ->
            comparatives.recommend(
                fx.company(),
                built.getId(),
                Map.of("HMO", proposal.getId()),
                "MGIC offers the widest network"));
    as.run(AO, () -> comparatives.submit(fx.company(), built.getId()));
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.FOR_SIGNOFF);
    assertThatThrownBy(() -> as.run(AO, () -> approval.signOff(fx.company(), built.getId(), null)))
        .extracting("code")
        .isEqualTo("EB_SIGNOFF_MAKER");
    EbComparative signed =
        as.run(TL, () -> approval.signOff(fx.company(), built.getId(), "Agreed"));
    assertThat(signed.getStatus()).isEqualTo(EbComparative.Status.APPROVED);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.READY_TO_PRESENT);
    assertThat(as.run(AO, () -> export.xlsx(fx.company(), built.getId()))).isNotEmpty();

    as.run(AO, () -> presenter.present(fx.company(), built.getId()));
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.WITH_CLIENT);
    as.run(
        AO,
        () ->
            comparatives.comment(
                fx.company(), built.getId(), true, "Can the HMO limit be raised?", null));

    assertThatThrownBy(() -> confirm(cycle, proposal, null))
        .extracting("code")
        .isEqualTo("EB_CONFIRMATION_EVIDENCE");
    confirm(cycle, proposal, realPdf("confirmation.pdf"));
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.CONFIRMED);

    List<Account> placed = as.run(AO, () -> trigger.trigger(fx.company(), cycle.getId()));
    assertThat(placed).hasSize(2);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.IN_PLACEMENT);
    for (Account a : placed) {
      Account saved = accounts.findByArn(a.getArn()).orElseThrow();
      assertThat(saved.getStatus()).isEqualTo(AccountStatus.SUBMITTED);
      assertThat(saved.getBusinessType()).isEqualTo(BusinessType.NEW_BUSINESS);
      assertThat(saved.getInsurerCode()).isEqualTo(MGIC);
      assertThat(saved.getProductCode()).isIn("EBHMO01", "EBGLI01");
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from doc_attachment_link where entity_type = 'Account' and entity_id = ?",
                  Integer.class,
                  saved.getId().toString()))
          .isPositive();
    }
    Account hmo =
        placed.stream().filter(a -> "EBHMO01".equals(a.getProductCode())).findFirst().orElseThrow();
    assertThat(accounts.findByArn(hmo.getArn()).orElseThrow().getPremium().grossPremium())
        .isGreaterThanOrEqualTo(new BigDecimal("1200000"));
    Boolean linked =
        tx.execute(
            st ->
                confirmations
                    .active(cycleRepository.findById(cycle.getId()).orElseThrow())
                    .orElseThrow()
                    .getLines()
                    .stream()
                    .allMatch(l -> l.getAccountArn() != null));
    assertThat(linked).isTrue();
  }

  private void confirm(EbCycle cycle, EbProposal proposal, UploadedFile evidence) {
    as.run(
        AO,
        () ->
            confirmations.confirm(
                fx.company(),
                cycle.getId(),
                new ConfirmationInput(
                    "EMAIL",
                    null,
                    "Confirmed by the HR head",
                    List.of(
                        new ConfirmationInput.Choice(1, proposal.getId()),
                        new ConfirmationInput.Choice(2, proposal.getId())),
                    evidence)));
  }

  @Test
  void aPremiumAboveTheThresholdWaitsForManagementWhichIsNeverTheAccountOfficer() {
    EbProgramme p = programme();
    EbCycle cycle = toFranchise(p);
    EbProposal proposal = toProposals(cycle, new BigDecimal("25000000"));
    EbComparative signed = signedOff(cycle);
    assertThat(signed.getStatus()).isEqualTo(EbComparative.Status.THRESHOLD_APPROVAL);
    assertThat(signed.getThresholdRules()).contains("HMO annual premium");
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.THRESHOLD_APPROVAL);
    assertThatThrownBy(() -> as.run(AO, () -> trigger.trigger(fx.company(), cycle.getId())))
        .extracting("code")
        .isEqualTo("EB_THRESHOLD_PENDING");
    assertThatThrownBy(
            () -> as.run(TL, () -> approval.approveThreshold(fx.company(), signed.getId(), null)))
        .extracting("code")
        .isEqualTo("WORKFLOW_ACTION_NOT_PERMITTED");
    EbComparative approved =
        as.run(MANAGEMENT, () -> approval.approveThreshold(fx.company(), signed.getId(), "OK"));
    assertThat(approved.getStatus()).isEqualTo(EbComparative.Status.APPROVED);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.READY_TO_PRESENT);
    assertThat(approval.decisions(approved)).hasSize(2);
    assertThat(proposal.getStatus()).isEqualTo(EbProposal.Status.VALIDATED);
  }

  @Test
  void theSignatoryReturnsAComparativeWithAReason() {
    EbCycle cycle = toFranchise(programme());
    toProposals(cycle, new BigDecimal("900000"));
    EbComparative built = as.run(AO, () -> comparatives.build(fx.company(), cycle.getId()));
    as.run(AO, () -> comparatives.submit(fx.company(), built.getId()));
    assertThatThrownBy(
            () -> as.run(TL, () -> approval.returnToAo(fx.company(), built.getId(), " ")))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
    EbComparative returned =
        as.run(TL, () -> approval.returnToAo(fx.company(), built.getId(), "Add the GPA option"));
    assertThat(returned.getStatus()).isEqualTo(EbComparative.Status.DRAFT);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.COMPARATIVE);
  }

  @Test
  void theClientsChangesAreRelayedAndEveryRequestedChangeMustBeAnswered() {
    EbCycle cycle = toFranchise(programme());
    EbProposal first = toProposals(cycle, new BigDecimal("1000000"));
    EbComparative signed = signedOff(cycle);
    as.run(AO, () -> presenter.present(fx.company(), signed.getId()));
    Long hmoItem =
        tx.execute(s -> tors.ofCycle(fx.company(), cycle.getId()).get(0).getItems().get(0).getId());
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        revisions.request(
                            fx.company(),
                            cycle.getId(),
                            new RevisionService.RevisionInput(null, List.of(), List.of(MGIC)))))
        .extracting("code")
        .isEqualTo("EB_REVISION_EMPTY");
    EbRevisionRequest revision =
        as.run(
            AO,
            () ->
                revisions.request(
                    fx.company(),
                    cycle.getId(),
                    new RevisionService.RevisionInput(
                        "Client asks a higher limit",
                        List.of(new RevisionService.Change(hmoItem, "PHP 200,000 per illness")),
                        List.of(MGIC))));
    assertThat(revision.getTargets()).hasSize(1);
    assertThat(stage(cycle)).isEqualTo(EbCycleStage.REVISION);

    assertThatThrownBy(() -> propose(cycle, MGIC, new BigDecimal("1100000"), List.of()))
        .extracting("code")
        .isEqualTo("EB_REVISION_ITEM_UNANSWERED");
    EbProposal revised =
        propose(
            cycle,
            MGIC,
            new BigDecimal("1100000"),
            List.of(new EbProposalItem.Data(hmoItem, "PHP 200,000 per illness", false, null)));
    assertThat(revised.getKind()).isEqualTo(EbProposal.Kind.REVISED);
    assertThat(revised.getVersionNo()).isEqualTo(2);
    assertThat(as.run(AO, () -> proposals.require(fx.company(), first.getId()).getStatus()))
        .isEqualTo(EbProposal.Status.SUPERSEDED);
    assertThat(as.run(AO, () -> revisions.ofCycle(fx.company(), cycle.getId()).get(0).getStatus()))
        .isEqualTo(EbRevisionRequest.Status.ANSWERED);
    EbComparative rebuilt = as.run(AO, () -> comparatives.build(fx.company(), cycle.getId()));
    assertThat(rebuilt.getVersionNo()).isEqualTo(2);
    assertThat(as.run(AO, () -> comparatives.require(fx.company(), signed.getId()).getStatus()))
        .isEqualTo(EbComparative.Status.SUPERSEDED);
  }

  @Test
  void aProposalNeedsTheInsurersDocumentAndAnOpenRequestAndCanBeRejected() {
    EbCycle cycle = toFranchise(programme());
    as.run(AO, () -> franchises.request(fx.company(), cycle.getId(), List.of(MGIC)))
        .forEach(this::approve);
    releaseTor(cycle);
    as.run(AO, () -> requests.send(fx.company(), cycle.getId(), List.of(MGIC)));
    ProposalInput noDocument =
        new ProposalInput(
            MGIC,
            null,
            null,
            null,
            null,
            null,
            List.of(new EbProposalLine.Data("HMO", "A", null, null, null, BigDecimal.TEN, null)),
            List.of(),
            List.of(),
            null);
    assertThatThrownBy(
            () -> as.run(AO, () -> proposals.record(fx.company(), cycle.getId(), noDocument)))
        .extracting("code")
        .isEqualTo("EB_PROPOSAL_DOCUMENT_REQUIRED");
    ProposalInput fromLac = noDocument.withDocument(realPdf("lac.pdf"));
    ProposalInput other =
        new ProposalInput(
            LAC,
            null,
            null,
            null,
            null,
            null,
            fromLac.lines(),
            List.of(),
            List.of(),
            fromLac.document());
    assertThatThrownBy(() -> as.run(AO, () -> proposals.record(fx.company(), cycle.getId(), other)))
        .extracting("code")
        .isEqualTo("EB_PROPOSAL_NOT_REQUESTED");
    EbProposal proposal =
        as.run(
            AO,
            () ->
                proposals.record(
                    fx.company(), cycle.getId(), noDocument.withDocument(realPdf("m.pdf"))));
    EbProposal rejected =
        as.run(
            AO, () -> proposals.reject(fx.company(), proposal.getId(), "Premium per plan missing"));
    assertThat(rejected.getStatus()).isEqualTo(EbProposal.Status.REJECTED);
    assertThatThrownBy(() -> as.run(AO, () -> comparatives.build(fx.company(), cycle.getId())))
        .extracting("code")
        .isEqualTo("EB_COMPARATIVE_NO_PROPOSAL");
    EbInsurerRequest request =
        as.run(AO, () -> requests.ofCycle(fx.company(), cycle.getId()).get(0));
    assertThat(request.getStatus()).isEqualTo(EbInsurerRequest.Status.RESPONDED);
  }
}
