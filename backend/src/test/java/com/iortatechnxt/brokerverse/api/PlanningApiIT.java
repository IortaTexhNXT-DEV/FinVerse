package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Write endpoints of budgets and closing through the HTTP stack. */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanningApiIT {

  private static final String MANAGER = "fmanager";
  private static final String ACCOUNTANT = "accountant";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private UserDetailsService users;
  @Autowired private TestCompanies companies;

  private Long parent;

  @BeforeAll
  void setUp() {
    parent = companies.create("TAPA", "PHP").getId();
    companies.openYear(parent, 2026);
  }

  private RequestPostProcessor as(String username) {
    return user(users.loadUserByUsername(username));
  }

  private JsonNode send(MockHttpServletRequestBuilder request, String username, int expected)
      throws Exception {
    String body =
        mvc.perform(request.with(as(username)).with(csrf()))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return body.isEmpty() ? null : json.readTree(body);
  }

  private MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
    return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
  }

  private MockHttpServletRequestBuilder putJson(String url, Object body) throws Exception {
    return put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
  }

  private static Map<String, Object> map(Object... keyValues) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      m.put((String) keyValues[i], keyValues[i + 1]);
    }
    return m;
  }

  @Test
  void budgetLifecycleOverHttp() throws Exception {
    Map<String, Object> create =
        map("companyId", parent, "fiscalYear", 2026, "versionType", "ORIGINAL", "name", "API");
    long id = send(postJson("/api/v1/budgets", create), MANAGER, 201).get("id").asLong();
    String base = "/api/v1/budgets/" + id;
    List<Integer> months = List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    send(
        putJson(
            base + "/lines",
            List.of(map("accountCode", "5601", "costCenter", "FIN", "months", months))),
        MANAGER,
        200);
    send(
        post(base + "/import")
            .contentType(MediaType.TEXT_PLAIN)
            .content("account_code,cost_centre,annual\n5603,FIN,1200\n"),
        MANAGER,
        200);
    // The guided Excel template, filled in below its example row, is imported as it is.
    byte[] template =
        mvc.perform(get("/api/v1/budgets/template").with(as(MANAGER)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    byte[] filled;
    try (var wb =
            new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(template));
        var out = new java.io.ByteArrayOutputStream()) {
      var sheet = wb.getSheet("Budget lines");
      var row = sheet.createRow(sheet.getLastRowNum() + 1);
      row.createCell(1).setCellValue("5603");
      row.createCell(2).setCellValue("FIN");
      row.createCell(15).setCellValue(2400d);
      wb.write(out);
      filled = out.toByteArray();
    }
    JsonNode imported =
        send(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                    base + "/import-file")
                .file(
                    new org.springframework.mock.web.MockMultipartFile(
                        "file", "budget.xlsx", "application/octet-stream", filled)),
            MANAGER,
            200);
    assertThat(imported.get("lines")).hasSize(1);
    assertThat(imported.get("lines").get(0).get("months").get(0).decimalValue())
        .isEqualByComparingTo("200");
    send(
        postJson(base + "/copy-actuals", map("sourceYear", 2025, "adjustmentPercent", 5)),
        MANAGER,
        422);
    send(post(base + "/submit"), MANAGER, 200);
    send(postJson(base + "/reject", map("reason", "Rework")), ACCOUNTANT, 200);
    send(post(base + "/submit"), MANAGER, 200);
    send(post(base + "/approve"), "checker", 403);
    JsonNode approved = send(post(base + "/approve"), ACCOUNTANT, 200);
    assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
    mvc.perform(get(base).with(as("auditor")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines[0].accountCode").value("5603"));
  }

  @Test
  void closingOverHttp() throws Exception {
    AccountingPeriod april = companies.period(parent, LocalDate.of(2026, 4, 30));
    Map<String, Object> revaluation =
        map("companyId", parent, "periodId", april.getId(), "autoReverse", false);
    JsonNode reval = send(postJson("/api/v1/closing/fx-revaluations", revaluation), MANAGER, 200);
    mvc.perform(
            get("/api/v1/closing/fx-revaluations/" + reval.get("id").asLong()).with(as(MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodName").value("2026-04"));

    Map<String, Object> close =
        map("companyId", parent, "fiscalYearId", april.getFiscalYear().getId());
    send(postJson("/api/v1/closing/year-end/close", close), MANAGER, 422);
    send(postJson("/api/v1/closing/year-end/close", close), ACCOUNTANT, 403);
  }
}
