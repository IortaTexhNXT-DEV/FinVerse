package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
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
  @Autowired private UserDetailsService users;

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
        "/api/v1/dashboard/premium?companyId={c}",
        "/api/v1/dashboard/claims?companyId={c}&asOf=2026-06-30",
        "/api/v1/dashboard/collections?companyId={c}",
        "/api/v1/dashboard/payables?companyId={c}",
        "/api/v1/dashboard/cash?companyId={c}",
        "/api/v1/dashboard/budget?companyId={c}",
        "/api/v1/dashboard/workload?companyId={c}",
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
        "/api/v1/claims?companyId={c}",
        "/api/v1/claims?companyId={c}&status=OPEN&businessLine=FIRE&q=CL-&lossFrom=2026-01-01",
        "/api/v1/claims/lpos?companyId={c}",
        "/api/v1/claims/0/reserves",
        "/api/v1/claims/0/settlements",
        "/api/v1/claims/0/recoveries",
        "/api/v1/claims/0/lpos",
        "/api/v1/claims/0/movements",
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
        // tax & statutory
        "/api/v1/tax/codes?companyId={c}",
        "/api/v1/tax/forms?companyId={c}",
        "/api/v1/tax/profiles?companyId={c}",
        "/api/v1/tax/calendar?companyId={c}&year=2026",
        "/api/v1/tax/worksheets/VAT?companyId={c}&from=2026-07-01&to=2026-09-30",
        "/api/v1/tax/worksheets/EWT?companyId={c}&from=2026-09-01&to=2026-09-30",
        "/api/v1/tax/worksheets/DST?companyId={c}&from=2026-09-01&to=2026-09-30",
        "/api/v1/tax/worksheets/PREMIUM_TAX?companyId={c}&from=2026-07-01&to=2026-09-30",
        "/api/v1/tax/worksheets/LGT?companyId={c}&from=2026-07-01&to=2026-09-30",
        "/api/v1/tax/worksheets/FST?companyId={c}&from=2026-09-01&to=2026-09-30",
        "/api/v1/tax/returns?companyId={c}&year=2026",
        "/api/v1/tax/returns?companyId={c}&year=2026&formCode=2550Q&status=DRAFT",
        "/api/v1/tax/2307/certificates?companyId={c}&year=2026",
        "/api/v1/tax/2307/batches?companyId={c}",
        "/api/v1/tax/exports/SLS?companyId={c}&year=2026&quarter=3",
        "/api/v1/tax/exports/SLP?companyId={c}&year=2026&quarter=3",
        "/api/v1/tax/exports/QAP?companyId={c}&year=2026&quarter=3",
        "/api/v1/tax/ic/mappings?companyId={c}",
        "/api/v1/tax/ic/schedules/PREMIUMS?companyId={c}&from=2026-01-01&to=2026-09-30",
        "/api/v1/tax/ic/schedules/NET_WORTH?companyId={c}&from=2026-01-01&to=2026-09-30",
        "/api/v1/tax/ic/schedules/RBC?companyId={c}&from=2026-01-01&to=2026-09-30",
        "/api/v1/reserves/parameters?companyId={c}",
        "/api/v1/reserves/takaful-setting?companyId={c}",
        "/api/v1/reserves/runs?companyId={c}",
        "/api/v1/reserves/summary?companyId={c}&asOf=2026-08-31",
        "/api/v1/reserves/triangles?companyId={c}&businessLine=FIRE&asOf=2026-08-31",
        // reinsurance
        "/api/v1/reinsurance/treaties?companyId={c}",
        "/api/v1/reinsurance/fac-placements?companyId={c}",
        "/api/v1/reinsurance/fac-placements?companyId={c}&status=PROVISIONAL",
        "/api/v1/reinsurance/allocation/preview?companyId={c}&from=2026-09-01&to=2026-09-30",
        "/api/v1/reinsurance/cessions?companyId={c}&policyNo=NONE",
        "/api/v1/reinsurance/cessions?companyId={c}&from=2026-09-01&to=2026-09-30",
        "/api/v1/reinsurance/claims/movements?companyId={c}&from=2026-01-01&to=2026-12-31",
        "/api/v1/reinsurance/soas?companyId={c}",
      })
  @WithUserDetails("fmanager")
  void readEndpointsRespondOk(String url) throws Exception {
    mvc.perform(get(url.replace("{c}", data.company().getId().toString())))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // platform cache and integration events (System Administrator)
        "/api/v1/admin/caches",
        "/api/v1/admin/events/topics",
        "/api/v1/admin/events/outbox",
        "/api/v1/admin/events/outbox?status=FAILED&topic=bibs.booking.invoice-booked.v1&key=X",
        "/api/v1/admin/events/archive?correlationId=none",
        "/api/v1/admin/events/dead-letters",
        "/api/v1/admin/events/dead-letters?status=ALL&page=1&size=500",
        // session log and own password status (BRD-11 U1-B, UAM-NFR-35 / 36)
        "/api/v1/admin/sessions",
        "/api/v1/admin/sessions?username=admin&open=true",
        "/api/v1/admin/sessions/online",
        "/api/v1/auth/sessions",
        "/api/v1/auth/password-status",
      })
  @WithUserDetails("admin")
  void platformSupportEndpointsRespondOk(String url) throws Exception {
    mvc.perform(get(url)).andExpect(status().isOk());
  }

  /**
   * The list reads of Sanction Screening (BRD-10) and User Access Maintenance (BRD-11), each as the
   * persona whose screen makes it (waves S2 / U2); the record reads are in the module API tests.
   */
  @ParameterizedTest
  @CsvSource({
    "compoff, /api/v1/screening/cases?companyId={c}",
    "compoff, /api/v1/screening/cases?companyId={c}&tab=STR&sla=BREACHED&page=0&size=10",
    "ucc, /api/v1/screening/cases?companyId={c}&tab=TEAM",
    "investigator, /api/v1/screening/cases?companyId={c}&tab=MY",
    "scrapprover, /api/v1/screening/cases?companyId={c}&tab=APPROVAL",
    "amlcom1, /api/v1/screening/cases?companyId={c}&tab=COMMITTEE",
    "compoff, /api/v1/screening/cases/tiles?companyId={c}",
    "compoff, /api/v1/screening/matches?companyId={c}",
    "compoff, /api/v1/screening/matches?companyId={c}&status=TRUE_MATCH&uncased=true",
    "compoff, /api/v1/screening/runs?companyId={c}",
    "compoff, /api/v1/screening/runs?companyId={c}&trigger=PERIODIC",
    "compoff, /api/v1/screening/high-risk-clients?companyId={c}",
    "compoff, /api/v1/screening/str?companyId={c}",
    "compoff, /api/v1/screening/str?companyId={c}&status=APPROVED",
    "auditor, /api/v1/screening/str?companyId={c}",
    "compoff, /api/v1/screening/str/extractions?companyId={c}",
    "compoff, /api/v1/screening/config/versions?companyId={c}&type=MATCH_CRITERIA",
    "compchk, /api/v1/screening/config/versions?companyId={c}&type=TEMPLATE",
    "compoff, /api/v1/screening/watchlist/entries",
    "compchk, /api/v1/screening/watchlist/changes?status=PENDING",
    "compoff, /api/v1/screening/watchlist/sources",
    "compoff, /api/v1/screening/watchlist/template",
    "compoff, /api/v1/screening/watchlist/runs",
    "compoff, /api/v1/screening/watchlist/runs?source=AML_ADVISORY",
    "requestor, /api/v1/nbadmin/access-requests",
    "requestor, /api/v1/nbadmin/access-requests?scope=MINE&status=DRAFT",
    "uamapprover, /api/v1/nbadmin/access-requests?scope=ASSIGNED",
    "secapprover, /api/v1/nbadmin/access-requests?scope=SECOND",
    "admin, /api/v1/nbadmin/access-requests?scope=IMPLEMENTATION&groupProfiles=true",
    "requestor, /api/v1/nbadmin/approvers",
    "badmin, /api/v1/nbadmin/approvers?userType=INTERNAL&subject=a013000101",
    "requestor, /api/v1/nbadmin/access-settings",
    "requestor, /api/v1/nbadmin/access-batches",
    "uamapprover, /api/v1/nbadmin/users",
    "uamapprover, /api/v1/nbadmin/roles",
    "uamapprover, /api/v1/nbadmin/access-matrix",
    "uamapprover, /api/v1/nbadmin/access-matrix/by-action",
  })
  void screeningAndUserAccessListsRespondOk(String user, String url) throws Exception {
    mvc.perform(
            get(url.replace("{c}", data.company().getId().toString()))
                .with(user(users.loadUserByUsername(user))))
        .andExpect(status().isOk());
  }

  /**
   * The list reads of Claims Handling (BRD-7), each as the persona whose screen makes it (wave
   * CL2); the record reads of one claim are in {@code ClaimsHandlingApiIT} and {@code
   * ClaimsStatusReportsApiIT}.
   */
  @ParameterizedTest
  @CsvSource({
    "clmofficer, /api/v1/broker-claims/home?companyId={c}",
    "clmofficer, /api/v1/broker-claims/worklist?companyId={c}",
    "clmofficer, /api/v1/broker-claims/worklist?companyId={c}&tab=MINE&q=BCL",
    "clmofficer, /api/v1/broker-claims/worklist?companyId={c}&tab=TEMP_CLOSED",
    "clmofficer, /api/v1/broker-claims/worklist?companyId={c}&tab=CLOSED&status=INSURER_LOA_ISSUANCE",
    "clmofficer, /api/v1/broker-claims/worklist?companyId={c}&tab=FOLLOW_UPS_DUE",
    "clmtl, /api/v1/broker-claims/worklist?companyId={c}&tab=ALL&flag=UNPAID_PREMIUM",
    "clmtl, /api/v1/broker-claims/worklist?companyId={c}&flag=AWAITING_REMITTANCE&page=1&size=5",
    "auditor, /api/v1/broker-claims/worklist?companyId={c}&flag=OVERDUE",
    "clmofficer, /api/v1/broker-claims/diary/mine?companyId={c}",
    "clmofficer, /api/v1/broker-claims/diary/mine?companyId={c}&includeDone=true",
    "clmofficer, /api/v1/broker-claims/search?companyId={c}&q=ARN-2026",
    "clmofficer, /api/v1/broker-claims/covers?companyId={c}&q=ARN-2026",
    "clmrisk, /api/v1/broker-claims/covers?companyId={c}&by=ASSURED&q=Demo",
    "clmofficer, /api/v1/broker-claims/location-refs?companyId={c}",
    "clmofficer, /api/v1/broker-claims/location-refs?companyId={c}&q=MGIC&page=0&size=5",
    "clmofficer, /api/v1/broker-claims/location-refs/by-cover/ARN-2026-940002?companyId={c}",
    "clmofficer, /api/v1/broker-claims/assignees",
    "clmuh, /api/v1/broker-claims/assignees",
    "ao, /api/v1/broker-claims/experience?arn=ARN-2026-940001",
    "mkttl, /api/v1/broker-claims/experience?arn=ARN-2026-940001&policyYear=1",
    "clmuh, /api/v1/broker-claims/setup/attributes/BCL_CLAIM_STATUS",
    "clmuh, /api/v1/broker-claims/setup/attributes/BCL_SETTLEMENT_TYPE",
    "clmuh, /api/v1/broker-claims/setup/matrix",
    "clmuh, /api/v1/broker-claims/setup/matrix/roles",
    "clmuh, /api/v1/broker-claims/setup/handlers",
    "clmuh, /api/v1/broker-claims/setup/users",
    "clmuh, /api/v1/broker-claims/setup/lists",
    "clmuh, /api/v1/lov/BCL_ADJUSTER/values",
    "clmrisk, /api/v1/reports",
    "ao, /api/v1/reports",
  })
  void claimsHandlingListsRespondOk(String user, String url) throws Exception {
    mvc.perform(
            get(url.replace("{c}", data.company().getId().toString()))
                .with(user(users.loadUserByUsername(user))))
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
  void healthIsUpWithSimulatedMailDelivery() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @WithUserDetails("accountant")
  void malformedRequestsAreRejectedWithBadRequest() throws Exception {
    mvc.perform(get("/api/v1/no-such-endpoint"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mvc.perform(
            post("/api/v1/journals")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\": \"not-a-number\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    mvc.perform(get("/api/v1/receivables/bank-accounts"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Required parameter 'companyId' is missing"));
    mvc.perform(get("/api/v1/receivables/bank-accounts?companyId=abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Parameter 'companyId' has an invalid value"));
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
                .content("{\"username\":\"auditor\",\"password\":\"Brokerverse@2026\"}"))
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
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"Brokerverse@2026\"}"))
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
