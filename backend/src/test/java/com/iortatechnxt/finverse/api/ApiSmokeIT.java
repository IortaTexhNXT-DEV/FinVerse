package com.iortatechnxt.finverse.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
