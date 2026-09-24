package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.NegativeAdjustmentPending;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.RelatedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequest;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.service.BatchDocuments;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.IncentiveRuleService;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads.UploadResult;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceApprovalSource;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceExtractionJob;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRelatedItems;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceWorkCounts;
import com.iortatechnxt.brokerverse.remittance.service.ScheduleDispatch;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Insurer OR upload with the exception report, early-remittance incentives, the collection feeds,
 * end-of-day requests, notifications, the Operations SPIs and the Remittance reports
 * (RMTID.005/012/013/016/023/026/034/035/039).
 */
@IntegrationTest
class RemittanceOperationsIT {

  private static final List<String> REPORTS =
      List.of(
          "REM-TRACKER",
          "REM-SPECIAL-REGISTER",
          "REM-SCHEDULE-NORMAL",
          "REM-SCHEDULE-SPECIAL",
          "REM-SCHEDULE-INCENTIVE",
          "REM-DTIP-SUMMARY",
          "REM-DTIP-DETAIL",
          "REM-REMITTED-BATCH",
          "REM-PAIDAR-OVER-DTIP",
          "REM-OR-EXCEPTION",
          "REM-EXCLUDED",
          "REM-HOLD");

  @Autowired private RemittanceFixtures fx;
  @Autowired private InsurerOrUploads uploads;
  @Autowired private BatchService batches;
  @Autowired private BatchDocuments documents;
  @Autowired private ScheduleDispatch dispatch;
  @Autowired private DisbursementQueueService queue;
  @Autowired private IncentiveRuleService incentives;
  @Autowired private ExtractionService extraction;
  @Autowired private RemittanceExtractionJob job;
  @Autowired private RemittanceRelatedItems related;
  @Autowired private RemittanceWorkCounts counts;
  @Autowired private RemittanceApprovalSource approvals;
  @Autowired private com.iortatechnxt.brokerverse.opsledger.service.FlowInService flowIn;
  @Autowired private ReportService reports;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;

  private void assignDv(RemittanceBatch batch) {
    DisbursementRequest request = queue.find("REMITTANCE", batch.getBatchNo()).orElseThrow();
    as.run(
        "disb",
        () -> tx.execute(s -> queue.assignDv(request.getId(), "DV-" + BookingFixtures.token())));
  }

  private UploadResult upload(String csv) {
    return as.run(
        "remit",
        () -> uploads.upload(new FlowInFile("or.csv", csv.getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void theInsurerOrScheduleUpdatesLinesAndReportsExceptions() {
    OpsInvoice first = fx.paidInvoice();
    RemittanceBatch batch = fx.approvedBatch(first);
    BatchLine line = batch.line(first.getInvoiceNo());
    String orNo = "OR-" + BookingFixtures.token();
    UploadResult early =
        upload(
            "batchNo,invoiceNo,orNo,orDate,orAmount\n"
                + batch.getBatchNo()
                + ",NO-SUCH,X,2026-09-20,1\n");
    assertThat(early.run().getFailedCount()).isEqualTo(1);

    assignDv(batch);
    String wrong = line.getAmounts().paidAr().subtract(BigDecimal.ONE).toPlainString();
    UploadResult result =
        upload(
            "batchNo;invoiceNo;orNo;orDate;orAmount\n"
                + batch.getBatchNo()
                + ";"
                + first.getInvoiceNo()
                + ";"
                + orNo
                + ";2026-09-20;"
                + wrong
                + "\n"
                + batch.getBatchNo()
                + ";"
                + first.getInvoiceNo()
                + ";;2026-09-20;10\n"
                + "RMB-NONE;INV;OR-1;2026-09-20;10\n"
                + batch.getBatchNo()
                + ";"
                + first.getInvoiceNo()
                + ";OR-X;20/09/2026;10\n");
    assertThat(result.updated())
        .singleElement()
        .satisfies(l -> assertThat(l.getOrStatus()).isEqualTo(OrStatus.AMOUNT_MISMATCH));
    assertThat(result.records()).hasSize(4);
    assertThat(result.run().getFailedCount()).isEqualTo(3);
    assertThat(fx.batchOf(first.getInvoiceNo()).getStage()).isEqualTo(BatchStage.OR_RECEIVED);
    assertThat(uploads.result(result.run().getId()).updated()).hasSize(1);
    assertThat(uploads.runs(org.springframework.data.domain.PageRequest.of(0, 5))).isNotEmpty();

    OpsInvoice second = fx.paidInvoice();
    RemittanceBatch other = fx.approvedBatch(second);
    assignDv(other);
    UploadResult duplicate =
        upload(
            "batchNo,invoiceNo,orNo,orDate,orAmount\n"
                + other.getBatchNo()
                + ","
                + second.getInvoiceNo()
                + ","
                + orNo
                + ",2026-09-21,"
                + other.getTotals().paidAr().toPlainString()
                + "\n");
    assertThat(duplicate.updated()).isEmpty();
    assertThat(duplicate.records())
        .singleElement()
        .satisfies(r -> assertThat(r.getMessage()).contains("already recorded"));
    UploadResult matched =
        upload(
            "batchNo,invoiceNo,orNo,orDate,orAmount\n"
                + other.getBatchNo()
                + ","
                + second.getInvoiceNo()
                + ",OR2-"
                + BookingFixtures.token()
                + ",2026-09-21,"
                + other.getTotals().paidAr().toPlainString()
                + "\n");
    assertThat(matched.updated())
        .singleElement()
        .satisfies(l -> assertThat(l.getOrStatus()).isEqualTo(OrStatus.MATCHED));
  }

  @Test
  void aWithIncentivesBatchDeductsTheEarlyRemittanceIncentive() {
    as.run(
        "remittl",
        () ->
            incentives.create(
                fx.company(),
                new EarlyIncentiveRule.Terms(
                    "INS-MGIC",
                    "MOTOR",
                    "RETAIL",
                    new BigDecimal("2"),
                    366,
                    IncentiveBasis.BOOKING,
                    LocalDate.of(2026, 1, 1),
                    null,
                    true,
                    "Test early remittance incentive")));
    assertThatThrownBy(
            () ->
                incentives.create(
                    fx.company(),
                    new EarlyIncentiveRule.Terms(
                        "INS-MGIC",
                        null,
                        null,
                        BigDecimal.ONE,
                        1,
                        IncentiveBasis.INCEPTION,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1),
                        true,
                        null)))
        .isInstanceOf(BusinessRuleException.class);
    OpsInvoice invoice = fx.invoice("RETAIL");
    fx.payInFull(invoice, RemittanceFixtures.PAID_ON);
    RemittanceBatch batch = fx.approvedBatch(fx.reload(invoice.getInvoiceNo()));
    assertThat(batch.getRemittanceType()).isEqualTo(RemittanceType.WITH_INCENTIVES);
    BatchLine line = batch.line(invoice.getInvoiceNo());
    assertThat(line.getAmounts().incentive()).isPositive();
    assertThat(line.getAmounts().incentive())
        .isEqualByComparingTo(
            line.getBasicPremium()
                .multiply(new BigDecimal("0.02"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN));
    assertThat(batch.getIncentiveOrStatus()).isEqualTo("DEFERRED");
    DisbursementRequest request = queue.find("REMITTANCE", batch.getBatchNo()).orElseThrow();
    assertThat(request.getAmount())
        .isEqualByComparingTo(
            line.getAmounts().netDue().subtract(line.getAmounts().incentiveTotal()));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_event_log where source_reference = ?",
                Integer.class,
                "RMB:" + batch.getBatchNo() + ":INC"))
        .isEqualTo(1);
    assertThat(documents.render(batch, DocumentKind.SCHEDULE_XLSX).content()).isNotEmpty();
    EarlyIncentiveRule rule =
        incentives.list(fx.company()).stream()
            .filter(r -> "RETAIL".equals(r.getSegment()))
            .findFirst()
            .orElseThrow();
    assertThat(
            incentives
                .update(
                    rule.getId(),
                    new EarlyIncentiveRule.Terms(
                        "INS-MGIC",
                        "MOTOR",
                        "RETAIL",
                        new BigDecimal("2"),
                        366,
                        IncentiveBasis.BOOKING,
                        LocalDate.of(2026, 1, 1),
                        null,
                        false,
                        "Closed"))
                .isActive())
        .isFalse();
  }

  @Test
  void theScheduleAndPaymentRequestAreStoredAndTheScheduleIsSentOnce() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    RemittanceBatch review = fx.batchOf(paid.getInvoiceNo());
    assertThat(documents.document(review, DocumentKind.SCHEDULE_PDF).content()).isNotEmpty();
    assertThatThrownBy(
            () ->
                as.run(
                    "remit",
                    () ->
                        dispatch.send(
                            review.getId(),
                            new ScheduleDispatch.Mail(List.of("x@insurer.ph"), null, "S", "B"))))
        .isInstanceOf(BusinessRuleException.class);
    as.run("remit", () -> batches.submit(review.getId(), null));
    as.run("remittl", () -> batches.approve(review.getId(), null));
    RemittanceBatch approved = fx.batchOf(paid.getInvoiceNo());
    for (DocumentKind kind : DocumentKind.values()) {
      assertThat(documents.document(approved, kind).content()).isNotEmpty();
    }
    var queued =
        as.run(
            "remit",
            () ->
                dispatch.send(
                    approved.getId(),
                    new ScheduleDispatch.Mail(
                        List.of("remit@insurer.ph"), null, "Schedule", "Attached")));
    assertThat(queued.messageId()).isNotNull();
    assertThatThrownBy(
            () ->
                as.run(
                    "remit",
                    () ->
                        dispatch.send(
                            approved.getId(),
                            new ScheduleDispatch.Mail(
                                List.of("remit@insurer.ph"), null, "Again", "Again"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already sent");
  }

  @Test
  void collectionFilesCreateHoldsAndSpecialRemittances() {
    OpsInvoice forHold = fx.paidInvoice();
    OpsInvoice forSpecial = fx.paidInvoice();
    var holdRun =
        as.run(
            "mktcoll",
            () ->
                flowIn.upload(
                    "COLLECTION_HOLD",
                    new FlowInFile(
                        "holds.csv",
                        ("invoiceNo,reasonCode,holdUntil,remarks\n"
                                + forHold.getInvoiceNo()
                                + ",OTHERS,"
                                + LocalDate.now().plusDays(7)
                                + ",From Collection\n"
                                + "NO-SUCH-INVOICE,OTHERS,"
                                + LocalDate.now().plusDays(7)
                                + ",x\n")
                            .getBytes(StandardCharsets.UTF_8))));
    assertThat(holdRun.getOkCount()).isEqualTo(1);
    assertThat(holdRun.getFailedCount()).isEqualTo(1);
    var specialRun =
        as.run(
            "mktcoll",
            () ->
                flowIn.upload(
                    "COLLECTION_SPECIAL_REMIT",
                    new FlowInFile(
                        "special.csv",
                        ("invoiceNo,conditionCode,remarks\n"
                                + forSpecial.getInvoiceNo()
                                + ",RENEWAL,Renewal due\n")
                            .getBytes(StandardCharsets.UTF_8))));
    assertThat(specialRun.getOkCount()).isEqualTo(1);
    var bad =
        as.run(
            "mktcoll",
            () ->
                flowIn.upload(
                    "COLLECTION_HOLD",
                    new FlowInFile("bad.csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8))));
    assertThat(bad.getErrorDetail()).contains("columns");
    List<RelatedItem> items = related.itemsFor(forHold.getInvoiceNo());
    assertThat(items).anySatisfy(i -> assertThat(i.type()).isEqualTo("HOLD"));
    assertThat(related.itemsFor(forSpecial.getInvoiceNo()))
        .anySatisfy(i -> assertThat(i.type()).isEqualTo("SPECIAL_REMITTANCE"));
  }

  @Test
  void endOfDayRequestsAreProcessedByTheScheduledExtraction() {
    OpsInvoice paid = fx.paidInvoice();
    EodRequest first =
        as.run("remit", () -> extraction.queueForEndOfDay(fx.company(), paid.getInvoiceNo()));
    EodRequest again =
        as.run("remit", () -> extraction.queueForEndOfDay(fx.company(), paid.getInvoiceNo()));
    assertThat(again.getId()).isEqualTo(first.getId());
    job.execute(LocalDate.now());
    Integer processed =
        jdbc.queryForObject(
            "select count(*) from rem_eod_request where id = ? and run_no is not null and tag = 'EXTRACTED'",
            Integer.class,
            first.getId());
    assertThat(processed).isEqualTo(1);
    assertThat(related.itemsFor(paid.getInvoiceNo()))
        .anySatisfy(i -> assertThat(i.type()).isEqualTo("REMITTANCE_BATCH"))
        .anySatisfy(i -> assertThat(i.type()).isEqualTo("EXTRACTION_TAG"));
  }

  @Test
  void aNegativeAdjustmentIsNotifiedToTheRemittanceTeam() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    as.run(
        "adjust",
        () ->
            tx.execute(
                s -> {
                  events.publishEvent(
                      new NegativeAdjustmentPending(
                          fx.company(), paid.getInvoiceNo(), "ENR-TEST", true, "adjust"));
                  events.publishEvent(
                      new NegativeAdjustmentPending(
                          fx.company(), paid.getInvoiceNo(), "ENR-TEST", false, "adjust"));
                  return null;
                }));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'remit' and entity_id = ?"
                    + " and title like 'Negative adjustment%'",
                Integer.class, paid.getInvoiceNo()))
        .isEqualTo(1);
  }

  @Test
  void theOperationsHomeAndApprovalInboxShowRemittanceWork() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    RemittanceBatch batch = fx.batchOf(paid.getInvoiceNo());
    as.run("remit", () -> batches.submit(batch.getId(), null));
    List<WorkCount> tiles = counts.counts(fx.company());
    assertThat(tiles)
        .extracting(WorkCount::key)
        .contains("remit-batches-approval", "remit-holds-active", "remit-special-approval");
    assertThat(tiles)
        .filteredOn(t -> t.key().equals("remit-batches-approval"))
        .singleElement()
        .satisfies(t -> assertThat(t.count()).isPositive());
    assertThat(
            approvals.pendingFor(
                ApprovalViewer.user(
                    "remittl", Set.of("REMIT_APPROVE", "HOLD_APPROVE", "SPECIAL_REMIT_APPROVE"))))
        .anySatisfy(p -> assertThat(p.reference()).isEqualTo(batch.getBatchNo()));
    assertThat(approvals.pendingFor(ApprovalViewer.user("remit", Set.of("REMIT_APPROVE"))))
        .noneSatisfy(p -> assertThat(p.reference()).isEqualTo(batch.getBatchNo()));
    assertThat(approvals.pendingFor(ApprovalViewer.user("ao", Set.of()))).isEmpty();
  }

  @Test
  void everyRemittanceReportRunsAndExports() {
    fx.approvedBatch(fx.paidInvoice());
    Map<String, String> params = new HashMap<>();
    params.put("companyId", fx.company().toString());
    params.put("from", "2026-01-01");
    params.put("to", LocalDate.now().plusDays(1).toString());
    as.run(
        "remittl",
        () -> {
          for (String code : REPORTS) {
            assertThat(reports.run(code, params).code()).isEqualTo(code);
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export(code, params, format).content())
                  .as(code + " " + format)
                  .isNotEmpty();
            }
          }
          return null;
        });
    Map<String, String> filtered = new HashMap<>(params);
    filtered.put("insurer", "INS-MGIC");
    filtered.put("status", "MATCHED");
    filtered.put("runNo", "FIR-NONE");
    filtered.put("batchNo", "RMB-NONE");
    as.run(
        "remit",
        () -> {
          assertThat(reports.run("REM-OR-EXCEPTION", filtered).rows()).isNotNull();
          assertThat(reports.run("REM-SCHEDULE-NORMAL", filtered).rows()).isNotNull();
          return null;
        });
  }
}
