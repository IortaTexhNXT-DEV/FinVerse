package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** ACSL endpoints through the full HTTP stack (ACSL 2.4-2.16). */
@IntegrationTest
class AcslApiIT {

  private static final String BASE = "/api/v1/acsl";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private AcslFixtures fx;

  private JsonNode upload(OpsInvoice invoice) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "soa.csv",
            "text/csv",
            AcslFixtures.file(AcslFixtures.row(invoice.getInvoiceNo(), "100", "100")));
    return api.read(
        mvc.perform(
                multipart(BASE + "/soa-uploads")
                    .file(file)
                    .param("companyId", String.valueOf(fx.company()))
                    .param("insurerCode", invoice.getInsurerCode())
                    .param("periodFrom", "2026-01-01")
                    .param("periodTo", "2026-06-30")
                    .with(user(users.loadUserByUsername("acsl")))
                    .with(csrf()))
            .andExpect(status().isOk()));
  }

  @Test
  void soaAndGlSlEndpointsRespond() throws Exception {
    OpsInvoice invoice = fx.invoice();
    JsonNode uploaded = upload(invoice);
    assertThat(uploaded.get("rowsLoaded").asInt()).isEqualTo(1);
    assertThat(uploaded.get("run").get("outstanding").asInt()).isEqualTo(1);
    String id = uploaded.get("id").asText();
    String c = "?companyId=" + fx.company();
    api.doPost("acsl", BASE + "/gl-sl/runs" + c, Map.of()).andExpect(status().isOk());
    JsonNode runs =
        api.read(api.doGet("acsl", BASE + "/gl-sl/runs" + c).andExpect(status().isOk()));
    String runId = runs.get(0).get("id").asText();
    for (String url :
        List.of(
            BASE + "/soa-uploads" + c,
            BASE + "/soa-uploads" + c + "&q=soa&page=0&size=5",
            BASE + "/soa-uploads/" + id,
            BASE + "/soa-uploads/" + id + "/log",
            BASE + "/soa-uploads/" + id + "/results",
            BASE + "/soa-uploads/" + id + "/results?bucket=OUTSTANDING",
            BASE + "/soa-layouts",
            BASE + "/gl-sl/runs/" + runId + "/rows",
            BASE + "/gl-sl/controls" + c)) {
      api.doGet("acslhead", url).andExpect(status().isOk());
    }
    api.doGet("acsl", BASE + "/soa-uploads/" + id + "/report")
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString(
                        invoice.getInsurerCode() + "_2026-01-01")));
    api.doPost("acsl", BASE + "/soa-uploads/" + id + "/reconcile", Map.of())
        .andExpect(jsonPath("$.run.runNo").value(2));
    api.doPut(
            "acsltl",
            BASE + "/gl-sl/controls" + c,
            Map.of(
                "accountCode",
                "2210",
                "source",
                "OPS_LEDGER",
                "components",
                "DTIP",
                "active",
                true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("OPS_LEDGER"));
  }

  @Test
  void casesAndCorrectionsOverHttp() throws Exception {
    OpsInvoice invoice = fx.invoice();
    String c = "?companyId=" + fx.company();
    JsonNode opened =
        api.read(
            api.doPost(
                    "acsl",
                    BASE + "/cases" + c,
                    Map.of(
                        "type", "INVESTIGATION",
                        "invoiceNo", invoice.getInvoiceNo(),
                        "subject", "HTTP investigation"))
                .andExpect(status().isOk()));
    String caseUrl = BASE + "/cases/" + opened.get("id").asText();
    api.doPost("acsltl", caseUrl + "/assign", Map.of("username", "acsl"))
        .andExpect(jsonPath("$.stage").value("ASSIGNED"));
    var original = fx.bookingLine(invoice, "1210.01");
    JsonNode correction =
        api.read(
            api.doPost(
                    "acsltl",
                    BASE + "/corrections" + c,
                    Map.of(
                        "kind",
                        "RECLASS",
                        "invoiceNo",
                        invoice.getInvoiceNo(),
                        "originalBatchNo",
                        original.batchNo(),
                        "description",
                        "HTTP reclass"))
                .andExpect(status().isOk()));
    String url = BASE + "/corrections/" + correction.get("id").asText();
    api.doPost("acsltl", url + "/assign", Map.of("username", "acsl"))
        .andExpect(jsonPath("$.stage").value("DRAFT"));
    String party = original.partyCode();
    api.doPut(
            "acsl",
            url + "/lines",
            Map.of(
                "lines",
                List.of(
                    Map.of(
                        "accountCode",
                        "1210.01",
                        "side",
                        "CREDIT",
                        "amount",
                        5,
                        "partyCode",
                        party),
                    Map.of(
                        "accountCode",
                        "1210.06",
                        "side",
                        "DEBIT",
                        "amount",
                        5,
                        "partyCode",
                        party))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDebit").value(5.0));
    for (String read :
        List.of(
            BASE + "/cases" + c,
            BASE + "/cases" + c + "&stage=ASSIGNED&type=INVESTIGATION&q=http",
            BASE + "/cases/counts" + c,
            caseUrl,
            BASE + "/invoices/" + invoice.getInvoiceNo() + "/cases",
            BASE + "/corrections" + c,
            BASE + "/corrections" + c + "&stage=DRAFT&q=http",
            BASE + "/corrections/counts" + c,
            url,
            url + "/original-lines")) {
      api.doGet("acslhead", read).andExpect(status().isOk());
    }
    api.doPost("acsl", url + "/submit", Map.of())
        .andExpect(jsonPath("$.stage").value("FOR_REVIEW"));
    api.doPost("acsltl", url + "/endorse", Map.of()).andExpect(status().isOk());
    api.doPost("acslhead", url + "/approve", Map.of("comment", "OK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("POSTED"))
        .andExpect(jsonPath("$.openItems").value(2));
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    String c = "?companyId=" + fx.company();
    api.doGet("mktao", BASE + "/cases" + c).andExpect(status().isForbidden());
    api.doPost("acslhead", BASE + "/cases" + c, Map.of("type", "INVESTIGATION", "subject", "x"))
        .andExpect(status().isForbidden());
    api.doPost("acsl", BASE + "/corrections" + c, Map.of("kind", "OTHER", "description", "x"))
        .andExpect(status().isForbidden());
    api.doPost("acsl", BASE + "/corrections/1/approve", Map.of()).andExpect(status().isForbidden());
    api.doPut(
            "acsl",
            BASE + "/gl-sl/controls" + c,
            Map.of("accountCode", "2210", "source", "OPS_LEDGER"))
        .andExpect(status().isForbidden());
    api.doPost("acsl", BASE + "/cases" + c, Map.of("subject", ""))
        .andExpect(status().isBadRequest());
  }
}
