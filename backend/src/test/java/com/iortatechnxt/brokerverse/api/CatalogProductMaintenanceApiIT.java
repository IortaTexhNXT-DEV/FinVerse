package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.catalog.PackageFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Product Maintenance catalog endpoints (BRD-3, wave P1-A) through the full HTTP stack: versions
 * and the validation checkpoint, coverages and clauses, incentive criteria, rate-scheme exceptions
 * and the calculator's version.
 */
@com.iortatechnxt.brokerverse.support.IntegrationTest
class CatalogProductMaintenanceApiIT {

  @Autowired private Api api;
  @Autowired private PackageFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "mbs, /api/v1/catalog/products?lifecycle=EXPIRED",
    "ao, /api/v1/catalog/products?packaged=true",
    "mbs, /api/v1/catalog/products/MTR12/versions",
    "tsuhead, /api/v1/catalog/products/MTR12/versions/2",
    "tsuhead, /api/v1/catalog/validation-queue",
    "ao, /api/v1/catalog/coverages",
    "mbs, /api/v1/catalog/coverages?line=MOTOR",
    "mbs, /api/v1/catalog/clauses",
    "mbs, /api/v1/catalog/incentive-criteria?companyId={c}",
    "badmin, /api/v1/catalog/rate-scheme-exceptions",
    "ao, /api/v1/catalog/rate-scheme-exceptions?transactionRef=QT-X",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isOk());
  }

  @Test
  void permissionsFollowTheActionMatrix() throws Exception {
    api.doGet("ao", "/api/v1/catalog/products?lifecycle=EXPIRED").andExpect(status().isForbidden());
    api.doGet("ao", "/api/v1/catalog/validation-queue").andExpect(status().isForbidden());
    api.doPost("ao", "/api/v1/catalog/coverages", coverage("X_" + PackageFixtures.code()))
        .andExpect(status().isForbidden());
    api.doPost(
            "tsuhead",
            "/api/v1/catalog/products/MTR12/versions",
            Map.of("companyId", fx.company(), "changeSummary", "Not allowed"))
        .andExpect(status().isForbidden());
    api.doPost(
            "mbs",
            "/api/v1/catalog/products/MTR12/versions/2/validate",
            Map.of("checklist", List.of()))
        .andExpect(status().isForbidden());
  }

  @Test
  void aVersionIsEditedSubmittedAndValidatedOverHttp() throws Exception {
    String code = fx.released();
    JsonNode draft =
        api.read(
            api.doPost(
                    "mbs",
                    "/api/v1/catalog/products/" + code + "/versions",
                    Map.of("companyId", fx.company(), "changeSummary", "Rate review"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.versionNo").value(2))
                .andExpect(jsonPath("$.summary.status").value("DRAFT")));
    String base = "/api/v1/catalog/products/" + code + "/versions/2";
    String tomorrow = fx.today().plusDays(1).toString();
    Map<String, Object> content =
        Map.of(
            "companyId",
            fx.company(),
            "rateScheme",
            Map.of("defaultRate", 1.6, "minimumPremium", 3500, "defaultCommissionRate", 15),
            "dates",
            Map.of("effectiveFrom", tomorrow, "packageEndDate", fx.today().plusYears(1).toString()),
            "coverages",
            List.of(Map.of("coverageCode", "OD_THEFT", "included", true, "sortOrder", 1)),
            "insurers",
            List.of(Map.of("insurerCode", "INS-MGIC", "role", "PANEL")),
            "insurerTerms",
            List.of(
                Map.of(
                    "insurerCode",
                    "INS-MGIC",
                    "coverageCode",
                    "OD_THEFT",
                    "included",
                    true,
                    "clauseCodes",
                    List.of("MTR_PARTICIPATION"))),
            "changeSummary",
            "New rate 1.6%");
    api.doPut("mbs", base, content)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheme.defaultRate").value(1.6))
        .andExpect(jsonPath("$.insurerTerms[0].clauseCodes").value("MTR_PARTICIPATION"));
    api.doPost("mbs", base + "/submit", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("FOR_VALIDATION"));
    api.doGet("tsuhead", "/api/v1/catalog/validation-queue")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.productCode == '" + code + "')]").isNotEmpty());
    api.doPost("tsuhead", base + "/return", Map.of("reason", "Check the minimum"))
        .andExpect(jsonPath("$.summary.status").value("DRAFT"))
        .andExpect(jsonPath("$.returnedReason").value("Check the minimum"));
    api.doPost("mbs", base + "/submit", Map.of()).andExpect(status().isOk());
    api.doPost("badmin", base + "/validate", Map.of("checklist", List.of("Rates within bounds")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("RELEASED"))
        .andExpect(jsonPath("$.summary.validatedBy").value("badmin"));
    api.doGet("ao", "/api/v1/catalog/products?q=" + code)
        .andExpect(jsonPath("$[0].currentVersionNo").value(1))
        .andExpect(jsonPath("$[0].lifecycleStatus").value("ACTIVE"));
    api.doPost(
            "ao",
            "/api/v1/catalog/rating/quote",
            Map.of(
                "companyId",
                fx.company(),
                "productCode",
                code,
                "items",
                List.of(Map.of("sumInsured", 1000000)),
                "schemeVersion",
                1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schemeVersion").value(1))
        .andExpect(jsonPath("$.nonCurrent").value(false));
    api.doPut("mbs", base, content).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void coveragesClausesIncentivesAndExceptionsAreMaintainedOverHttp() throws Exception {
    String token = PackageFixtures.code();
    JsonNode coverage =
        api.read(
            api.doPost("mbs", "/api/v1/catalog/coverages", coverage("C_" + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION")));
    api.doPost(
            "approver",
            "/api/v1/catalog/records/COVERAGE/" + coverage.get("id").asLong() + "/authorize",
            Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doPost(
            "mbs",
            "/api/v1/catalog/clauses",
            Map.of(
                "code",
                "CL_" + token,
                "kind",
                "WARRANTY",
                "title",
                "Test warranty",
                "wording",
                "Warranted that...",
                "effectiveFrom",
                "2026-01-01"))
        .andExpect(status().isCreated());

    String product = fx.released();
    JsonNode criterion =
        api.read(
            api.doPost(
                    "mbs",
                    "/api/v1/catalog/incentive-criteria",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "code",
                        "I" + token,
                        "name",
                        "Http incentive",
                        "incentiveType",
                        "CAMPAIGN",
                        "valueBasis",
                        "FIXED_AMOUNT",
                        "value",
                        500,
                        "products",
                        List.of(Map.of("productCode", product)),
                        "effectiveFrom",
                        "2026-01-01"))
                .andExpect(status().isCreated()));
    long id = criterion.get("id").asLong();
    api.doPost(
            "approver", "/api/v1/catalog/records/INCENTIVE_CRITERIA/" + id + "/authorize", Map.of())
        .andExpect(status().isOk());
    api.doGet("mbs", "/api/v1/catalog/incentive-criteria/" + id + "/history")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recordStatus").value("ACTIVE"));
    api.doPost("mbs", "/api/v1/catalog/incentive-criteria/" + id + "/deactivate", Map.of())
        .andExpect(jsonPath("$.recordStatus").value("INACTIVE"));

    JsonNode exception =
        api.read(
            api.doPost(
                    "ao",
                    "/api/v1/catalog/rate-scheme-exceptions",
                    Map.of(
                        "productCode",
                        product,
                        "requestedRate",
                        2.5,
                        "transactionRef",
                        "QT-" + token,
                        "reason",
                        "Loyal client"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNo").isNotEmpty()));
    api.doPost(
            "approver",
            "/api/v1/catalog/records/RATE_SCHEME_EXCEPTION/"
                + exception.get("id").asLong()
                + "/deactivate",
            Map.of())
        .andExpect(jsonPath("$.recordStatus").value("INACTIVE"));
  }

  private static Map<String, Object> coverage(String code) {
    return Map.of(
        "lineCode",
        "MOTOR",
        "code",
        code,
        "name",
        "Test coverage",
        "kind",
        "EXTENSION",
        "basic",
        false,
        "sortOrder",
        90);
  }
}
