package com.iortatechnxt.finverse.api;

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
import com.iortatechnxt.finverse.period.domain.AccountingPeriod;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestCompanies;
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

/** Write endpoints of budgets, inter-company, consolidation and closing through the HTTP stack. */
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
  private Long subsidiary;

  @BeforeAll
  void setUp() {
    parent = companies.create("TAPA", "PHP").getId();
    subsidiary = companies.create("TAPB", "USD").getId();
    companies.openYear(parent, 2026);
    companies.openYear(subsidiary, 2026);
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
  void intercompanyConsolidationAndClosingOverHttp() throws Exception {
    Map<String, Object> relationship =
        map(
            "companyAId", parent,
            "aDueFromAccount", "1607",
            "aDueToAccount", "2510",
            "companyBId", subsidiary,
            "bDueFromAccount", "1607",
            "bDueToAccount", "2510");
    JsonNode rel = send(postJson("/api/v1/intercompany/relationships", relationship), MANAGER, 201);
    String relUrl = "/api/v1/intercompany/relationships/" + rel.get("id").asLong();
    send(post(relUrl + "/active?active=true"), MANAGER, 200);
    Map<String, Object> charge =
        map(
            "type", "CHARGE",
            "creditorCompanyId", parent,
            "debtorCompanyId", subsidiary,
            "valueDate", "2026-04-15",
            "currency", "USD",
            "amount", 250,
            "creditorAccount", "4700",
            "debtorAccount", "5605",
            "narration", "Fee",
            "costCenter", "FIN");
    send(postJson("/api/v1/intercompany/transactions", charge), MANAGER, 201);

    Map<String, Object> member =
        map(
            "companyId",
            subsidiary,
            "ownershipPct",
            100,
            "investmentAccount",
            "1506",
            "equityAccounts",
            List.of("3100"));
    Map<String, Object> group =
        map(
            "code", "TAPG",
            "name", "API group",
            "parentCompanyId", parent,
            "currency", "PHP",
            "ctaAccount", "3450",
            "nciAccount", "3600",
            "goodwillAccount", "1850",
            "active", true,
            "members", List.of(member));
    long groupId =
        send(postJson("/api/v1/consolidation/groups", group), MANAGER, 201).get("id").asLong();
    send(putJson("/api/v1/consolidation/groups/" + groupId, group), MANAGER, 200);
    String runs = "/api/v1/consolidation/groups/" + groupId + "/runs?asOf=2026-05-31";
    long runId = send(post(runs), MANAGER, 201).get("id").asLong();
    mvc.perform(get("/api/v1/consolidation/runs/" + runId).with(as("auditor")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trialBalance").isArray());
    send(post("/api/v1/consolidation/runs/" + runId + "/finalize"), MANAGER, 200);

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
