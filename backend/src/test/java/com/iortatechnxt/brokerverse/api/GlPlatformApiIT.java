package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** GL platform (BRD-5 wave A1-GL) endpoints through the full HTTP stack. */
@IntegrationTest
class GlPlatformApiIT {

  private static final Path SAMPLE = Path.of("..", "docs", "samples", "coa_upload_sample.xlsx");

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;
  @Autowired private TestCompanies companies;

  @ParameterizedTest
  @CsvSource({
    "gltl, /api/v1/coa/numbering?companyId={c}",
    "gltl, /api/v1/coa/accounts/lookup?companyId={c}&key=1111",
    "gltl, /api/v1/coa/accounts?companyId={c}&q=11",
    "gltl, /api/v1/coa/uploads?companyId={c}",
    "gltl, /api/v1/coa/uploads/template",
    "gltl, /api/v1/journals/assignees",
    "gltl, /api/v1/journals?companyId={c}&assignedTo=gltl",
    "gltl, /api/v1/accounting/cost-center-rules?companyId={c}",
    "fmanager, /api/v1/organization/employees?companyId={c}&q=a",
    "gltl, /api/v1/organization/employees?companyId={c}",
    "gltl, /api/v1/closing/settings",
    "gltl, /api/v1/closing/close-schedules?companyId={c}",
    "gltl, /api/v1/closing/broking-books?companyId={c}",
    "gltl, /api/v1/currencies/revaluation-rates?year=2026",
    "gltl, /api/v1/reports/batches",
    "fmanager, /api/v1/receivables/bank-rec/layouts?companyId={c}",
  })
  void readEndpointsRespondOk(String user, String url) throws Exception {
    api.doGet(user, url.replace("{c}", data.company().getId().toString()))
        .andExpect(status().isOk());
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    Long c = data.company().getId();
    api.doGet("glofficer", "/api/v1/coa/uploads?companyId=" + c).andExpect(status().isForbidden());
    api.doGet("glofficer", "/api/v1/journals/assignees").andExpect(status().isForbidden());
    api.doPost(
            "glofficer",
            "/api/v1/currencies/revaluation-rates",
            Map.of("currencyCode", "USD", "month", "2026-08", "rate", "58.1"))
        .andExpect(status().isForbidden());
    api.doPost("glofficer", "/api/v1/closing/close-now", Map.of("companyId", c, "periodId", 1))
        .andExpect(status().isForbidden());
    api.doGet("ao", "/api/v1/closing/close-schedules?companyId=" + c)
        .andExpect(status().isForbidden());
  }

  @Test
  void chartUploadDemoFileLoadsPendingAuthorization() throws Exception {
    Long company = companies.create("TGLU", "PHP").getId();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/coa/uploads/template")
                .with(user(users.loadUserByUsername("gltl"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", Matchers.containsString("coa_accounts")));
    MockMultipartFile file =
        new MockMultipartFile("file", "coa_upload_sample.xlsx", null, Files.readAllBytes(SAMPLE));
    JsonNode job = upload(company, file, 8, 0);
    long id = job.get("id").asLong();
    api.doGet("gltl", "/api/v1/coa/uploads/" + id + "/rows")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(8));
    api.doPost("gltl", "/api/v1/coa/uploads/" + id + "/commit", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.committedRows").value(8))
        .andExpect(jsonPath("$.failedRows").value(0));
    api.doGet("gltl", "/api/v1/coa/uploads/" + id + "/report").andExpect(status().isOk());

    api.doGet("gltl", "/api/v1/coa/accounts/lookup?companyId=" + company + "&key=prusdbp")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("1913.01"))
        .andExpect(jsonPath("$.parentCode").value("1913"))
        .andExpect(jsonPath("$.negativeBalancePolicy").value("WARN"))
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doGet("gltl", "/api/v1/coa/accounts/lookup?companyId=" + company + "&key=1913")
        .andExpect(jsonPath("$.postable").value(false));
    api.doGet("gltl", "/api/v1/coa/accounts?companyId=" + company + "&q=DTIPUSD")
        .andExpect(jsonPath("$[0].code").value("2912"));

    // The same file again: every account exists now.
    JsonNode again = upload(company, file, 0, 8);
    api.doPost("gltl", "/api/v1/coa/uploads/" + again.get("id").asLong() + "/cancel", null)
        .andExpect(status().isOk());
    api.doGet("fmanager", "/api/v1/coa/uploads/" + id + "/rows").andExpect(status().isForbidden());
  }

  @Test
  void numberingProposesTheNextChildCode() throws Exception {
    Long company = companies.create("TGLN", "PHP").getId();
    api.doPut(
            "gltl",
            "/api/v1/coa/numbering",
            Map.of(
                "companyId",
                company,
                "parentCode",
                "1210",
                "separator",
                ".",
                "width",
                2,
                "active",
                true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.example").value("1210.01"));
    String next =
        api.read(
                api.doGet(
                    "gltl",
                    "/api/v1/coa/accounts/next-code?companyId=" + company + "&parentCode=1210"))
            .get("code")
            .asText();
    assertThat(next).startsWith("1210.").hasSize(7).isNotEqualTo("1210.01");

    Map<String, Object> account =
        Map.ofEntries(
            Map.entry("companyId", company),
            Map.entry("code", ""),
            Map.entry("name", "Premium Receivable - Motor Mania"),
            Map.entry("shortName", "PRMM"),
            Map.entry("accountClass", "ASSET"),
            Map.entry("level", "SUB"),
            Map.entry("parentCode", "1210"),
            Map.entry("allowManualPosting", true),
            Map.entry("openedOn", "2026-01-01"),
            Map.entry("negativeBalancePolicy", "BLOCK"));
    api.doPost("gltl", "/api/v1/coa/accounts", account)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(next))
        .andExpect(jsonPath("$.negativeBalancePolicy").value("BLOCK"));
    Map<String, Object> duplicateShort = new java.util.HashMap<>(account);
    duplicateShort.put("code", "1210.99");
    api.doPost("gltl", "/api/v1/coa/accounts", duplicateShort)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("SHORT_CODE_TAKEN"));
    api.doGet("gltl", "/api/v1/coa/numbering?companyId=" + company)
        .andExpect(jsonPath("$[0].parentCode").value("1210"));
  }

  @Test
  void revaluationRateIsTheMonthEndClosingRateAndBecomesTheNextBookRate() throws Exception {
    YearMonth month = YearMonth.of(2031, 3);
    api.doPost(
            "glhead",
            "/api/v1/currencies/revaluation-rates",
            Map.of("currencyCode", "USD", "month", month.toString(), "rate", "58.12345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rateType").value("CLOSING"))
        .andExpect(jsonPath("$.effectiveDate").value("2031-03-31"));
    api.doGet("gltl", "/api/v1/currencies/revaluation-rates?year=2031")
        .andExpect(jsonPath("$[0].effectiveDate").value("2031-03-31"));
    api.doPost("glhead", "/api/v1/currencies/revaluation-rates/2031-03/copy-to-book", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rateType").value("BOOK"))
        .andExpect(jsonPath("$[0].effectiveDate").value("2031-04-01"))
        .andExpect(jsonPath("$[0].rate").value(58.12));
    api.doPost("glhead", "/api/v1/currencies/revaluation-rates/2031-03/copy-to-book", null)
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void costCentreRulesAndEmployeesAreMaintained() throws Exception {
    Long c = data.company().getId();
    JsonNode rule =
        api.read(
            api.doPost(
                    "fmanager",
                    "/api/v1/accounting/cost-center-rules",
                    Map.of(
                        "companyId",
                        c,
                        "priority",
                        9000,
                        "sourceModule",
                        "APITEST",
                        "costCenter",
                        "FIN",
                        "active",
                        false))
                .andExpect(status().isCreated()));
    api.doPut(
            "fmanager",
            "/api/v1/accounting/cost-center-rules/" + rule.get("id").asLong(),
            Map.of("companyId", c, "priority", 9001, "costCenter", "FIN", "active", false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.priority").value(9001));
    api.doPost(
            "fmanager",
            "/api/v1/accounting/cost-center-rules",
            Map.of("companyId", c, "priority", 1, "costCenter", "NOPE", "active", false))
        .andExpect(status().is4xxClientError());

    String no = "E" + (System.nanoTime() % 1_000_000);
    Map<String, Object> employee =
        Map.of(
            "companyId",
            c,
            "employeeNo",
            no,
            "fullName",
            "Api Employee",
            "branchId",
            data.branch("HO").getId(),
            "costCenter",
            "FIN",
            "hiredOn",
            "2024-01-15");
    JsonNode created =
        api.read(
            api.doPost("fmanager", "/api/v1/organization/employees", employee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true)));
    Map<String, Object> separated = new java.util.HashMap<>(employee);
    separated.put("separatedOn", "2026-06-30");
    api.doPut("fmanager", "/api/v1/organization/employees/" + created.get("id").asLong(), separated)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    api.doPost("fmanager", "/api/v1/organization/employees", employee)
        .andExpect(status().isConflict());
    api.doPost("gltl", "/api/v1/organization/employees", employee)
        .andExpect(status().isForbidden());
  }

  @Test
  void reportBatchZipsTheReportsAndRecordsFailures() throws Exception {
    Long c = data.company().getId();
    JsonNode batch =
        api.read(
            api.doPost(
                    "fmanager",
                    "/api/v1/reports/batches",
                    Map.of(
                        "codes", List.of("GL-COA", "ORG-HEADCOUNT-CC", "NO-SUCH-REPORT"),
                        "parameters", Map.of("companyId", c.toString()),
                        "format", "CSV"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.items[0].status").value("OK"))
                .andExpect(jsonPath("$.items[2].status").value("FAILED")));
    long id = batch.get("id").asLong();
    api.doGet("fmanager", "/api/v1/reports/batches/" + id + "/file")
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/zip"));
    api.doGet("fmanager", "/api/v1/reports/batches/" + id).andExpect(status().isOk());
    api.doGet("gltl", "/api/v1/reports/batches/" + id).andExpect(status().isForbidden());

    api.doPost(
            "gltl",
            "/api/v1/reports/batches",
            Map.of(
                "codes",
                List.of("GL-COA", "GL-COA"),
                "parameters",
                Map.of("companyId", c.toString()),
                "mergedPdf",
                true,
                "paper",
                "LETTER",
                "orientation",
                "LANDSCAPE",
                "fitToWidth",
                false))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.fileName").value(Matchers.endsWith(".pdf")))
        .andExpect(jsonPath("$.paper").value("LETTER"));
  }

  @Test
  void exportAppliesPrintOptionsAndColumnFilters() throws Exception {
    String c = data.company().getId().toString();
    String csv =
        api.doPost(
                "fmanager",
                "/api/v1/reports/GL-COA/export?format=CSV&filter=code:1111",
                Map.of("companyId", c))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(csv).contains("1111").doesNotContain("4100");
    api.doPost(
            "fmanager",
            "/api/v1/reports/GL-COA/export?format=PDF&paper=A3&orientation=PORTRAIT&fitToWidth=false",
            Map.of("companyId", c))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"));
  }

  @Test
  void headcountPerCostCentreRunsForTheGlTeam() throws Exception {
    api.doPost(
            "gltl",
            "/api/v1/reports/ORG-HEADCOUNT-CC/run",
            Map.of("companyId", data.company().getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("ORG-HEADCOUNT-CC"));
    api.doPost(
            "gltl",
            "/api/v1/reports/ORG-HEADCOUNT-CC/export?format=XLSX",
            Map.of("companyId", data.company().getId().toString()))
        .andExpect(status().isOk());
  }

  @Test
  void closingControlsOverHttp() throws Exception {
    Long company = companies.create("TGLH", "PHP").getId();
    companies.openYear(company, 2030);
    Long march = companies.period(company, LocalDate.of(2030, 3, 15)).getId();
    api.doGet(
            "gltl",
            "/api/v1/closing/close-schedules/proposal?companyId=" + company + "&periodId=" + march)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduledAt").value(Matchers.startsWith("2030-04-02T09:00")));
    JsonNode schedule =
        api.read(
            api.doPost(
                    "gltl",
                    "/api/v1/closing/close-schedules",
                    Map.of("companyId", company, "periodId", march))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.periodName").value("2030-03")));
    api.doPost(
            "gltl",
            "/api/v1/closing/close-schedules/" + schedule.get("id").asLong() + "/cancel",
            Map.of("reason", "Moved to the 3rd banking day"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    api.doPost(
            "gltl",
            "/api/v1/closing/close-schedules",
            Map.of("companyId", company, "periodId", march, "scheduledAt", "2030-06-02T09:00:00Z"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLOSE_ONLY_PREVIOUS_MONTH"));

    api.doGet(
            "gltl",
            "/api/v1/closing/broking-books/pending?companyId=" + company + "&periodId=" + march)
        .andExpect(status().isOk());
    api.doPost(
            "gltl",
            "/api/v1/closing/broking-books/close",
            Map.of("companyId", company, "periodId", march))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locked").value(true))
        .andExpect(jsonPath("$.note").value("Nothing pending"));
    api.doPost(
            "gltl",
            "/api/v1/closing/broking-books/reopen",
            Map.of("companyId", company, "periodId", march))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(
            "gltl",
            "/api/v1/closing/broking-books/reopen",
            Map.of("companyId", company, "periodId", march, "reason", "Late booking"))
        .andExpect(jsonPath("$.locked").value(false));
    api.doGet("gltl", "/api/v1/closing/broking-books?companyId=" + company)
        .andExpect(jsonPath("$[0].periodName").value("2030-03"));
  }

  private JsonNode upload(Long company, MockMultipartFile file, int valid, int invalid)
      throws Exception {
    String body =
        mvc.perform(
                multipart("/api/v1/coa/uploads")
                    .file(file)
                    .param("companyId", company.toString())
                    .with(user(users.loadUserByUsername("gltl"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.validRows").value(valid))
            .andExpect(jsonPath("$.invalidRows").value(invalid))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
  }
}
