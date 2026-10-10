package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerBatchService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.RenewalInsurerResponseService;
import com.iortatechnxt.brokerverse.renewal.lamd.service.LamdReportHandler;
import com.iortatechnxt.brokerverse.renewal.marketing.service.OverrideService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReviewService;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyParties;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyPolicy;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Renewal wave R1-C: the renewal account on reaching processing, Processing Officer assignment,
 * insurer batches and responses with matching, and the LAMD paid-off routing.
 */
@IntegrationTest
class RenewalProcessingIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private RenewalAssignmentService assignments;
  @Autowired private AccountHistoryService history;
  @Autowired private RenewalDispositionService dispositions;
  @Autowired private ReviewService review;
  @Autowired private OverrideService overrides;
  @Autowired private ProcessingService processing;
  @Autowired private InsurerBatchService batches;
  @Autowired private RenewalInsurerResponseService responses;
  @Autowired private ExtractionService extraction;
  @Autowired private InitiationService initiation;
  @Autowired private LamdReportHandler lamd;
  @Autowired private AccountRepository accounts;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;

  @Autowired
  private com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService auditLogs;

  @Autowired
  private com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService queries;

  private RenewalCandidate inProcessing() {
    RenewalCandidate c = fx.unassignedRetail();
    List<String> ref = List.of(c.getRenewalRef());
    as.run(TL, () -> assignments.assign(fx.company(), ref, AO, null));
    as.run(AO, () -> history.open(fx.company(), c.getRenewalRef()));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                c.getRenewalRef(),
                new RenewalDispositionService.Input(
                    RenewalDisposition.FOR_RENEWAL, null, null, null, null)));
    as.run(AO, () -> dispositions.push(fx.company(), ref));
    as.run(
        TL,
        () ->
            overrides.override(
                fx.company(),
                ref,
                new OverrideService.Request(
                    OverrideKind.OUTSTANDING_BALANCE, null, "OTHERS", "Paid at renewal")));
    assertThat(as.run(TL, () -> review.post(fx.company(), ref)).refused()).isEmpty();
    assertThat(as.run(PROC_TL, () -> processing.assign(fx.company(), ref, PO)).refused()).isEmpty();
    return fx.reload(c);
  }

  @Test
  void theAuditLogNamesTheProcessingOfficerByName() {
    RenewalCandidate c = inProcessing();
    var entries =
        as.run(
            PROC_TL,
            () ->
                auditLogs.entries(
                    fx.company(),
                    new com.iortatechnxt.brokerverse.renewal.audit.service.RenewalAuditLogService
                        .Query(null, null, null, c.getRenewalRef(), null)));
    assertThat(entries)
        .anyMatch(e -> e.description() != null && e.description().startsWith("Processing Officer "))
        .noneMatch(e -> ("Processing Officer " + PO).equals(e.description()));
  }

  @Test
  void aPostedRenewalGetsItsRenewalAccountAndGoesToTheInsurer() {
    RenewalCandidate c = inProcessing();
    assertThat(c.getStage()).isEqualTo(RenewalStage.IN_PROCESSING);
    assertThat(c.getAssignedPo()).isEqualTo(PO);
    assertThat(c.getRenewalArn()).isNotNull();
    Account renewal = tx.execute(s -> accounts.findByArn(c.getRenewalArn()).orElseThrow());
    assertThat(renewal.getClassification().businessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(renewal.getClassification().renewalOfRef()).isEqualTo(c.getExpiringArn());

    String insurer = c.getSnapshot().insurerCode();
    InsurerBatch batch =
        as.run(
            PO, () -> batches.create(fx.company(), insurer, c.getExpiryDate(), c.getExpiryDate()));
    assertThat(batch.getLineCount()).isPositive();
    assertThat(as.run(PO, () -> batches.file(fx.company(), batch.getBatchNo())).content())
        .isNotEmpty();
    as.run(PO, () -> batches.send(fx.company(), batch.getBatchNo()));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.WITH_INSURER);

    InsurerResponse r =
        as.run(
            PO,
            () ->
                responses.manual(
                    fx.company(),
                    c.getRenewalRef(),
                    new InsurerResponse.Content(
                        InsurerResponseCode.RENEW_AS_IS,
                        "REN-1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                    false));
    assertThat(r.isLatestValid()).isTrue();
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.RA_READY);
  }

  @Test
  void aResponseForAnotherPolicyNeverProgressesTheRenewal() {
    RenewalCandidate c = inProcessing();
    String insurer = c.getSnapshot().insurerCode();
    InsurerBatch batch =
        as.run(
            PO, () -> batches.create(fx.company(), insurer, c.getExpiryDate(), c.getExpiryDate()));
    as.run(PO, () -> batches.send(fx.company(), batch.getBatchNo()));
    InsurerResponse r =
        as.run(
            PO,
            () ->
                tx.execute(
                    st ->
                        responses.record(
                            fx.reload(c),
                            new InsurerResponse.Content(
                                InsurerResponseCode.RENEW_AS_IS,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                            new RenewalInsurerResponseService.Source(
                                "UPLOAD", "JOB-X", 2, "OTHER-POLICY"),
                            false)));
    assertThat(r.isLatestValid()).isFalse();
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isEqualTo(RenewalStage.WITH_INSURER);
    assertThat(after.getBucket())
        .as(
            () ->
                as.run(PO, () -> queries.latestResults(after)).stream()
                    .map(x -> x.getCheckCode() + "=" + x.getOutcome() + "/" + x.getSeverity())
                    .toList()
                    .toString())
        .isEqualTo(Bucket.EXCEPTION);
  }

  @Test
  void aPaidOffLoanTagsTheRenewalNotForRenewal() {
    String pn = "PN-L" + System.nanoTime() % 1_000_000;
    RenewalCandidate c =
        as.run(
            PROC_TL,
            () ->
                extraction.createLegacy(
                    fx.company(),
                    new LegacyHeader(
                        "QPS-" + pn,
                        "QPS",
                        null,
                        new LegacyPolicy(
                            "FI-" + pn,
                            null,
                            "MTR12",
                            "MOTOR",
                            null,
                            null,
                            LocalDate.of(2027, 3, 1),
                            LocalDate.of(2028, 3, 1),
                            new BigDecimal("800000.00"),
                            new BigDecimal("14000.00"),
                            "PHP",
                            pn),
                        new LegacyParties(
                            "CL-2026-000001",
                            "Seed Client",
                            null,
                            "INS-MGIC",
                            "ao",
                            "T-CBG1",
                            "CBG",
                            "BDO Unibank"),
                        false,
                        null),
                    null,
                    false));
    as.run(TL, () -> initiation.initiate(fx.company(), List.of(c.getRenewalRef())));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.FOR_PROCESSING);
    BulkContext context =
        new BulkContext(
            fx.company(),
            "LAMD-" + pn,
            LocalDate.of(2026, 9, 20),
            Map.of(
                LamdReportHandler.PARAM_TYPE,
                "PAID_OFF",
                LamdReportHandler.PARAM_PERIOD,
                "2026-09"));
    BulkRow row = new BulkRow(2, Map.of("PN Number", pn));
    assertThat(lamd.validate(row, context)).isEmpty();
    as.run("lamd", () -> tx.execute(s -> lamd.commit(row, context)));
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isEqualTo(RenewalStage.LETTER_PENDING);
    assertThat(after.getDisposition().reasonCode()).isEqualTo("LOAN_FULLY_PAID");
  }
}
