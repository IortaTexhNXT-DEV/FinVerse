package com.iortatechnxt.finverse.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Calls every read endpoint through the full HTTP stack (security, serialization, lazy loading) to
 * catch problems unit and service tests cannot see.
 */
@IntegrationTest
class ApiSmokeIT {

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;
  @Autowired private PeriodService periods;
  @Autowired private ConsolidationGroupService groups;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/v1/auth/me",
        "/api/v1/organization/companies",
        "/api/v1/organization/branches?companyId={c}",
        "/api/v1/organization/holidays?companyId={c}&year=2026",
        "/api/v1/currencies",
        "/api/v1/coa/accounts?companyId={c}",
        "/api/v1/coa/accounts?companyId={c}&q=11",
        "/api/v1/coa/categories",
        "/api/v1/dimensions?companyId={c}&type=COST_CENTER",
        "/api/v1/periods/years?companyId={c}",
        "/api/v1/journals?companyId={c}",
        "/api/v1/journals?companyId={c}&status=POSTED&inputter=accountant",
        "/api/v1/reports",
        "/api/v1/dashboard?companyId={c}",
        "/api/v1/audit-logs?from=2026-01-01&to=2026-12-31",
        "/api/v1/parties?companyId={c}",
        "/api/v1/parties?companyId={c}&types=BROKER&types=AGENT&q=pa",
        "/api/v1/subledger/items?companyId={c}&partyCode=C-000201",
        "/api/v1/subledger/outstanding?companyId={c}&asOf=2026-12-31",
        "/api/v1/accounting/event-types",
        "/api/v1/accounting/rules?companyId={c}",
        "/api/v1/accounting/events?companyId={c}&from=2026-01-01&to=2026-12-31",
        "/api/v1/budgets?companyId={c}",
        "/api/v1/budgets?companyId={c}&fiscalYear=2026",
        "/api/v1/budgets/variance?companyId={c}&asOf=2026-06-30&byCostCenter=true",
        "/api/v1/budgets/alerts?companyId={c}&asOf=2026-06-30&threshold=50",
        "/api/v1/intercompany/relationships",
        "/api/v1/intercompany/relationships?companyId={c}",
        "/api/v1/intercompany/transactions?companyId={c}",
        "/api/v1/intercompany/reconciliation?companyId={c}&asOf=2026-06-30",
        "/api/v1/consolidation/groups",
        "/api/v1/closing/fx-revaluations?companyId={c}",
      })
  @WithUserDetails("fmanager")
  void readEndpointsRespondOk(String url) throws Exception {
    mvc.perform(get(url.replace("{c}", data.company().getId().toString())))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("fmanager")
  void currencyRatesAndStatementRespondOk() throws Exception {
    String today = LocalDate.now().toString();
    mvc.perform(get("/api/v1/currencies/rates?from=2026-01-01&to=" + today))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/currencies/rates/effective?baseCurrency=PHP&currency=USD&date=2026-06-30"))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("fmanager")
  void planningAndClosingEndpointsRespondOk() throws Exception {
    Long company = data.company().getId();
    var year = periods.yearContaining(company, LocalDate.of(2026, 6, 30));
    Long june =
        periods.listPeriods(year.getId()).stream()
            .filter(p -> p.getName().equals("2026-06"))
            .findFirst()
            .orElseThrow()
            .getId();
    String c = "?companyId=" + company;
    mvc.perform(get("/api/v1/closing/period-end/checklist" + c + "&periodId=" + june))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
    mvc.perform(get("/api/v1/closing/year-end/checklist" + c + "&fiscalYearId=" + year.getId()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/closing/year-end/preview" + c + "&fiscalYearId=" + year.getId()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/closing/year-end/close?fiscalYearId=" + year.getId()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/closing/fx-revaluations/preview" + c + "&periodId=" + june))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodName").value("2026-06"));
    Long group = groups.getByCode("FVGRP").getId();
    mvc.perform(get("/api/v1/consolidation/groups/" + group + "/runs")).andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("uw")
  void missingPermissionIsForbiddenNotUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/admin/users"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void anonymousRequestIsUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/organization/companies")).andExpect(status().isUnauthorized());
  }

  @Test
  void loginReturnsTokenAndWrongPasswordIsRejected() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"auditor\",\"password\":\"Finverse@2026\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.roles[0]").value("AUDITOR"));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"auditor\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
  }

  @Test
  @WithUserDetails("fmanager")
  void validationErrorsListFields() throws Exception {
    mvc.perform(post("/api/v1/journals").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors.companyId").exists());
  }
}
