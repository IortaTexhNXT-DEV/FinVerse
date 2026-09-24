package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.TestFlowInHandler;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** Operations foundation endpoints (opsledger, notification preferences) through HTTP. */
@IntegrationTest
class OperationsFoundationApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private OpsLedgerFixtures fx;
  @Autowired private DisbursementQueueService queue;
  @Autowired private ExtractRepositoryService extracts;
  @Autowired private HandoffService handoffs;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "cashier, /api/v1/ops/home?companyId={c}",
    "remittl, /api/v1/ops/home?companyId={c}",
    "comptrol, /api/v1/ops/home?companyId={c}",
    "ao, /api/v1/ops/invoices?companyId={c}",
    "cashier, /api/v1/ops/invoices?companyId={c}&q=BI&payment=UNPAID&remittance=WITH_OUTSTANDING_BALANCE"
        + "&from=2026-01-01&to=2026-12-31&dp=false&locked=false",
    "remit, /api/v1/ops/invoices?companyId={c}&flag=HOLD&insurer=INS-MGIC&client=CL-2026-000001",
    "remit, /api/v1/ops/accounts/ARN-2026-940001/invoices",
    "recon, /api/v1/ops/extracts?companyId={c}",
    "recon, /api/v1/ops/extracts?companyId={c}&folder=REMITTANCE",
    "adjust, /api/v1/ops/handoffs?companyId={c}",
    "adjust, /api/v1/ops/handoffs?companyId={c}&status=CLOSED",
    "disb, /api/v1/ops/disbursements?companyId={c}",
    "disb, /api/v1/ops/disbursements?companyId={c}&status=SENT&status=PAID",
    "admin, /api/v1/ops/flow-in/feeds",
    "admin, /api/v1/ops/flow-in/runs",
    "admin, /api/v1/ops/flow-in/runs?feed=OPS_INVOICE_FEED",
    "cashier, /api/v1/notifications/preferences",
    "cashier, /api/v1/reports/runs",
    "comptrol, /api/v1/reports/runs?code=OPS-TEST-ARCHIVED",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
    "uw, /api/v1/ops/home?companyId={c}",
    "cashier, /api/v1/ops/disbursements?companyId={c}",
    "cashier, /api/v1/ops/flow-in/feeds",
    "ao, /api/v1/ops/flow-in/runs",
  })
  void usersWithoutTheRightPermissionAreRefused(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isForbidden());
  }

  @Test
  void theHomeShowsOnlyTheUsersSections() throws Exception {
    api.doGet("cashier", "/api/v1/ops/home?companyId=" + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[?(@.section == 'CASHIERING')]").exists())
        .andExpect(jsonPath("$.sections[?(@.section == 'DISBURSEMENT')]").doesNotExist())
        .andExpect(jsonPath("$.links").isArray());
    api.doGet("disb", "/api/v1/ops/home?companyId=" + c())
        .andExpect(jsonPath("$.sections[0].section").value("DISBURSEMENT"));
  }

  @Test
  void anInvoiceIsSearchedViewedAndReplayed() throws Exception {
    BookedInvoice booked = fx.bookMotor();
    String no = booked.getInvoiceNo();
    api.doGet("cashier", "/api/v1/ops/invoices?companyId=" + c() + "&q=" + no)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].invoiceNo").value(no))
        .andExpect(jsonPath("$.content[0].paymentStatus").value("UNPAID"))
        .andExpect(jsonPath("$.content[0].flags.directPayment").value(false));
    api.doGet("cashier", "/api/v1/ops/invoices/" + no)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoice.keys.invoiceNo").value(no))
        .andExpect(jsonPath("$.invoice.components.length()").value(11))
        .andExpect(jsonPath("$.invoice.shares[0].lead").value(true))
        .andExpect(jsonPath("$.booking.bookedInvoiceId").isNumber())
        .andExpect(jsonPath("$.movements").isNotEmpty());
    api.doGet("cashier", "/api/v1/ops/invoices/" + no + "/movements")
        .andExpect(jsonPath("$[0].type").value("BOOKED"));
    api.doGet("cashier", "/api/v1/ops/invoices/" + no + "/history").andExpect(status().isOk());
    api.doGet("cashier", "/api/v1/ops/accounts/" + booked.getArn() + "/invoices")
        .andExpect(jsonPath("$[0].keys.invoiceNo").value(no));
    api.doGet("cashier", "/api/v1/ops/invoices/NO-SUCH").andExpect(status().isNotFound());

    api.doPost("admin", "/api/v1/ops/invoices/replay", Map.of("invoiceNo", no))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    api.doPost("admin", "/api/v1/ops/invoices/replay", Map.of("arn", booked.getArn()))
        .andExpect(status().isOk());
    api.doPost("admin", "/api/v1/ops/invoices/replay", Map.of("companyId", fx.company()))
        .andExpect(status().isOk());
    api.doPost("admin", "/api/v1/ops/invoices/replay", Map.of())
        .andExpect(status().isUnprocessableEntity());
    api.doPost("cashier", "/api/v1/ops/invoices/replay", Map.of("invoiceNo", no))
        .andExpect(status().isForbidden());
  }

  @Test
  void theDisbursementQueueIsWorkedThroughTheApi() throws Exception {
    String ref = "RMB-" + BookingFixtures.token();
    Long id =
        as.run(
            "remit",
            () ->
                tx.execute(
                    s ->
                        queue
                            .send(
                                fx.company(),
                                new DisbursementRequest.Spec(
                                    DisbursementRequest.Type.REMITTANCE,
                                    "REMITTANCE",
                                    ref,
                                    "INS-MGIC",
                                    "MGIC",
                                    "PHP",
                                    new BigDecimal("100.00"),
                                    "Batch",
                                    null))
                            .getId()));
    String base = "/api/v1/ops/disbursements/" + id;
    api.doGet("disb", base).andExpect(jsonPath("$.status").value("SENT"));
    api.doPost("disb", base + "/acknowledge", null)
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    api.doPost("disb", base + "/dv", Map.of("dvNo", "DV-1"))
        .andExpect(jsonPath("$.dvNo").value("DV-1"));
    api.doPost("disb", base + "/paid", null).andExpect(jsonPath("$.status").value("PAID"));
    api.doPost("disb", base + "/return", Map.of("reason", "Late"))
        .andExpect(status().isUnprocessableEntity());
    api.doPost("disb", base + "/dv", Map.of("dvNo", "")).andExpect(status().isBadRequest());
  }

  @Test
  void feedsAreConfiguredAndFilesUploaded() throws Exception {
    jdbc.update(
        "insert into ops_flow_in_feed (code, name, partner_system, direction, transport, owner_module,"
            + " created_at, created_by) values (?, 'Test feed', 'TEST', 'INBOUND', 'MANUAL_UPLOAD',"
            + " 'TEST', now(), 'TEST') on conflict (code) do nothing",
        TestFlowInHandler.FEED);
    api.doPut(
            "admin",
            "/api/v1/ops/flow-in/feeds/" + TestFlowInHandler.FEED,
            Map.of("cron", "-", "active", true))
        .andExpect(jsonPath("$.uploadable").value(true));
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "feed.txt",
            "text/plain",
            (BookingFixtures.token() + ";OK").getBytes(StandardCharsets.UTF_8));
    JsonNode run =
        api.read(
            mvc.perform(
                    multipart("/api/v1/ops/flow-in/feeds/" + TestFlowInHandler.FEED + "/upload")
                        .file(file)
                        .with(user(users.loadUserByUsername("admin")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED")));
    api.doGet("admin", "/api/v1/ops/flow-in/runs/" + run.get("id").asLong() + "/records")
        .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"));
  }

  @Test
  void extractsAreDownloadedAndHandoffsClosed() throws Exception {
    String folder = "API/" + BookingFixtures.token();
    Long fileId =
        as.run(
            "remit",
            () ->
                tx.execute(
                    s ->
                        extracts
                            .store(
                                fx.company(),
                                new ExtractFile.Location(folder, "a.csv"),
                                new DropContent("text/csv", "x\n".getBytes(StandardCharsets.UTF_8)),
                                new ExtractFile.Origin("REMITTANCE", null))
                            .id()));
    api.doGet("recon", "/api/v1/ops/extracts/" + fileId + "/file")
        .andExpect(status().isOk())
        .andExpect(content().bytes("x\n".getBytes(StandardCharsets.UTF_8)));

    Long handoffId =
        as.run(
            "remit",
            () ->
                tx.execute(
                    s ->
                        handoffs
                            .record(
                                fx.company(),
                                "ReceiptIssuer",
                                "CASH_RECEIPT",
                                new OpsHandoff.Spec(
                                    "REMITTANCE",
                                    "API-" + folder,
                                    null,
                                    BigDecimal.TEN,
                                    "PHP",
                                    "Issue an OR",
                                    null))
                            .getId()));
    api.doPost("recon", "/api/v1/ops/handoffs/" + handoffId + "/close", Map.of("reason", "Done"))
        .andExpect(status().isForbidden());
    api.doPost("cashier", "/api/v1/ops/handoffs/" + handoffId + "/close", Map.of("reason", "Done"))
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void notificationPreferencesAreKeptPerUser() throws Exception {
    api.doPut(
            "remit",
            "/api/v1/notifications/preferences/REMIT_EXTRACTION_DONE",
            Map.of("inApp", false, "email", true))
        .andExpect(jsonPath("$.inApp").value(false))
        .andExpect(jsonPath("$.custom").value(true));
    api.doGet("remit", "/api/v1/notifications/preferences")
        .andExpect(jsonPath("$[?(@.code == 'REMIT_EXTRACTION_DONE')].email").value(true));
    api.doPut(
            "remit",
            "/api/v1/notifications/preferences/NO_SUCH_EVENT",
            Map.of("inApp", true, "email", true))
        .andExpect(status().isNotFound());
  }
}
