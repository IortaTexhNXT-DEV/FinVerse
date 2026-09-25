package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.report.TestArchivedReport;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.report.domain.ReportRunAction;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * HTTP contract of the Collections / Accounting foundation: the invoice family (DIS 3.27.2), the
 * cancellation of a payment request (DIS 2.20.0) and scheduled report files with an availability
 * time (BRCLXN.024-029, 045).
 */
@IntegrationTest
class CollectionsAccountingFoundationApiIT {

  @Autowired private Api api;
  @Autowired private OpsLedgerFixtures fx;
  @Autowired private DisbursementQueueService queue;
  @Autowired private ReportArchiveService archive;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;

  @Test
  void theInvoiceFamilyIsReadThroughTheApi() throws Exception {
    BookedInvoice booked = fx.bookMotor();
    String no = booked.getInvoiceNo();
    api.doGet("cashier", "/api/v1/ops/invoices/" + no + "/family")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].keys.invoiceNo").value(no))
        .andExpect(jsonPath("$[0].keys.rootInvoiceNo").value(no));
    api.doGet("mktcoll", "/api/v1/ops/invoices/" + no).andExpect(status().isOk());
    api.doGet("uw", "/api/v1/ops/invoices/" + no + "/family").andExpect(status().isForbidden());
    api.doGet("cashier", "/api/v1/ops/invoices/NO-SUCH/family")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void aPaymentRequestIsCancelledThroughTheApi() throws Exception {
    String ref = "CAN-" + BookingFixtures.token();
    Long id =
        as.run(
            "cashier",
            () ->
                tx.execute(
                    s ->
                        queue
                            .send(
                                fx.company(),
                                new DisbursementRequest.Spec(
                                        DisbursementRequest.Type.REFUND,
                                        "TESTMOD",
                                        ref,
                                        BookingFixtures.CLIENT,
                                        null,
                                        "PHP",
                                        new BigDecimal("50.00"),
                                        "Refund",
                                        null)
                                    .routed("RFP-" + ref, "CLIENT", "REFUND", null, true))
                            .getId()));
    String base = "/api/v1/ops/disbursements/" + id;
    api.doGet("disb", base)
        .andExpect(jsonPath("$.rfpNo").value("RFP-" + ref))
        .andExpect(jsonPath("$.cancelledAt").doesNotExist());
    api.doPost("cashier", base + "/cancel", Map.of("reason", "Duplicate"))
        .andExpect(status().isForbidden());
    api.doPost("disb", base + "/cancel", Map.of("reason", "Duplicate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelReason").value("Duplicate"));
    api.doPost("disb", base + "/cancel", Map.of("reason", "Again"))
        .andExpect(status().isUnprocessableEntity());
    api.doGet("disb", "/api/v1/ops/disbursements?companyId=" + fx.company() + "&status=CANCELLED")
        .andExpect(status().isOk());
  }

  @Test
  void scheduledFilesAreDownloadedFromTheirAvailabilityTime() throws Exception {
    byte[] content = "invoice,balance\n".getBytes(StandardCharsets.UTF_8);
    RunFile file = new RunFile("CSV", "outstanding.csv", "text/csv", content);
    ReportRun later =
        as.run(
            "admin",
            () ->
                tx.execute(
                    s ->
                        archive.archiveGenerated(
                            TestArchivedReport.CODE,
                            List.of("Period: 2026-09"),
                            1,
                            file,
                            Instant.now().plus(Duration.ofDays(1)))));
    ReportRun ready =
        as.run(
            "admin",
            () ->
                tx.execute(
                    s ->
                        archive.archiveGenerated(
                            TestArchivedReport.CODE,
                            List.of("Period: 2026-08"),
                            1,
                            file,
                            Instant.now().minus(Duration.ofMinutes(1)))));
    assertThat(later.getAction()).isEqualTo(ReportRunAction.GENERATE);
    api.doGet("comptrol", "/api/v1/reports/runs?code=" + TestArchivedReport.CODE)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.action == 'GENERATE')]").exists());
    api.doGet("cashier", "/api/v1/reports/runs/" + later.getId() + "/file")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("REPORT_FILE_NOT_AVAILABLE"));
    api.doGet("cashier", "/api/v1/reports/runs/" + ready.getId() + "/file")
        .andExpect(status().isOk());

    ReportRun generated =
        as.run(
            "admin",
            () ->
                reports.generate(
                    TestArchivedReport.CODE, Map.of("note", "job"), ExportFormat.CSV, null));
    assertThat(generated.getAction()).isEqualTo(ReportRunAction.GENERATE);
    assertThat(generated.getRowCount()).isEqualTo(1);
    assertThat(generated.getAvailableFrom()).isNull();
    api.doGet("cashier", "/api/v1/reports/runs/" + generated.getId() + "/file")
        .andExpect(status().isOk());
  }
}
