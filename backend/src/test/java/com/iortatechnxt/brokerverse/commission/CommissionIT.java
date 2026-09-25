package com.iortatechnxt.brokerverse.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.Certificate;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.OrLink;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Beneficiary;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.PeriodType;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.RunStatus;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.SchemeType;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRun;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme.Terms;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveTier;
import com.iortatechnxt.brokerverse.commission.service.CertificateService;
import com.iortatechnxt.brokerverse.commission.service.CommissionWorkCounts;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpCollectionService;
import com.iortatechnxt.brokerverse.commission.service.DpCollectionService.CollectRequest;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService.Answer;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackSlaJob;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.commission.service.DpItemFilter;
import com.iortatechnxt.brokerverse.commission.service.DpListService;
import com.iortatechnxt.brokerverse.commission.service.DpResponseHandler;
import com.iortatechnxt.brokerverse.commission.service.EstimatedItemService;
import com.iortatechnxt.brokerverse.commission.service.IncentivePosting;
import com.iortatechnxt.brokerverse.commission.service.IncentiveRunService;
import com.iortatechnxt.brokerverse.commission.service.IncentiveSchemeService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Direct payment commission end to end (CMRID.001-013, MKTID.012) and BIR certificates (CMRID.015):
 * list intake with validation and sanitation, confirmation, billing, feedback SLA, insurer answers,
 * collection with the OR hand-off, PR reversal and reinstatement.
 */
@IntegrationTest
class CommissionIT {

  private static final String HANDLER = CommissionFixtures.HANDLER;
  private static final String BANK = "1111";
  private static final String LEAD = "commtl";

  @Autowired private CommissionFixtures fx;
  @Autowired private DpListService lists;
  @Autowired private DpIntakeService intake;
  @Autowired private DpBillingService billings;
  @Autowired private DpFeedbackService feedback;
  @Autowired private DpCollectionService collection;
  @Autowired private DpFeedbackSlaJob slaJob;
  @Autowired private CertificateService certificates;
  @Autowired private EstimatedItemService estimated;
  @Autowired private CommissionWorkCounts counts;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private FlowInService flowIn;
  @Autowired private SystemParameterService parameters;
  @Autowired private IncentiveSchemeService schemes;
  @Autowired private IncentiveRunService runs;
  @Autowired private IncentivePosting posting;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  @Test
  void dpListsAreValidatedAndSanitised() {
    OpsInvoice dp = fx.directPayment();
    OpsInvoice regular = fx.regular();
    String unknown = "NO-SUCH-" + BookingFixtures.token();
    DpList list =
        fx.upload(
            List.of(
                CommissionFixtures.row(dp),
                CommissionFixtures.row(regular),
                unknown + ",,INS-MGIC,100,"));
    assertThat(list.getItemCount()).isEqualTo(3);
    assertThat(list.getValidCount()).isEqualTo(1);
    DpItem valid = fx.itemOf(list, dp);
    assertThat(valid.getTag()).isEqualTo(DpTag.DP_FOR_CONFIRMATION);
    assertThat(valid.getSanitation()).isEqualTo(Sanitation.VALID);
    assertThat(valid.getCommission()).isEqualByComparingTo(dp.getCommission());
    assertThat(valid.getNetCommission())
        .isEqualByComparingTo(
            dp.getCommission()
                .add(dp.getVatOnCommission())
                .subtract(dp.component(LedgerComponent.WTAX).getBooked()));
    assertThat(valid.getRuleResults()).contains("DIRECT_PAYMENT|PASS").doesNotContain("|FAIL|");
    DpItem notDp = fx.itemOf(list, regular);
    assertThat(notDp.getSanitation()).isEqualTo(Sanitation.INVALID);
    assertThat(notDp.getRuleResults()).contains("DIRECT_PAYMENT|FAIL|");
    assertThat(fx.itemsOf(list))
        .anySatisfy(
            i -> {
              assertThat(i.getInvoiceNo()).isEqualTo(unknown);
              assertThat(i.getRuleResults()).startsWith("INVOICE_BOOKED|FAIL|");
            });

    DpList again = fx.upload(List.of(CommissionFixtures.row(dp)));
    DpItem duplicate = fx.itemsOf(again).get(0);
    assertThat(duplicate.getSanitation()).isEqualTo(Sanitation.DUPLICATE);
    assertThat(duplicate.getDuplicateOf()).isEqualTo(valid.getId());

    byte[] same = CommissionFixtures.file(List.of(CommissionFixtures.row(regular)));
    as.run(HANDLER, () -> lists.upload(fx.company(), "CEBU_DP_20260930.csv", same));
    assertThatThrownBy(
            () -> as.run(HANDLER, () -> lists.upload(fx.company(), "CEBU_DP_20260930.csv", same)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already taken in");
    assertThatThrownBy(() -> as.run(HANDLER, () -> lists.upload(fx.company(), "list.csv", same)))
        .isInstanceOf(BusinessRuleException.class);

    DpItem revalidated = as.run(HANDLER, () -> intake.revalidate(notDp.getId()));
    assertThat(revalidated.getSanitation()).isEqualTo(Sanitation.INVALID);
    List<DpItem> excluded = as.run(HANDLER, () -> intake.exclude(List.of(valid.getId()), "Test"));
    assertThat(excluded.get(0).getTag()).isEqualTo(DpTag.EXCLUDED);
    assertThat(
            as.run(
                    HANDLER,
                    () ->
                        intake.items(
                            fx.company(),
                            new DpItemFilter(
                                List.of(DpTag.EXCLUDED),
                                Sanitation.VALID,
                                dp.getInsurerCode(),
                                list.getId(),
                                null,
                                dp.getInvoiceNo()),
                            PageRequest.of(0, 10)))
                .getContent())
        .hasSize(1);
    assertThat(
            as.run(
                HANDLER,
                () ->
                    lists.tracker(
                        fx.company(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))))
        .anySatisfy(s -> assertThat(s.received()).isTrue());
    assertThat(as.run(HANDLER, () -> lists.pull(fx.company()))).isEmpty();
  }

  @Test
  void aBillingIsAnsweredCollectedAndThePremiumReceivableReversed() {
    OpsInvoice dp = fx.directPayment();
    DpBilling billing = fx.billed(List.of(dp));
    assertThat(billing.getStage()).isEqualTo(DpBilling.AWAITING);
    assertThat(billing.getSlaDue()).isAfter(LocalDate.now().plusDays(9));
    DpItem item = billings.itemsOf(billing.getId()).get(0);
    assertThat(item.getTag()).isEqualTo(DpTag.BILLED);

    assertThat(slaJob.execute(billing.getSlaDue().plusDays(1)).itemsProcessed()).isPositive();
    assertThat(billings.require(billing.getId()).isOverdueAlerted()).isTrue();
    assertThat(counts.counts(fx.company())).extracting(c -> c.key()).contains("DP_OVERDUE");

    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        collection.collect(billing.getId(), new CollectRequest(null, BANK, null))))
        .isInstanceOf(BusinessRuleException.class);
    DpBilling approved =
        as.run(
            HANDLER,
            () ->
                feedback.answer(
                    billing.getId(), List.of(new Answer(dp.getInvoiceNo(), true, null, "OK"))));
    assertThat(approved.getStage()).isEqualTo(DpBilling.APPROVED);

    DpBilling closed =
        as.run(
            HANDLER,
            () ->
                collection.collect(
                    billing.getId(), new CollectRequest(null, BANK, "CERT-" + dp.getInvoiceNo())));
    assertThat(closed.getStage()).isEqualTo("CLOSED");
    assertThat(closed.getOrStatus()).isEqualTo("ISSUED");
    DpItem reversed = fx.reload(item);
    assertThat(reversed.getTag()).isEqualTo(DpTag.PR_REVERSED);
    assertThat(reversed.getCollectedAmount()).isEqualByComparingTo(item.getNetCommission());
    OpsInvoice after = ledger.require(dp.getInvoiceNo());
    assertThat(after.premiumBalance()).isEqualByComparingTo("0");
    assertThat(after.component(LedgerComponent.DTIP).getBalance()).isEqualByComparingTo("0");
    assertThat(after.component(LedgerComponent.COMMISSION).getBalance()).isEqualByComparingTo("0");
    List<OpsInvoiceMovement> moves = ledger.movements(dp.getInvoiceNo());
    assertThat(moves)
        .anySatisfy(m -> assertThat(m.getMovementType()).isEqualTo(MovementType.DP_REVERSAL))
        .anySatisfy(
            m -> {
              assertThat(m.getMovementType()).isEqualTo(MovementType.APPLIED);
              assertThat(m.getBatchNo()).isNotNull();
            });
    assertThat(counts.itemsFor(dp.getInvoiceNo()))
        .anySatisfy(r -> assertThat(r.status()).isEqualTo(DpTag.PR_REVERSED.name()));

    DpItem reinstated =
        as.run(
            HANDLER,
            () -> collection.reinstate(item.getId(), "DP_CANCELLATION", "Policy cancelled"));
    assertThat(reinstated.getTag()).isEqualTo(DpTag.COLLECTED);
    assertThat(reinstated.getReturnedRef()).isNotBlank();
    assertThat(ledger.require(dp.getInvoiceNo()).premiumBalance())
        .isEqualByComparingTo(dp.getGrossPremium());
    assertThatThrownBy(
            () -> as.run(HANDLER, () -> collection.reinstate(item.getId(), "PRM_OTHERS", null)))
        .isInstanceOf(BusinessRuleException.class);
    DpItem again = as.run(HANDLER, () -> collection.reverse(item.getId()));
    assertThat(again.getTag()).isEqualTo(DpTag.PR_REVERSED);
    assertThat(again.getReversalCount()).isEqualTo(2);
    assertThat(ledger.require(dp.getInvoiceNo()).premiumBalance()).isEqualByComparingTo("0");
  }

  @Test
  void theReversalIsPostedToTheLedgerWhenTheParameterIsOn() {
    OpsInvoice dp = fx.directPayment();
    DpBilling billing = fx.billed(List.of(dp));
    as.run(
        HANDLER,
        () ->
            feedback.answer(
                billing.getId(), List.of(new Answer(dp.getInvoiceNo(), true, null, null))));
    as.run("admin", () -> parameters.update("DP_PR_REVERSAL_POSTING", "true"));
    try {
      as.run(
          HANDLER, () -> collection.collect(billing.getId(), new CollectRequest(null, BANK, null)));
      DpItem item = billings.itemsOf(billing.getId()).get(0);
      as.run(HANDLER, () -> collection.reinstate(item.getId(), "DP_WRONG_OR_DETAILS", null));
    } finally {
      as.run("admin", () -> parameters.update("DP_PR_REVERSAL_POSTING", "false"));
    }
    assertThat(ledger.movements(dp.getInvoiceNo()))
        .filteredOn(m -> m.getMovementType() == MovementType.DP_REVERSAL)
        .isNotEmpty()
        .allSatisfy(m -> assertThat(m.getJournalBatchNo()).isNotNull());
  }

  @Test
  void rejectedAccountsReturnToCollectionAndBillingsCanBeCancelled() {
    OpsInvoice one = fx.directPayment();
    OpsInvoice two = fx.directPayment();
    DpBilling billing = fx.billed(List.of(one, two));
    String answers =
        "Billing No.,Invoice No.,Decision,Reason,Comment\n"
            + billing.getBillingNo()
            + ","
            + one.getInvoiceNo()
            + ",REJECTED,OTHERS,Not ours\n"
            + billing.getBillingNo()
            + ","
            + two.getInvoiceNo()
            + ",N,OTHERS,Paid to BDOI\n"
            + billing.getBillingNo()
            + ",NOT-ON-IT,REJECTED,OTHERS,\n"
            + "NO-BILLING,"
            + one.getInvoiceNo()
            + ",Y,,\n";
    var run =
        as.run(
            HANDLER,
            () ->
                flowIn.upload(
                    DpResponseHandler.FEED,
                    new FlowInFile(
                        "answers-" + BookingFixtures.token() + ".csv",
                        answers.getBytes(StandardCharsets.UTF_8))));
    assertThat(run.getStatus()).isEqualTo(FlowInEnums.RunStatus.PARTIAL);
    assertThat(billings.require(billing.getId()).getStage()).isEqualTo("RETURNED_TO_COLLECTION");
    assertThat(billings.itemsOf(billing.getId()))
        .allSatisfy(
            i -> {
              assertThat(i.getTag()).isEqualTo(DpTag.REJECTED);
              assertThat(i.getFeedbackReason()).isEqualTo("OTHERS");
              assertThat(i.getReturnedRef()).isNotBlank();
            });

    OpsInvoice three = fx.directPayment();
    DpList list = fx.upload(List.of(CommissionFixtures.row(three)));
    Long id = fx.itemsOf(list).get(0).getId();
    as.run(HANDLER, () -> intake.confirm(List.of(id)));
    DpBilling draft = as.run(HANDLER, () -> billings.prepare(fx.company(), List.of(id))).get(0);
    assertThatThrownBy(() -> as.run(HANDLER, () -> billings.prepare(fx.company(), List.of(id))))
        .isInstanceOf(BusinessRuleException.class);
    DpBilling cancelled = as.run(HANDLER, () -> billings.cancel(draft.getId(), "Wrong insurer"));
    assertThat(cancelled.getStage()).isEqualTo("CANCELLED");
    assertThat(fx.itemsOf(list).get(0).getTag()).isEqualTo(DpTag.DP_FOR_BILLING);
    assertThatThrownBy(() -> as.run(HANDLER, () -> billings.cancel(billing.getId(), "late")))
        .isInstanceOf(BusinessRuleException.class);
    DpBilling rebilled = fx.billed(List.of(fx.directPayment()));
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        feedback.answer(
                            rebilled.getId(),
                            List.of(
                                new Answer(
                                    billings.itemsOf(rebilled.getId()).get(0).getInvoiceNo(),
                                    false,
                                    null,
                                    null)))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("reason");
  }

  @Test
  void certificatesAreSubmittedRejectedResubmittedAndAcknowledged() {
    Certificate cert =
        new Certificate(
            "2307",
            "C-" + BookingFixtures.token(),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 9, 30),
            new BigDecimal("237.91"),
            List.of(new OrLink("OR-" + BookingFixtures.token(), new BigDecimal("2379.13"))));
    CertificateSubmission s =
        as.run(HANDLER, () -> certificates.submit(fx.company(), "ins-mgic", cert));
    assertThat(s.getStage()).isEqualTo(CertificateSubmission.SUBMITTED);
    assertThat(s.getInsurerCode()).isEqualTo("INS-MGIC");
    assertThatThrownBy(() -> as.run(HANDLER, () -> certificates.acknowledge(s.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    CertificateSubmission rejected =
        as.run("comptrol", () -> certificates.reject(s.getId(), "Scan unreadable"));
    assertThat(rejected.getStage()).isEqualTo(CertificateSubmission.REJECTED);
    assertThat(rejected.getRejectReason()).isEqualTo("Scan unreadable");
    CertificateSubmission resubmitted =
        as.run(HANDLER, () -> certificates.resubmit(s.getId(), cert));
    assertThat(resubmitted.getSubmittedCount()).isEqualTo(2);
    CertificateSubmission done =
        as.run("comptrol", () -> certificates.acknowledge(s.getId(), "Filed"));
    assertThat(done.getStage()).isEqualTo("ACKNOWLEDGED");
    assertThat(done.getDecidedBy()).isEqualTo("comptrol");
    assertThatThrownBy(() -> as.run(HANDLER, () -> certificates.resubmit(s.getId(), cert)))
        .isInstanceOf(BusinessRuleException.class);
    Certificate noOr =
        new Certificate("2307", "X", cert.periodFrom(), cert.periodTo(), BigDecimal.ONE, List.of());
    assertThatThrownBy(
            () -> as.run(HANDLER, () -> certificates.submit(fx.company(), "INS-MGIC", noOr)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("official receipt");
    assertThat(as.run(HANDLER, () -> certificates.search(fx.company(), null, PageRequest.of(0, 5))))
        .isNotEmpty();
    assertThat(as.run(HANDLER, () -> certificates.receiptsOf(fx.company(), "INS-MGIC")))
        .isNotNull();
  }

  @Test
  void incentivesAreComputedPostedAndPassedOn() {
    OpsInvoice invoice = fx.regular();
    String insurer = invoice.getInsurerCode();
    LocalDate day = invoice.getBookingDate();
    assertThat(as.run(LEAD, () -> schemes.list(fx.company())))
        .extracting(IncentiveScheme::getCode)
        .contains("NO_TOUCH", "TOP_UP", "MOTOR_MANIA");
    Long empty =
        as.run(LEAD, () -> schemes.list(fx.company())).stream()
            .filter(s -> "NO_TOUCH".equals(s.getCode()))
            .findFirst()
            .orElseThrow()
            .getId();
    assertThatThrownBy(() -> as.run(LEAD, () -> runs.compute(empty, day, day)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no tiers");

    IncentiveScheme tiered =
        as.run(
            LEAD,
            () ->
                schemes.create(
                    fx.company(),
                    "TT" + BookingFixtures.token(),
                    terms(
                        Calculation.TARGET_TIERED,
                        Beneficiary.BDOI,
                        insurer,
                        List.of(
                            new IncentiveTier(BigDecimal.ONE, BigDecimal.ONE, null, null, null),
                            new IncentiveTier(
                                new BigDecimal("999999999999"),
                                BigDecimal.TEN,
                                null,
                                null,
                                null)))));
    IncentiveRun run = as.run(LEAD, () -> runs.compute(tiered.getId(), day, day));
    assertThat(run.getStatus()).isEqualTo(RunStatus.COMPUTED);
    assertThat(run.getEligibleCount()).isPositive();
    assertThat(run.getIncentiveAmount()).isPositive();
    assertThat(run.getPassOnAmount()).isEqualByComparingTo("0");
    assertThat(as.run(LEAD, () -> runs.lines(run.getId(), PageRequest.of(0, 500))).getContent())
        .anySatisfy(l -> assertThat(l.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo()));
    IncentiveRun posted = as.run(LEAD, () -> posting.post(run.getId()));
    assertThat(posted.getStatus()).isEqualTo(RunStatus.POSTED);
    assertThat(posted.getJournalRefs()).isNotBlank();
    assertThatThrownBy(() -> as.run(LEAD, () -> runs.cancel(run.getId())))
        .isInstanceOf(BusinessRuleException.class);

    IncentiveScheme fixed =
        as.run(
            LEAD,
            () ->
                schemes.create(
                    fx.company(),
                    "FP" + BookingFixtures.token(),
                    terms(
                        Calculation.FIXED_PER_POLICY,
                        Beneficiary.BRANCH,
                        insurer,
                        List.of(
                            new IncentiveTier(null, null, null, BigDecimal.ONE, BigDecimal.TEN)))));
    IncentiveRun branch = as.run(LEAD, () -> runs.compute(fixed.getId(), day, day));
    assertThat(branch.getPassOnAmount()).isEqualByComparingTo(branch.getIncentiveAmount());
    IncentiveRun passed = as.run(LEAD, () -> posting.post(branch.getId()));
    assertThat(passed.getJournalRefs().split(", ")).hasSizeGreaterThanOrEqualTo(3);
    IncentiveRun again = as.run(LEAD, () -> runs.compute(fixed.getId(), day, day));
    assertThat(as.run(LEAD, () -> runs.cancel(again.getId())).getStatus())
        .isEqualTo(RunStatus.CANCELLED);
    assertThat(as.run(LEAD, () -> runs.runs(fx.company(), fixed.getId(), PageRequest.of(0, 5))))
        .hasSize(2);

    assertThatThrownBy(() -> as.run(LEAD, () -> runs.compute(fixed.getId(), day, day.minusDays(1))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    LEAD,
                    () ->
                        schemes.update(
                            fixed.getId(),
                            terms(
                                Calculation.TARGET_TIERED,
                                Beneficiary.BDOI,
                                insurer,
                                List.of(
                                    new IncentiveTier(
                                        null, null, null, BigDecimal.ONE, BigDecimal.TEN))))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("production target");
  }

  private static Terms terms(
      Calculation calculation, Beneficiary beneficiary, String insurer, List<IncentiveTier> tiers) {
    return new Terms(
        "Test " + calculation,
        SchemeType.OTHER,
        calculation,
        PeriodType.CUSTOM,
        beneficiary,
        insurer,
        List.of(),
        List.of(),
        null,
        null,
        true,
        "Integration test scheme",
        tiers);
  }

  @Test
  void invoicesAreFlaggedEstimated() {
    OpsInvoice invoice = fx.regular();
    as.run(HANDLER, () -> estimated.flag(invoice.getInvoiceNo(), true, "Premium estimated"));
    assertThat(estimated.estimated(fx.company(), PageRequest.of(0, 200)).getContent())
        .anySatisfy(i -> assertThat(i.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo()));
    as.run(HANDLER, () -> estimated.flag(invoice.getInvoiceNo(), false, "Final premium"));
    assertThat(ledger.require(invoice.getInvoiceNo()).isEstimated()).isFalse();
  }

  @Test
  void everyCommissionReportRunsAndExports() {
    OpsInvoice dp = fx.directPayment();
    fx.billed(List.of(dp));
    Map<String, String> params = new HashMap<>();
    params.put("companyId", fx.company().toString());
    params.put("from", "2026-01-01");
    params.put("to", "2026-12-31");
    List<String> codes =
        List.of(
            "CMR-COMMISSION-RECEIVABLE",
            "CMR-PRODUCTION-YEARLY",
            "CMR-DP-STATUS",
            "CMR-INCENTIVE",
            "CMR-FEEDBACK-SLA",
            "CMR-BIR-CERT");
    as.run(
        LEAD,
        () -> {
          for (String code : codes) {
            assertThat(reports.run(code, params).code()).isEqualTo(code);
            assertThat(reports.export(code, params, ExportFormat.CSV).content()).isNotEmpty();
          }
          Map<String, String> insurer = new HashMap<>(params);
          insurer.put("insurer", dp.getInsurerCode());
          assertThat(reports.run("CMR-DP-STATUS", insurer).rows()).isNotEmpty();
          return null;
        });
  }
}
