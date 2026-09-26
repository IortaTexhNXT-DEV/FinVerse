package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.PROCESSOR;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.domain.WriteOffAction;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentBatchHandler;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentDailyReportJob;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.MinimalBalanceFileHandler;
import com.iortatechnxt.brokerverse.adjustment.service.WriteOffService;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Minimal balance file and batch requests by upload (ADJID.006/026), the Adjustment reports
 * (ADJID.016/017/019/021/026) and the {@code ADJ_DAILY_REPORT} job.
 */
@IntegrationTest
class AdjustmentFilesAndReportsIT {

  private static final List<String> REPORTS =
      List.of("ADJ-DAILY", "ADJ-VALIDATION-LIST", "ADJ-REGISTER", "ADJ-AGING", "ADJ-MINBAL-FILE");

  @Autowired private AdjustmentFixtures fx;
  @Autowired private MinimalBalanceFileHandler minimalBalance;
  @Autowired private AdjustmentBatchHandler batchUpload;
  @Autowired private WriteOffService writeOffs;
  @Autowired private AdjustmentQueryService queries;
  @Autowired private ReportService reports;
  @Autowired private AdjustmentDailyReportJob job;
  @Autowired private AsUser as;

  private BulkContext context() {
    return new BulkContext(
        fx.company(), "BLK-ADJ-" + BookingFixtures.token(), LocalDate.now(), Map.of());
  }

  private static BulkRow line(int n, String invoiceNo, String balance) {
    return new BulkRow(n, Map.of("Invoice No", invoiceNo, "Balance", balance));
  }

  @Test
  void theMinimalBalanceFileWritesOffDebitsAndCreditsCreditsWithinTheRange() {
    OpsInvoice debit = fx.invoice();
    fx.payAllBut(debit, new BigDecimal("50.00"));
    OpsInvoice credit = fx.invoice();
    fx.payInFull(credit);
    fx.overpay(credit, new BigDecimal("20.00"));
    OpsInvoice large = fx.invoice();
    fx.payAllBut(large, new BigDecimal("500.00"));
    BulkContext context = context();

    assertThat(minimalBalance.validate(line(1, "NO-SUCH-INVOICE", "10.00"), context)).isNotEmpty();
    assertThat(minimalBalance.validate(line(2, large.getInvoiceNo(), "500.00"), context))
        .anySatisfy(e -> assertThat(e).contains("outside"));
    assertThat(minimalBalance.validate(line(3, debit.getInvoiceNo(), "49.00"), context))
        .anySatisfy(e -> assertThat(e).contains("does not match"));
    BulkRow good = line(4, debit.getInvoiceNo(), "50.00");
    assertThat(minimalBalance.validate(good, context)).isEmpty();
    BulkOutcome outcome = as.run(PROCESSOR, () -> minimalBalance.process(good, context));
    assertThat(outcome.category()).isEqualTo(WriteOffAction.WRITE_OFF.name());
    OpsInvoice written = fx.reload(debit);
    assertThat(written.premiumBalance()).isZero();
    assertThat(written.isWrittenOff()).isTrue();
    assertThat(written.component(LedgerComponent.BASIC).getWrittenOff())
        .isEqualByComparingTo("50.00");
    assertThat(minimalBalance.validate(good, context))
        .anySatisfy(e -> assertThat(e).contains("already written off"));

    assertThat(fx.reload(credit).premiumBalance()).isEqualByComparingTo("-20.00");
    BulkRow overpaid = line(5, credit.getInvoiceNo(), "-20.00");
    assertThat(minimalBalance.validate(overpaid, context)).isEmpty();
    BulkOutcome credited = as.run(PROCESSOR, () -> minimalBalance.process(overpaid, context));
    assertThat(credited.category()).isEqualTo(WriteOffAction.CREDIT.name());
    assertThat(fx.reload(credit).premiumBalance()).isZero();
    assertThat(writeOffs.fileRange().min()).isEqualByComparingTo("10.00");
    assertThat(queries.writeOffs(fx.company(), PageRequest.of(0, 50)).getContent())
        .anySatisfy(i -> assertThat(i.getInvoiceNo()).isEqualTo(debit.getInvoiceNo()));
    assertThat(minimalBalance.outcomeCategories()).containsExactly("WRITE_OFF", "CREDIT");
    assertThat(minimalBalance.blocksDuplicateFiles()).isTrue();
    assertThat(minimalBalance.columns()).hasSize(2);
  }

  @Test
  void aBatchUploadRaisesAndSubmitsOneRequestPerRow() {
    OpsInvoice invoice = fx.invoice();
    Map<String, String> values = new HashMap<>();
    values.put("Invoice No", invoice.getInvoiceNo());
    values.put("Endorsement Type", "FIN_CHANGE_COVER");
    values.put("Request Type", "FLAT_CANCELLATION");
    values.put("Reason", "UNIT_SOLD");
    values.put("Effective Date", FROM.toString());
    values.put("Refund Basis", "pro_rata");
    values.put("Description", "Uploaded cancellation");
    BulkRow row = new BulkRow(1, values);
    BulkContext context = context();

    assertThat(batchUpload.validate(row, context)).isEmpty();
    String requestNo = as.run(PROCESSOR, () -> batchUpload.commit(row, context));
    assertThat(requestNo).startsWith("ENR-");
    assertThat(queries.forInvoice(invoice.getInvoiceNo()))
        .singleElement()
        .satisfies(r -> assertThat(r.getStage()).isEqualTo(RequestStage.FOR_VALIDATION));
    assertThat(batchUpload.validate(row, context)).isNotEmpty();
    BulkRow bad = new BulkRow(2, Map.of("Invoice No", "NOPE", "Endorsement Type", "FIN_TSI"));
    assertThat(batchUpload.validate(bad, context)).isNotEmpty();
    assertThat(batchUpload.duplicateKey(row)).contains(invoice.getInvoiceNo());
    assertThat(batchUpload.columns()).hasSize(17);
    assertThat(batchUpload.permission()).isEqualTo("ADJ_POST");
  }

  @Test
  void theReportsRunAndExportAndTheDailyJobArchivesTheDay() {
    EndorsementRequest posted =
        fx.raiseAndPost(
            fx.invoice(),
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
            AmountInput.NONE);
    Map<String, String> params =
        Map.of(
            "companyId", String.valueOf(fx.company()),
            "from", LocalDate.now().minusDays(1).toString(),
            "to", LocalDate.now().plusDays(1).toString());

    as.run(
        "adjtl",
        () -> {
          for (String code : REPORTS) {
            assertThat(reports.run(code, params).code()).isEqualTo(code);
            assertThat(reports.export(code, params, ExportFormat.CSV).content()).isNotEmpty();
          }
          return null;
        });
    String validationList =
        new String(
            as.run("adjtl", () -> reports.export("ADJ-VALIDATION-LIST", params, ExportFormat.CSV))
                .content(),
            StandardCharsets.UTF_8);
    assertThat(validationList).contains(posted.getRequestNo());
    assertThat(job.name()).isEqualTo("ADJ_DAILY_REPORT");
    assertThat(job.execute(LocalDate.now()).itemsProcessed()).isPositive();
  }
}
