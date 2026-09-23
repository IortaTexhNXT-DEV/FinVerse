package com.iortatechnxt.finverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tax API through the full HTTP stack with CSRF tokens: masters maker-checker, worksheet, returns
 * lifecycle, 2307 certificates, BIR exports and IC schedules, and permission checks.
 */
@IntegrationTest
class TaxApiIT {

  private static final String CODES = "/api/v1/tax/codes";
  private static final String RETURNS = "/api/v1/tax/returns";

  @Autowired private Api api;
  @Autowired private TaxFixtures fixtures;

  @BeforeEach
  void masters() {
    fixtures.masters();
  }

  @Test
  void taxCodeIsCreatedByMakerAndAuthorizedByChecker() throws Exception {
    String code = "WC" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    JsonNode created =
        api.read(
            api.doPost(
                    "accountant",
                    CODES,
                    Json.of(
                        "companyId", fixtures.companyId(),
                        "code", code,
                        "name", "Test ATC",
                        "taxType", "EWT",
                        "atc", code,
                        "payeeClass", "CORPORATE",
                        "rate", 2,
                        "glAccountCode", "2508",
                        "incomeNature", "Test services",
                        "effectiveFrom", "2026-01-01"))
                .andExpect(status().isCreated()));
    assertThat(created.get("recordStatus").asText()).isEqualTo("PENDING_AUTHORIZATION");
    long id = created.get("id").asLong();
    api.doPost("accountant", CODES + "/" + id + "/authorize", null)
        .andExpect(status().isForbidden());
    api.doPost("checker", CODES + "/" + id + "/authorize", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doPut(
            "accountant",
            CODES + "/" + id,
            Json.of(
                "companyId",
                fixtures.companyId(),
                "code",
                code,
                "name",
                "Test ATC renamed",
                "taxType",
                "EWT",
                "atc",
                code,
                "rate",
                2,
                "glAccountCode",
                "2508",
                "effectiveFrom",
                "2026-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doPost(
            "accountant",
            CODES,
            Json.of(
                "companyId", fixtures.companyId(),
                "code", "BAD",
                "name", "Missing ATC",
                "taxType", "EWT",
                "rate", 2,
                "glAccountCode", "2508",
                "effectiveFrom", "2026-01-01"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ATC_REQUIRED"));
  }

  @Test
  void returnIsPreparedFiledAndPaidThroughTheApi() throws Exception {
    String c = fixtures.companyId().toString();
    JsonNode draft =
        api.read(
            api.doPost(
                    "accountant",
                    RETURNS,
                    Json.of("companyId", c, "formCode", "2551Q", "periodStart", "2026-10-01"))
                .andExpect(status().isCreated()));
    long id = draft.get("id").asLong();
    assertThat(draft.get("status").asText()).isEqualTo("DRAFT");
    assertThat(draft.get("dueDate").asText()).isEqualTo("2027-01-25");
    api.doPost("accountant", RETURNS + "/" + id + "/refresh", null).andExpect(status().isOk());
    api.doPost(
            "checker",
            RETURNS + "/" + id + "/file",
            Json.of("filedOn", "2027-01-20", "reference", "EFPS-API"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FILED"));
    api.doPost(
            "checker",
            RETURNS + "/" + id + "/pay",
            Json.of("paidOn", "2027-01-20", "bankAccountCode", "BDO-CA", "reference", "PAY-API"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.remittance.paymentReference").value("PAY-API"));
    api.doGet("auditor", RETURNS + "/" + id).andExpect(status().isOk());
    api.doGet("auditor", RETURNS + "?companyId=" + c + "&year=2026&status=PAID")
        .andExpect(status().isOk());

    JsonNode lbt =
        api.read(
            api.doPost(
                "accountant",
                RETURNS,
                Json.of("companyId", c, "formCode", "LBT", "periodStart", "2026-10-01")));
    api.doPost(
            "accountant",
            RETURNS + "/" + lbt.get("id").asLong() + "/cancel",
            Json.of("reason", "Prepared by mistake"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void worksheetsExportsCertificatesAndSchedulesAreServed() throws Exception {
    String c = "companyId=" + fixtures.companyId();
    api.doGet("accountant", "/api/v1/tax/worksheets/VAT?" + c + "&from=2026-07-01&to=2026-09-30")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodLabel").value("2026-Q3"))
        .andExpect(jsonPath("$.lines").isArray());
    api.doGet("accountant", "/api/v1/tax/exports/QAP?" + c + "&year=2026&quarter=3")
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv"));
    api.doGet("accountant", "/api/v1/tax/ic/schedules/RBC?" + c + "&from=2026-01-01&to=2026-09-30")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rbc.hurdle").value(100));

    fixtures.supplierInvoice(LocalDate.of(2026, 7, 7), "3000");
    JsonNode batch =
        api.read(
            api.doPost(
                "accountant",
                "/api/v1/tax/2307/batches",
                Json.of("companyId", fixtures.companyId(), "year", 2026, "quarter", 3)));
    if (batch.has("id")) {
      api.doGet("accountant", "/api/v1/tax/2307/batches/" + batch.get("id").asLong() + "/pdf")
          .andExpect(status().isOk())
          .andExpect(content().contentType("application/pdf"));
    }
    JsonNode register =
        api.read(api.doGet("accountant", "/api/v1/tax/2307/certificates?" + c + "&year=2026"));
    assertThat(register.isArray()).isTrue();
    assertThat(register.size()).isPositive();
    long certificate = register.get(0).get("id").asLong();
    api.doGet("accountant", "/api/v1/tax/2307/certificates/" + certificate + "/pdf")
        .andExpect(status().isOk());
  }

  @Test
  void usersWithoutTaxPermissionsAreRefused() throws Exception {
    String c = "companyId=" + fixtures.companyId();
    api.doGet("uw", "/api/v1/tax/codes?" + c).andExpect(status().isForbidden());
    api.doPost(
            "auditor",
            RETURNS,
            Json.of(
                "companyId", fixtures.companyId(), "formCode", "LBT", "periodStart", "2026-07-01"))
        .andExpect(status().isForbidden());
  }
}
