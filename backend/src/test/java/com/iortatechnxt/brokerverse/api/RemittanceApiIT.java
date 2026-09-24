package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Remittance endpoints through HTTP: reads, permissions and the batch, hold and special flows. */
@com.iortatechnxt.brokerverse.support.IntegrationTest
class RemittanceApiIT {

  private static final String BASE = "/api/v1/remittance";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private RemittanceFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  private ResultActions upload(String username, String url, String content) throws Exception {
    return mvc.perform(
        multipart(url)
            .file(
                new MockMultipartFile(
                    "file", "file.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8)))
            .with(user(users.loadUserByUsername(username)))
            .with(csrf()));
  }

  @ParameterizedTest
  @CsvSource({
    "remit, /runs?companyId={c}",
    "remittl, /eod-requests?companyId={c}",
    "remit, /accounts?companyId={c}&q=RMB",
    "remit, /dtip?companyId={c}&q=INV&insurer=INS-MGIC&status=UNPROCESSED&payment=PAID",
    "remit, /incentive-rules?companyId={c}",
    "remit, /insurer-or/runs",
    "remit, /batches?companyId={c}",
    "remittl, /batches?companyId={c}&stage=FOR_APPROVAL&stage=REVIEW_IN_PROCESS&insurer=INS-MGIC&type=NORMAL_PHP&q=RMB",
    "mktcoll, /holds?companyId={c}",
    "mkttl, /holds?companyId={c}&stage=FOR_APPROVAL&q=HLD",
    "remit, /holds?companyId={c}",
    "mktcoll, /special?companyId={c}",
    "remittl, /special?companyId={c}&stage=FOR_APPROVAL&q=SPR",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /runs?companyId={c}",
    "cashier, /batches?companyId={c}",
    "ao, /holds?companyId={c}",
    "cashier, /special?companyId={c}",
    "mktcoll, /dtip?companyId={c}",
  })
  void usersWithoutTheRightPermissionAreRefused(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isForbidden());
  }

  @Test
  void aBatchIsExtractedReviewedApprovedAndDocumentedThroughTheApi() throws Exception {
    OpsInvoice paid = fx.paidInvoice();
    String no = paid.getInvoiceNo();
    api.doPost("mktcoll", BASE + "/runs", Map.of("companyId", fx.company(), "invoiceNo", no))
        .andExpect(status().isForbidden());
    JsonNode run =
        api.read(
            api.doPost("remit", BASE + "/runs", Map.of("companyId", fx.company(), "invoiceNo", no))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.extracted").value(1)));
    api.doGet("remit", BASE + "/runs/" + run.get("id").asLong()).andExpect(status().isOk());
    api.doGet("remit", BASE + "/runs/" + run.get("id").asLong() + "/tags")
        .andExpect(jsonPath("$.content[0].tag").value("EXTRACTED"));
    JsonNode account =
        api.read(
            api.doGet("remit", BASE + "/accounts?companyId=" + c() + "&q=" + no)
                .andExpect(jsonPath("$.content[0].line.invoiceNo").value(no)));
    long id = account.get("content").get(0).get("batchId").asLong();
    String batch = BASE + "/batches/" + id;

    api.doGet("remittl", batch)
        .andExpect(jsonPath("$.summary.stage").value("REVIEW_IN_PROCESS"))
        .andExpect(jsonPath("$.lines[0].invoiceNo").value(no));
    api.doPost(
            "remit", batch + "/exclude", Map.of("invoiceNos", List.of(no), "reasonCode", "OTHERS"))
        .andExpect(jsonPath("$.lines[0].exclusion.excluded").value(true));
    api.doPost("remit", batch + "/exclude", Map.of("invoiceNos", List.of(), "reasonCode", "OTHERS"))
        .andExpect(status().isBadRequest());
    api.doPost("remit", batch + "/restore", Map.of("invoiceNo", no))
        .andExpect(jsonPath("$.summary.lineCount").value(1));
    api.doGet("remit", batch + "/preview")
        .andExpect(jsonPath("$.lineCount").value(1))
        .andExpect(jsonPath("$.problems").isEmpty());
    api.doGet("remit", batch + "/documents/SCHEDULE_PDF")
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
    api.doPost("remittl", batch + "/assign", Map.of("username", "remit"))
        .andExpect(jsonPath("$.summary.processor").value("remit"));
    api.doPost("remit", batch + "/submit", Map.of("comment", "Checked"))
        .andExpect(jsonPath("$.summary.stage").value("FOR_APPROVAL"));
    api.doPost("remit", batch + "/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost("remittl", batch + "/approve", Map.of("comment", "OK"))
        .andExpect(jsonPath("$.summary.stage").value("APPROVED"))
        .andExpect(jsonPath("$.disbursement.requestNo").isNotEmpty());
    api.doGet("remit", batch + "/documents/PAYMENT_REQUEST_PDF").andExpect(status().isOk());
    api.doGet("remit", batch + "/documents/SCHEDULE_XLSX").andExpect(status().isOk());
    api.doPost(
            "remit",
            batch + "/send-schedule",
            Map.of("to", List.of("remit@insurer.ph"), "subject", "Schedule", "body", "Attached"))
        .andExpect(jsonPath("$.messageId").isNumber());
    api.doGet("remit", BASE + "/dtip?companyId=" + c() + "&q=" + no)
        .andExpect(jsonPath("$.content[0].remittanceStatus").value("APPROVED"))
        .andExpect(jsonPath("$.content[0].tag").value("EXTRACTED"));

    JsonNode result =
        api.read(
            upload(
                    "remit",
                    BASE + "/insurer-or/upload",
                    "batchNo,invoiceNo,orNo,orDate,orAmount\n"
                        + account.get("content").get(0).get("batchNo").asText()
                        + ",NOT-IN-BATCH,OR-1,2026-09-20,1\n")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.failed").value(1)));
    api.doGet("remit", BASE + "/insurer-or/runs/" + result.get("run").get("id").asLong())
        .andExpect(jsonPath("$.records[0].status").value("FAILED"));
    upload("mktcoll", BASE + "/insurer-or/upload", "x").andExpect(status().isForbidden());
  }

  @Test
  void aBatchIsReturnedThroughTheApi() throws Exception {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    long id = fx.batchOf(paid.getInvoiceNo()).getId();
    api.doPost("remit", BASE + "/batches/" + id + "/return", Map.of("comment", "x"))
        .andExpect(status().isBadRequest());
    api.doPost("remit", BASE + "/batches/" + id + "/return", Map.of("reasonCode", "OTHERS"))
        .andExpect(jsonPath("$.summary.stage").value("RETURNED"));
    api.doPost(
            "remit",
            BASE + "/eod-requests",
            Map.of("companyId", fx.company(), "invoiceNo", paid.getInvoiceNo()))
        .andExpect(jsonPath("$.invoiceNo").value(paid.getInvoiceNo()));
  }

  @Test
  void holdsAreRequestedAndDecidedThroughTheApi() throws Exception {
    OpsInvoice paid = fx.paidInvoice();
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", fx.company());
    body.put("invoiceNo", paid.getInvoiceNo());
    body.put("reasonCode", "OTHERS");
    body.put("holdUntil", LocalDate.now().plusDays(5).toString());
    body.put("submit", false);
    JsonNode hold =
        api.read(
            api.doPost("mktcoll", BASE + "/holds", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("DRAFT")));
    String url = BASE + "/holds/" + hold.get("id").asLong();
    api.doPut(
            "mktcoll",
            url,
            Map.of(
                "reasonCode",
                "OTHERS",
                "remarks",
                "Changed",
                "holdUntil",
                LocalDate.now().plusDays(6).toString()))
        .andExpect(jsonPath("$.remarks").value("Changed"));
    api.doPost("mktcoll", url + "/submit", null)
        .andExpect(jsonPath("$.stage").value("FOR_APPROVAL"));
    api.doPost("mktcoll", url + "/decision", Map.of("approve", true))
        .andExpect(status().isForbidden());
    api.doPost("mkttl", url + "/decision", Map.of("approve", true, "comment", "OK"))
        .andExpect(jsonPath("$.stage").value("ACTIVE"));
    api.doPost("mkttl", url + "/assign", Map.of("username", "remit"))
        .andExpect(jsonPath("$.assignedProcessor").value("remit"));
    api.doPost(
            "mktcoll", url + "/extend", Map.of("holdUntil", LocalDate.now().plusDays(9).toString()))
        .andExpect(jsonPath("$.stage").value("EXTENSION_FOR_APPROVAL"));
    api.doPost("mkttl", url + "/extension-decision", Map.of("approve", true))
        .andExpect(jsonPath("$.extensionCount").value(1));
    api.doPost("mktcoll", url + "/request-cancel", Map.of("comment", "Resolved"))
        .andExpect(jsonPath("$.stage").value("CANCEL_FOR_APPROVAL"));
    api.doPost("mkttl", url + "/cancel-decision", Map.of("approve", false))
        .andExpect(jsonPath("$.stage").value("ACTIVE"));
    api.doPost("mktcoll", url + "/release", Map.of("comment", "Paid"))
        .andExpect(jsonPath("$.stage").value("RELEASED"));
    api.doGet("remit", url)
        .andExpect(jsonPath("$.requestNo").value(hold.get("requestNo").asText()));

    body.put("holdUntil", LocalDate.now().plusDays(3).toString());
    JsonNode draft =
        api.read(api.doPost("mktcoll", BASE + "/holds", body).andExpect(status().isCreated()));
    api.doPost("mktcoll", BASE + "/holds/" + draft.get("id").asLong() + "/cancel", null)
        .andExpect(jsonPath("$.stage").value("CANCELLED"));
    upload(
            "mktcoll",
            BASE + "/holds/upload",
            "invoiceNo,reasonCode,holdUntil\nNO-SUCH,"
                + "OTHERS,"
                + LocalDate.now().plusDays(3)
                + "\n")
        .andExpect(jsonPath("$.failed").value(1));
  }

  @Test
  void specialRemittancesAreRequestedAndDecidedThroughTheApi() throws Exception {
    OpsInvoice paid = fx.paidInvoice();
    Map<String, Object> body =
        Map.of(
            "companyId",
            fx.company(),
            "invoiceNo",
            paid.getInvoiceNo(),
            "conditionCode",
            "RENEWAL");
    JsonNode request =
        api.read(
            api.doPost("mktcoll", BASE + "/special", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("FOR_APPROVAL")));
    String url = BASE + "/special/" + request.get("id").asLong();
    api.doGet("remittl", url).andExpect(jsonPath("$.conditionCode").value("RENEWAL"));
    api.doPost("mktcoll", url + "/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost("remittl", url + "/reject", Map.of("reasonCode", "OTHERS", "comment", "No"))
        .andExpect(jsonPath("$.stage").value("REJECTED"));
    JsonNode again =
        api.read(api.doPost("mktcoll", BASE + "/special", body).andExpect(status().isCreated()));
    api.doPost(
            "remittl",
            BASE + "/special/" + again.get("id").asLong() + "/approve",
            Map.of("comment", "OK"))
        .andExpect(jsonPath("$.stage").value("IN_PROCESS_REMITTANCE"))
        .andExpect(jsonPath("$.batchNo").isNotEmpty());
    upload("mktcoll", BASE + "/special/upload", "invoiceNo,conditionCode\nNO-SUCH,RENEWAL\n")
        .andExpect(jsonPath("$.failed").value(1));
  }

  @Test
  void incentiveRulesAreMaintainedThroughTheApi() throws Exception {
    Map<String, Object> rule = new HashMap<>();
    rule.put("companyId", fx.company());
    rule.put("insurerCode", "INS-API");
    rule.put("rate", 1.5);
    rule.put("windowDays", 30);
    rule.put("basis", "INCEPTION");
    rule.put("effectiveFrom", "2026-01-01");
    rule.put("active", true);
    api.doPost("remit", BASE + "/incentive-rules", rule).andExpect(status().isForbidden());
    JsonNode created =
        api.read(
            api.doPost("remittl", BASE + "/incentive-rules", rule)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.insurerCode").value("INS-API")));
    rule.put("active", false);
    api.doPut("remittl", BASE + "/incentive-rules/" + created.get("id").asLong(), rule)
        .andExpect(jsonPath("$.active").value(false));
    rule.remove("companyId");
    api.doPost("remittl", BASE + "/incentive-rules", rule)
        .andExpect(status().isUnprocessableEntity());
    rule.put("rate", 0);
    api.doPost("remittl", BASE + "/incentive-rules", rule).andExpect(status().isBadRequest());
  }
}
