package com.iortatechnxt.brokerverse.eb.seed;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeApproval;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativePresenter;
import com.iortatechnxt.brokerverse.eb.comparative.service.EbComparativeService;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationInput;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationService;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.franchise.service.FranchiseService;
import com.iortatechnxt.brokerverse.eb.market.service.EbProposalService;
import com.iortatechnxt.brokerverse.eb.market.service.InsurerRequestService;
import com.iortatechnxt.brokerverse.eb.market.service.ProposalInput;
import com.iortatechnxt.brokerverse.eb.market.service.TorService;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SEED PROFILE ONLY: the new business of Bayside Builders (cycle EBC-2026-000002, HMO and GPA, in
 * REQUIREMENTS after the SQL seed) taken to the client's confirmation through the Employee Benefits
 * services (conformance gap G03): the broker of record validated, the franchise of MGIC approved,
 * the terms of reference released and the request for proposal sent, MGIC's proposal recorded and
 * validated, the comparative recommended, signed off by the Team Lead and presented, and the HR
 * officer's confirmation recorded on the seed date. The placement report has a row from day one.
 * Idempotent: nothing is done unless the cycle is still in REQUIREMENTS; a step that fails is
 * logged and the rest is skipped.
 */
@Component
@Profile("seed")
@Order(150)
public class EbPlacementSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(EbPlacementSeedData.class);
  private static final String CYCLE = "EBC-2026-000002";
  private static final String AO = "ebao2";
  private static final String TL = "ebtl";
  private static final String MGIC = "INS-MGIC";
  private static final int HMO_LINE = 1;
  private static final int GPA_LINE = 2;
  private static final int MEMBERS = 85;
  private static final int NETWORK_RATING = 4;

  private final EbCycleRepository cycles;
  private final CycleService cycleService;
  private final BorService bor;
  private final FranchiseService franchises;
  private final TorService tors;
  private final InsurerRequestService requests;
  private final EbProposalService proposals;
  private final EbComparativeService comparatives;
  private final ComparativeApproval approval;
  private final ComparativePresenter presenter;
  private final ConfirmationService confirmations;
  private final SeedUsers users;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param cycles cycles
   * @param cycleService cycle actions
   * @param bor broker of record
   * @param franchises franchise requests
   * @param tors terms of reference
   * @param requests requests for proposal
   * @param proposals proposals
   * @param comparatives comparatives
   * @param approval comparative sign-off
   * @param presenter comparative presentation
   * @param confirmations client confirmation
   * @param users seed sign-in
   * @param transactions transaction manager
   * @param clock clock
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public EbPlacementSeedData(
      EbCycleRepository cycles,
      CycleService cycleService,
      BorService bor,
      FranchiseService franchises,
      TorService tors,
      InsurerRequestService requests,
      EbProposalService proposals,
      EbComparativeService comparatives,
      ComparativeApproval approval,
      ComparativePresenter presenter,
      ConfirmationService confirmations,
      SeedUsers users,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.cycles = cycles;
    this.cycleService = cycleService;
    this.bor = bor;
    this.franchises = franchises;
    this.tors = tors;
    this.requests = requests;
    this.proposals = proposals;
    this.comparatives = comparatives;
    this.approval = approval;
    this.presenter = presenter;
    this.confirmations = confirmations;
    this.users = users;
    this.tx = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    cycles
        .findByCycleNo(CYCLE)
        .filter(c -> c.getStage() == EbCycleStage.REQUIREMENTS)
        .ifPresent(
            c -> {
              try {
                confirm(c.getCompanyId(), c.getId());
                LOG.info("Employee Benefits seed: {} confirmed by the client", CYCLE);
              } catch (RuntimeException ex) {
                LOG.warn("Employee Benefits seed of {} stopped: {}", CYCLE, ex.getMessage());
              }
            });
  }

  private void confirm(Long company, Long cycle) {
    LocalDate today = BusinessClock.today(clock);
    EbBor version = users.as(AO, () -> bor.upload(company, cycle, pdf("bor.pdf")));
    users.as(
        TL,
        () ->
            bor.validate(
                company,
                version.getId(),
                new EbBor.Checklist(true, true, true, today.minusDays(1), today.plusYears(1))));
    users.as(AO, () -> cycleService.remarket(company, cycle));
    requestProposal(company, cycle);
    EbProposal proposal = propose(company, cycle, today);
    present(company, cycle, proposal);
    users.as(
        AO,
        () ->
            confirmations.confirm(
                company,
                cycle,
                new ConfirmationInput(
                    "EMAIL",
                    today,
                    "Confirmed by the HR officer",
                    List.of(
                        new ConfirmationInput.Choice(HMO_LINE, proposal.getId()),
                        new ConfirmationInput.Choice(GPA_LINE, proposal.getId())),
                    pdf("client-confirmation.pdf"))));
  }

  /** The franchise of MGIC approved, the terms of reference released and the RFP sent. */
  private void requestProposal(Long company, Long cycle) {
    List<EbFranchiseRequest> sent =
        users.as(AO, () -> franchises.request(company, cycle, List.of(MGIC)));
    users.as(
        AO,
        () ->
            franchises.decide(
                company,
                sent.get(0).getId(),
                new FranchiseService.DecisionInput(
                    true, null, null, "Approved by e-mail", pdf("franchise-reply.pdf"))));
    users.as(
        AO,
        () ->
            tors.saveItems(
                company,
                cycle,
                List.of(
                    new EbTorItem.Data(
                        "HMO", "PLAN-A", "Annual benefit limit", "PHP 150,000 per illness"),
                    new EbTorItem.Data(
                        "GPA", null, "Accidental death", "PHP 500,000 per member"))));
    users.as(AO, () -> tors.release(company, cycle));
    users.as(AO, () -> requests.send(company, cycle, List.of(MGIC)));
  }

  /** The comparative recommended, signed off by the Team Lead and presented to the client. */
  private void present(Long company, Long cycle, EbProposal proposal) {
    EbComparative built = users.as(AO, () -> comparatives.build(company, cycle));
    users.as(
        AO,
        () ->
            comparatives.recommend(
                company,
                built.getId(),
                Map.of("HMO", proposal.getId(), "GPA", proposal.getId()),
                "MGIC offers the widest hospital network at the budget"));
    users.as(AO, () -> comparatives.submit(company, built.getId()));
    users.as(TL, () -> approval.signOff(company, built.getId(), "Agreed"));
    users.as(AO, () -> presenter.present(company, built.getId()));
  }

  private EbProposal propose(Long company, Long cycle, LocalDate today) {
    List<EbProposalItem.Data> answers =
        tx.execute(
            s ->
                tors.ofCycle(company, cycle).get(0).getItems().stream()
                    .map(i -> new EbProposalItem.Data(i.getId(), "As required", false, null))
                    .toList());
    ProposalInput input =
        new ProposalInput(
            MGIC,
            today,
            today.plusMonths(2),
            null,
            "Standard HMO and group accident terms",
            null,
            List.of(
                new EbProposalLine.Data(
                    "HMO", "PLAN-A", "Plan A", MEMBERS, null, new BigDecimal("1020000"), null),
                new EbProposalLine.Data(
                    "GPA",
                    "GPA-500",
                    "Accident cover PHP 500,000",
                    MEMBERS,
                    null,
                    new BigDecimal("63750"),
                    new BigDecimal("42500000"))),
            answers,
            List.of(
                new EbProposalFactor.Data("HOSPITAL_NETWORK", "1,200 hospitals", NETWORK_RATING)),
            pdf("proposal-mgic.pdf"));
    EbProposal recorded = users.as(AO, () -> proposals.record(company, cycle, input));
    return users.as(AO, () -> proposals.validate(company, recorded.getId()));
  }

  /** A one-page PDF standing for a document received by e-mail. */
  private static UploadedFile pdf(String name) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document()) {
      PdfWriter.getInstance(doc, out);
      doc.open();
      doc.add(new Paragraph("Bayside Builders - " + name));
    }
    return new UploadedFile(name, out.toByteArray());
  }
}
