package com.iortatechnxt.finverse.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
        "/api/v1/accounting/events?companyId={c}&from=2026-01-01&to=2026-12-31&status=FAILED",
        "/api/v1/subledger/ageing?companyId={c}&asOf=2026-12-31",
        "/api/v1/subledger/ageing?companyId={c}&asOf=2026-12-31&partyCode=C-000201",
        "/api/v1/journals?companyId={c}&batchNo=JV-HO-2026",
        "/api/v1/approvals/inbox",
        "/api/v1/approvals/inbox?companyId={c}",
        "/api/v1/approvals/counts",
        "/api/v1/attachments?entityType=JournalBatch&entityId=1",
        "/api/v1/attachments/policy",
        "/api/v1/journals/recurring?companyId={c}",
        "/api/v1/alerts",
        "/api/v1/alerts?status=OPEN&severity=HIGH&companyId={c}&from=2026-01-01&to=2026-12-31",
        "/api/v1/alerts/summary",
        "/api/v1/alerts/exception-codes",
        "/api/v1/system/parameters",
        "/api/v1/system/configuration",
        "/api/v1/system/info",
        "/api/v1/system/about",
        "/api/v1/system/session-policy",
        "/api/v1/system/jobs",
        "/api/v1/system/jobs/runs",
        // payables (payables, payments, petty cash, PDC issued)
        "/api/v1/payables/bank-accounts?companyId={c}",
        "/api/v1/payables/bank-accounts?companyId={c}&activeOnly=true",
        "/api/v1/payables/invoices?companyId={c}",
        "/api/v1/payables/invoices?companyId={c}&status=APPROVED&partyCode=S-0001",
        "/api/v1/payables/vouchers?companyId={c}&from=2026-01-01&to=2026-12-31",
        "/api/v1/payables/vouchers/payable-items?companyId={c}&partyCode=S-0001",
        "/api/v1/payables/pdc-issued?companyId={c}",
        "/api/v1/payables/petty-cash/funds?companyId={c}",
        "/api/v1/underwriting/products?companyId={c}",
        "/api/v1/underwriting/policies?companyId={c}",
        "/api/v1/underwriting/policies?companyId={c}&status=APPROVED&q=P-&fromDate=2026-01-01",
        "/api/v1/underwriting/quotations?companyId={c}",
        "/api/v1/underwriting/quotations?companyId={c}&status=PENDING_APPROVAL",
        "/api/v1/underwriting/open-covers?companyId={c}",
        "/api/v1/assets/categories?companyId={c}",
        "/api/v1/assets/register?companyId={c}",
        "/api/v1/assets/register?companyId={c}&status=ACTIVE&q=fa",
        "/api/v1/assets/depreciation/preview?companyId={c}&period=2026-09",
        "/api/v1/assets/depreciation/runs?companyId={c}",
        "/api/v1/investments/portfolios?companyId={c}",
        "/api/v1/investments/holdings?companyId={c}",
        "/api/v1/investments/holdings?companyId={c}&status=ACTIVE&q=t",
        "/api/v1/investments/runs?companyId={c}",
        "/api/v1/investments/runs/preview?companyId={c}&type=ACCRUAL&period=2026-09",
        "/api/v1/investments/runs/preview?companyId={c}&type=AMORTIZATION&period=2026-09",
        "/api/v1/receivables/receipts?companyId={c}",
        "/api/v1/receivables/receipts?companyId={c}&status=APPROVED&mode=CHEQUE&from=2026-01-01",
        "/api/v1/receivables/bank-accounts?companyId={c}",
        "/api/v1/receivables/open-items?companyId={c}&partyCode=C-000201",
        "/api/v1/receivables/party-statement?companyId={c}&partyCode=C-000201&from=2026-01-01&to=2026-12-31",
        "/api/v1/receivables/ageing-slots",
        "/api/v1/receivables/deposits/undeposited?companyId={c}",
        "/api/v1/receivables/deposits/slips?companyId={c}",
        "/api/v1/receivables/pdcs?companyId={c}",
        "/api/v1/receivables/pdcs?companyId={c}&status=ON_HAND",
        "/api/v1/receivables/bank-rec/statements?companyId={c}",
        "/api/v1/receivables/bank-rec/workbench?companyId={c}&bankAccountCode=1111&asOf=2026-09-30",
        "/api/v1/receivables/bank-rec/brs?companyId={c}&bankAccountCode=1111&asOf=2026-09-30",
        "/api/v1/receivables/bank-rec/matches?companyId={c}&bankAccountCode=1111",
        "/api/v1/receivables/bank-rec/reconciliations?companyId={c}",
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/v1/receivables/receipts?companyId={c}",
        "/api/v1/receivables/deposits/slips?companyId={c}",
        "/api/v1/receivables/pdcs?companyId={c}",
        "/api/v1/payables/invoices?companyId={c}",
      })
  @WithUserDetails("auditor")
  void readOnlyUsersCanInquireReceiptsAndPayments(String url) throws Exception {
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

  private String bearerToken(String username) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"Finverse@2026\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  @Test
  void bearerRequestsAreExemptFromCsrfAndValidationErrorsListFields() throws Exception {
    mvc.perform(
            post("/api/v1/journals")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken("fmanager"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors.companyId").exists());
  }

  @Test
  @WithUserDetails("fmanager")
  void stateChangingRequestWithoutBearerTokenNeedsCsrfToken() throws Exception {
    mvc.perform(post("/api/v1/journals").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }
}
