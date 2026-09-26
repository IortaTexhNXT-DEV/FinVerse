package com.iortatechnxt.brokerverse.opsintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.commission.CommissionFixtures;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.service.DpBillingSender;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpCollectionService;
import com.iortatechnxt.brokerverse.commission.service.DpCollectionService.CollectRequest;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService.Answer;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.prodrecon.ReconFixtures;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator.Outcome;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService.ExtractRequest;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconCycleService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconSendService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.IncentiveRuleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The later steps of the Operations journey of {@link OperationsEndToEndIT}: the production
 * reconciliation cycle with the early incentive from remittance's rules, a direct payment
 * commission billed and collected with its OR, and a minimal balance written off.
 */
@Component
public class OpsJourneyLater {

  private static final String RECON = "recon";
  private static final String COMMREC = "commrec";
  private static final String INSURER = "INS-MGIC";

  private final OpsJourney journey;
  private final ProductionExtractService extracts;
  private final ReconSendService sender;
  private final ReconUploadService uploads;
  private final ReconCycleService cycles;
  private final ReconItemRepository reconItems;
  private final EarlyIncentiveValidator incentives;
  private final IncentiveRuleService rules;
  private final FlowInService flowIn;
  private final DpItemRepository dpItems;
  private final DpIntakeService intake;
  private final DpBillingService billings;
  private final DpBillingSender billingSender;
  private final DpFeedbackService feedback;
  private final DpCollectionService collection;
  private final BulkService bulk;
  private final ExtractionService extraction;
  private final InvoiceTagRepository tags;
  private final BatchLineRepository lines;

  OpsJourneyLater(
      OpsJourney journey,
      ProductionExtractService extracts,
      ReconSendService sender,
      ReconUploadService uploads,
      ReconCycleService cycles,
      ReconItemRepository reconItems,
      EarlyIncentiveValidator incentives,
      IncentiveRuleService rules,
      FlowInService flowIn,
      DpItemRepository dpItems,
      DpIntakeService intake,
      DpBillingService billings,
      DpBillingSender billingSender,
      DpFeedbackService feedback,
      DpCollectionService collection,
      BulkService bulk,
      ExtractionService extraction,
      InvoiceTagRepository tags,
      BatchLineRepository lines) {
    this.journey = journey;
    this.extracts = extracts;
    this.sender = sender;
    this.uploads = uploads;
    this.cycles = cycles;
    this.reconItems = reconItems;
    this.incentives = incentives;
    this.rules = rules;
    this.flowIn = flowIn;
    this.dpItems = dpItems;
    this.intake = intake;
    this.billings = billings;
    this.billingSender = billingSender;
    this.feedback = feedback;
    this.collection = collection;
    this.bulk = bulk;
    this.extraction = extraction;
    this.tags = tags;
    this.lines = lines;
  }

  /** The latest batch line of an invoice. */
  public BatchLine lineOf(String invoiceNo) {
    return lines.findByInvoiceNo(invoiceNo).get(0);
  }

  /**
   * 8. The register is extracted, sent and answered by the insurer: the remitted invoice matches,
   * another one shows its premium discrepancy, and the early incentive is validated against
   * remittance's rule through the {@code EarlyIncentiveRules} port.
   */
  public void reconciled(String invoiceNo) {
    OpsInvoice other = journey.invoice(journey.bookMotor().getInvoiceNo());
    ReconExtract extract =
        journey.as(
            RECON,
            () ->
                extracts.extract(
                    new ExtractRequest(
                        journey.company(),
                        INSURER,
                        BookingFixtures.BOOKED_ON,
                        BookingFixtures.BOOKED_ON),
                    ExtractTrigger.MANUAL));
    Long cycleId = extract.getCycleId();
    journey.as(RECON, () -> sender.send(extract.getId(), List.of("recon@insurer-seed.ph"), null));
    OpsInvoice remitted = journey.invoice(invoiceNo);
    byte[] file =
        ReconFixtures.file(
            List.of(
                ReconFixtures.line(remitted, BigDecimal.ZERO),
                ReconFixtures.line(other, new BigDecimal("5.00"))));
    ReconUploadService.UploadResult result =
        journey.as(
            RECON,
            () ->
                uploads.upload(
                    journey.company(), "mgic-e2e-" + BookingFixtures.token() + ".csv", file));
    assertThat(result.run().getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    assertThat(item(cycleId, invoiceNo).getStatus()).isEqualTo(ReconStatus.MATCHED);
    assertThat(item(cycleId, other.getInvoiceNo()).getDiscrepancies()).isEqualTo("GROSS_PREMIUM");
    assertThat(cycles.require(cycleId).getStage()).isEqualTo(ReconCycle.RECONCILING);
    validatedEarlyIncentive(cycleId, remitted);
  }

  private com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem item(Long cycle, String no) {
    return reconItems.findByCycleIdAndInvoiceNo(cycle, no).orElseThrow();
  }

  private void validatedEarlyIncentive(Long cycleId, OpsInvoice remitted) {
    EarlyIncentiveRule rule =
        journey.as(
            "remittl",
            () ->
                rules.create(
                    journey.company(),
                    terms("E2E early remittance " + BookingFixtures.token(), true)));
    try {
      assertThat(incentives.validate(journey.company(), cycleId))
          .filteredOn(l -> l.invoiceNo().equals(remitted.getInvoiceNo()))
          .singleElement()
          .satisfies(
              l -> {
                assertThat(l.outcome()).isEqualTo(Outcome.ELIGIBLE);
                assertThat(l.ratePercent()).isEqualByComparingTo("2");
                assertThat(l.remittedOn()).isNotNull();
                assertThat(l.expected())
                    .isEqualByComparingTo(
                        remitted
                            .component(LedgerComponent.BASIC)
                            .getBooked()
                            .multiply(new BigDecimal("0.02"))
                            .setScale(2, RoundingMode.HALF_UP));
              });
    } finally {
      journey.as("remittl", () -> rules.update(rule.getId(), terms("Closed", false)));
    }
  }

  private static EarlyIncentiveRule.Terms terms(String description, boolean active) {
    return new EarlyIncentiveRule.Terms(
        INSURER,
        "MOTOR",
        "CBG",
        new BigDecimal("2"),
        366,
        IncentiveBasis.BOOKING,
        LocalDate.of(2026, 1, 1),
        null,
        active,
        description);
  }

  /**
   * 10. A direct payment account arrives on the Collection DP list, is billed to the insurer,
   * approved and collected: the premium receivable is reversed and the commission OR issued by
   * cashiering through {@code ReceiptIssuer}.
   */
  public void directPaymentCommissionCollected() {
    OpsInvoice dp = journey.invoice(journey.bookDirectPayment().getInvoiceNo());
    assertThat(dp.isDpFlag()).isTrue();
    assertThat(dp.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_APPLICABLE);
    assertThat(journey.openItems(dp.getInvoiceNo()))
        .containsKey(OpenItemRole.INSURER_COMMISSION)
        .doesNotContainKey(OpenItemRole.CLIENT_PREMIUM);

    FlowInRun run =
        journey.as(
            COMMREC,
            () ->
                flowIn.upload(
                    "COLLECTION_DP_LIST",
                    new FlowInFile(
                        "HO_DP_20260930.csv",
                        CommissionFixtures.file(List.of(CommissionFixtures.row(dp))))));
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    DpItem listed = dpItems.findByInvoiceNoOrderByIdDesc(dp.getInvoiceNo()).get(0);
    assertThat(listed.getTag()).isEqualTo(DpTag.DP_FOR_CONFIRMATION);

    journey.as(COMMREC, () -> intake.confirm(List.of(listed.getId())));
    DpBilling prepared =
        journey
            .as(COMMREC, () -> billings.prepare(journey.company(), List.of(listed.getId())))
            .get(0);
    journey.as(
        COMMREC, () -> billingSender.send(prepared.getId(), List.of("billing@insurer.ph"), null));
    journey.as(
        COMMREC,
        () ->
            feedback.answer(
                prepared.getId(), List.of(new Answer(dp.getInvoiceNo(), true, null, "Paid"))));
    DpBilling closed =
        journey.as(
            COMMREC,
            () ->
                collection.collect(
                    prepared.getId(),
                    new CollectRequest(null, "1111", "CERT-" + dp.getInvoiceNo())));
    assertThat(closed.getStage()).isEqualTo("CLOSED");
    assertThat(closed.getOrStatus()).isEqualTo("ISSUED");
    assertThat(closed.getOrNo()).startsWith("OR-HO-");
    DpItem reversed = dpItems.findById(listed.getId()).orElseThrow();
    assertThat(reversed.getTag()).isEqualTo(DpTag.PR_REVERSED);
    assertThat(journey.postedEvents("DPC:" + listed.getId()))
        .containsExactly("OPS_DP_COMMISSION_COLLECT");

    OpsInvoice after = journey.invoice(dp.getInvoiceNo());
    assertThat(after.premiumBalance()).isZero();
    assertThat(after.component(LedgerComponent.DTIP).getBalance()).isZero();
    assertThat(after.component(LedgerComponent.COMMISSION).getBalance()).isZero();
    assertThat(journey.movements(dp.getInvoiceNo(), MovementType.DP_REVERSAL)).isNotEmpty();
    assertThat(after.getRemittanceStatus()).isEqualTo(RemittanceStatus.NOT_APPLICABLE);
  }

  /**
   * 11. A payment leaves PHP 50.00 of basic premium open; the minimal balance file writes it off,
   * and remittance then refuses the written-off invoice.
   */
  public void minimalBalanceWrittenOff() {
    String invoiceNo = journey.bookMotor().getInvoiceNo();
    BigDecimal left = new BigDecimal("50.00");
    journey.pay(invoiceNo, journey.invoice(invoiceNo).premiumBalance().subtract(left));
    assertThat(journey.invoice(invoiceNo).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(journey.balance(invoiceNo, LedgerComponent.BASIC)).isEqualByComparingTo(left);

    String csv = "Invoice No,Balance\n" + invoiceNo + "," + left.toPlainString() + "\n";
    BulkJob job =
        journey.as(
            "adjust",
            () ->
                bulk.upload(
                    new BulkUpload(
                        journey.company(),
                        "MINIMAL_BALANCE_FILE",
                        "minbal-" + BookingFixtures.token() + ".csv",
                        csv.getBytes(StandardCharsets.UTF_8),
                        Map.of())));
    assertThat(job.getValidRows()).isEqualTo(1);
    assertThat(journey.as("adjust", () -> bulk.commit(job.getId())).getCommittedRows())
        .isEqualTo(1);

    OpsInvoice written = journey.invoice(invoiceNo);
    assertThat(written.isWrittenOff()).isTrue();
    assertThat(written.premiumBalance()).isZero();
    assertThat(written.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(journey.moved(invoiceNo, MovementType.WRITE_OFF, LedgerComponent.BASIC))
        .isEqualByComparingTo(left);
    assertThat(journey.postedEvents("WO:" + invoiceNo)).containsExactly("OPS_WRITE_OFF");
    assertThat(journey.balancedJournals(invoiceNo)).isPositive();

    journey.as(
        "remit",
        () ->
            extraction.run(
                journey.company(),
                new Scope(ExtractionTrigger.MANUAL_INVOICE, null, null, invoiceNo),
                LocalDate.now()));
    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(invoiceNo))
        .hasValueSatisfying(
            t -> {
              assertThat(t.getTag()).isEqualTo(ExtractionTag.UNEXTRACTED_DUE);
              assertThat(t.getReasons()).contains("WRITTEN_OFF");
            });
  }
}
