package com.iortatechnxt.brokerverse.frbs;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The FRBS endpoints through the full HTTP stack (FRBS 2.10, 3.2.0, DIS 2.11): report pack, account
 * schedules and commentary, service-fee runs, rules and recipients, received certificates, and the
 * Excel and PDF exports of every report of the pack.
 */
@IntegrationTest
class FrbsApiIT {

  private static final String FEE = "/api/v1/frbs/service-fee";
  private static final String SCHEDULES = "/api/v1/finreport/schedules";
  private static final String CERTS = "/api/v1/tax/received-certificates";
  private static final List<String> FRBS_REPORTS =
      List.of(
          "FRBS-SERVICE-FEE",
          "FRBS-SERVICE-FEE-DETAIL",
          "FRBS-MANCOM-MARKET",
          "FRBS-BRANCH-PRODUCTION",
          "FRBS-BRANCH-PRODUCTION-SUM",
          "FRBS-EXPENSE-GROUPING",
          "FRBS-GAP",
          "FRBS-CASH-FLOW",
          "FRBS-SUSTAINABILITY-PROD",
          "GL-SCHEDULE");

  @Autowired private Api api;
  @Autowired private FrbsFixtures fx;
  @Autowired private MockMvc mvc;

  private Map<String, String> reportParams() {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(fx.company()));
    p.put("from", LocalDate.now().withDayOfYear(1).toString());
    p.put("to", LocalDate.now().toString());
    p.put("year", String.valueOf(LocalDate.now().getYear()));
    p.put("schedule", "SCH-COMM-RECEIVABLE");
    p.put("asOf", LocalDate.now().toString());
    return p;
  }

  @Test
  void readEndpointsRespond() throws Exception {
    ServiceFeeRun run = fx.approvedRun();
    String c = "?companyId=" + fx.company();
    String runUrl = FEE + "/runs/" + run.getId();
    for (String url :
        List.of(
            "/api/v1/frbs/report-pack",
            SCHEDULES,
            SCHEDULES + "?activeOnly=true",
            SCHEDULES + "/SCH-PR-PHP",
            SCHEDULES + "/GARD-VARIANCE-SIE/comments" + c + "&period=" + YearMonth.now(),
            FEE + "/runs" + c,
            FEE + "/runs" + c + "&stage=APPROVED&q=sfr&page=0&size=5",
            FEE + "/runs/counts" + c,
            runUrl,
            runUrl + "/lines",
            runUrl + "/invoices",
            FEE + "/rules",
            FEE + "/recipients" + c,
            CERTS + c,
            CERTS + c + "&status=RECORDED&q=ins")) {
      api.doGet(FrbsFixtures.OFFICER, url).andExpect(status().isOk());
    }
    api.doGet(FrbsFixtures.OFFICER, runUrl)
        .andExpect(jsonPath("$.stage").value("APPROVED"))
        .andExpect(jsonPath("$.runNo").value(run.getRunNo()));
    api.doGet(FrbsFixtures.OFFICER, runUrl + "/lines")
        .andExpect(jsonPath("$[0].status").exists())
        .andExpect(jsonPath("$[0].amounts.fee").exists());
    JsonNode pack = api.read(api.doGet(FrbsFixtures.OFFICER, "/api/v1/frbs/report-pack"));
    assertThat(pack.findValuesAsText("reportCode")).contains("GL-SCHEDULE", "TAX-SAWT", "FRBS-GAP");
    for (JsonNode entry : pack) {
      if (entry.get("reportCode").asText().startsWith("TAX-")
          || entry.get("reportCode").asText().startsWith("FRBS-")) {
        assertThat(entry.get("available").asBoolean()).as(entry.toString()).isTrue();
      }
    }
    api.doGet(FrbsFixtures.OFFICER, SCHEDULES + "/SCH-PR-USD")
        .andExpect(jsonPath("$.values.currency").value("USD"))
        .andExpect(jsonPath("$.wordOutput").value(false));
  }

  @Test
  void everyReportOfThePackExportsToExcelAndPdf() throws Exception {
    for (String code : FRBS_REPORTS) {
      api.doPost(FrbsFixtures.OFFICER, "/api/v1/reports/" + code + "/run", reportParams())
          .andExpect(status().isOk());
      api.doPost(
              FrbsFixtures.OFFICER,
              "/api/v1/reports/" + code + "/export?format=XLSX",
              reportParams())
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .contentType(
                      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
      api.doPost(
              FrbsFixtures.OFFICER,
              "/api/v1/reports/" + code + "/export?format=PDF",
              reportParams())
          .andExpect(status().isOk())
          .andExpect(content().contentType("application/pdf"));
    }
  }

  @Test
  void aRunIsComputedSubmittedAndApprovedOverHttp() throws Exception {
    fx.paidInvoice();
    fx.costCentreRule();
    LocalDate today = FrbsFixtures.today();
    Map<String, Object> period = Map.of("from", today.toString(), "to", today.toString());
    JsonNode run =
        api.read(
            api.doPost(FrbsFixtures.OFFICER, FEE + "/runs?companyId=" + fx.company(), period)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("COMPUTED")));
    String url = FEE + "/runs/" + run.get("id").asLong();
    api.doPost(FrbsFixtures.OFFICER, url + "/recompute", Map.of()).andExpect(status().isOk());
    api.doPost(FrbsFixtures.OFFICER, url + "/submit", Map.of("comment", "Go"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("FOR_APPROVAL"));
    api.doPost(FrbsFixtures.OFFICER, url + "/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost(FrbsFixtures.LEAD, url + "/approve", Map.of("comment", "OK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("APPROVED"));
    JsonNode lines = api.read(api.doGet(FrbsFixtures.OFFICER, url + "/lines"));
    long lineId = lines.get(0).get("id").asLong();
    api.doPost(
            FrbsFixtures.OFFICER,
            FEE + "/lines/" + lineId + "/release",
            Map.of("releasedOn", today.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RELEASED"));
    api.doPost(FrbsFixtures.OFFICER, FEE + "/lines/" + lineId + "/resend", Map.of())
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithUserDetails(FrbsFixtures.OFFICER)
  void aLiquidationReportIsUploadedOverHttp() throws Exception {
    ServiceFeeRun run = fx.approvedRun();
    ServiceFeeLine line = fx.paidLines(run).get(0);
    fx.pay(run, line);
    MockMultipartFile report =
        new MockMultipartFile(
            "file",
            "liquidation.pdf",
            "application/pdf",
            "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII));
    mvc.perform(
            multipart(FEE + "/lines/" + line.getId() + "/liquidate")
                .file(report)
                .param("liquidatedOn", FrbsFixtures.today().toString())
                .param("remarks", "Received from the unit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIQUIDATED"))
        .andExpect(jsonPath("$.tags.liquidationReportId").exists());
  }

  @Test
  void setupSchedulesAndCertificatesOverHttp() throws Exception {
    String unit = "HU" + System.nanoTime() % 1_000_000;
    JsonNode recipient =
        api.read(
            api.doPost(
                    FrbsFixtures.LEAD,
                    FEE + "/recipients?companyId=" + fx.company(),
                    Map.of(
                        "salesUnit",
                        unit,
                        "payeeCode",
                        "BR-1",
                        "payeeName",
                        "Branch",
                        "active",
                        true))
                .andExpect(status().isOk()));
    api.doPut(
            FrbsFixtures.LEAD,
            FEE + "/recipients/" + recipient.get("id").asLong(),
            Map.of("payeeCode", "BR-2", "payeeName", "Branch 2", "active", true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payeeCode").value("BR-2"));
    Map<String, Object> rule = new HashMap<>();
    rule.put("segment", "OTHERS");
    rule.put("marketSegments", List.of("INSTITUTIONAL"));
    rule.put("rate", 0.5);
    rule.put("netOfWtax", true);
    rule.put("effectiveFrom", "2019-01-01");
    rule.put("effectiveTo", "2019-12-31");
    rule.put("active", false);
    JsonNode created =
        api.read(api.doPost(FrbsFixtures.LEAD, FEE + "/rules", rule).andExpect(status().isOk()));
    api.doPut(FrbsFixtures.LEAD, FEE + "/rules/" + created.get("id").asLong(), rule)
        .andExpect(status().isOk());
    api.doPost(FrbsFixtures.OFFICER, FEE + "/rules", rule).andExpect(status().isForbidden());

    Map<String, Object> comment =
        Map.of(
            "companyId",
            fx.company(),
            "period",
            YearMonth.now().toString(),
            "rowKey",
            "4101",
            "text",
            "Seasonal");
    api.doPut(FrbsFixtures.OFFICER, SCHEDULES + "/GARD-VARIANCE-SIE/comments", comment)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("Seasonal"));
    Map<String, Object> removal = new HashMap<>(comment);
    removal.put("text", "");
    api.doPut(FrbsFixtures.OFFICER, SCHEDULES + "/GARD-VARIANCE-SIE/comments", removal)
        .andExpect(status().isNoContent());
    Map<String, Object> def = new HashMap<>();
    def.put("code", "SCH-H" + System.nanoTime() % 1_000_000);
    def.put("name", "HTTP schedule");
    def.put("family", "OTHER");
    def.put("selectorKind", "ACCOUNT_PREFIX");
    def.put("accountSelector", "1220");
    def.put("grouping", "PARTY");
    def.put("side", "DEBIT");
    def.put("basis", "BALANCE");
    def.put("ageingSlots", "30,60");
    def.put("active", true);
    def.put("columns", List.of(Map.of("measure", "CLOSING", "label", "Balance")));
    api.doPost(FrbsFixtures.OFFICER, SCHEDULES, def).andExpect(status().isForbidden());
    api.doPost(FrbsFixtures.LEAD, SCHEDULES, def).andExpect(status().isOk());
    api.doPut(FrbsFixtures.LEAD, SCHEDULES + "/" + def.get("code"), def).andExpect(status().isOk());

    Map<String, Object> cert = new HashMap<>();
    cert.put("certificateNo", "H-" + System.nanoTime() % 1_000_000);
    cert.put("agentCode", "INS-HTTP");
    cert.put("agentName", "HTTP Insurer");
    cert.put("periodFrom", LocalDate.now().withDayOfMonth(1).toString());
    cert.put("periodTo", LocalDate.now().toString());
    cert.put("receivedOn", LocalDate.now().toString());
    cert.put(
        "lines", List.of(Map.of("kind", "COMMISSION", "atc", "WC158", "income", 1000, "tax", 20)));
    JsonNode recorded =
        api.read(
            api.doPost("disb", CERTS + "?companyId=" + fx.company(), cert)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED")));
    api.doGet("disb", CERTS + "/" + recorded.get("id").asLong()).andExpect(status().isOk());
    api.doPost("disb", CERTS + "/" + recorded.get("id").asLong() + "/cancel", Map.of("reason", "x"))
        .andExpect(status().isForbidden());
    api.doPost(
            "fmanager",
            CERTS + "/" + recorded.get("id").asLong() + "/cancel",
            Map.of("reason", "Wrong"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    api.doGet("epol", "/api/v1/frbs/report-pack").andExpect(status().isForbidden());
    api.doGet("epol", FEE + "/runs?companyId=1").andExpect(status().isForbidden());
    api.doGet("epol", SCHEDULES).andExpect(status().isForbidden());
    api.doPost("epol", FEE + "/runs?companyId=1", Map.of("from", "2026-01-01", "to", "2026-01-31"))
        .andExpect(status().isForbidden());
    api.doPost(FrbsFixtures.OFFICER, FEE + "/runs?companyId=1", Map.of())
        .andExpect(status().isBadRequest());
    api.doGet(FrbsFixtures.OFFICER, FEE + "/runs/999999999").andExpect(status().isNotFound());
    api.doGet(FrbsFixtures.OFFICER, SCHEDULES + "/NO-SUCH").andExpect(status().isNotFound());
  }
}
