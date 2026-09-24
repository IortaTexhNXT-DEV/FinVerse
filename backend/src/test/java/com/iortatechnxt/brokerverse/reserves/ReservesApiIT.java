package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.UwFixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Full-stack API test of the actuarial reserves endpoints (security, CSRF, JSON). */
@IntegrationTest
@Transactional
class ReservesApiIT {

  private static final String BASE = "/api/v1/reserves";

  @Autowired private Api api;
  @Autowired private ReserveFixtures fx;
  @Autowired private UwFixtures uw;

  private Map<String, Object> parameterBody(String line) {
    return Map.ofEntries(
        Map.entry("companyId", fx.companyId()),
        Map.entry("businessLine", line),
        Map.entry("effectiveFrom", "2026-01-01"),
        Map.entry("ibnrMethod", "CHAIN_LADDER"),
        Map.entry("ibnrRate", 0),
        Map.entry("triangleBasis", "PAID"),
        Map.entry("developmentPeriod", "QUARTER"),
        Map.entry("accidentPeriods", 8),
        Map.entry("mfadPct", 7.5),
        Map.entry("ulaePct", 3),
        Map.entry("expectedLossRatio", 60),
        Map.entry("treatyCommissionPct", 30),
        Map.entry("facCommissionPct", 20),
        Map.entry("remarks", "API test"));
  }

  @Test
  void parametersAndRunLifecycleThroughTheApi() throws Exception {
    uw.issue(uw.brokerRequest(uw.product("MOTOR", false)), UwFixtures.ISSUE);
    String c = "?companyId=" + fx.companyId();

    JsonNode param =
        api.read(
            api.doPost("accountant", BASE + "/parameters", parameterBody("MOTOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION")));
    long paramId = param.get("id").asLong();
    api.doPut("accountant", BASE + "/parameters/" + paramId, parameterBody("MOTOR"))
        .andExpect(status().isOk());
    api.doPost("accountant", BASE + "/parameters/" + paramId + "/authorize", null)
        .andExpect(status().isForbidden());
    api.doPost("checker", BASE + "/parameters/" + paramId + "/authorize", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.maker").value("accountant"));
    api.doPut("accountant", BASE + "/parameters/" + paramId, parameterBody("MOTOR"))
        .andExpect(status().isUnprocessableEntity());
    api.doGet("accountant", BASE + "/parameters" + c)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + paramId + ")].maker").value("accountant"));

    api.doPut(
            "accountant",
            BASE + "/takaful-setting",
            Map.of(
                "companyId",
                fx.companyId(),
                "enabled",
                true,
                "productCodes",
                "PA-IND",
                "participantSharePct",
                70,
                "taxPct",
                5,
                "costCenter",
                "FIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(true));
    api.doPost("fmanager", BASE + "/takaful-setting/authorize" + c, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));

    JsonNode run =
        api.read(
            api.doPost(
                    "accountant",
                    BASE + "/runs",
                    Map.of("companyId", fx.companyId(), "valuationDate", "2026-03-15"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run.valuationDate").value("2026-03-31"))
                .andExpect(jsonPath("$.run.status").value("PREVIEW")));
    long runId = run.get("run").get("id").asLong();
    assertThat(run.get("totals").size()).isPositive();
    api.doGet("accountant", BASE + "/runs/" + runId + "/upr?businessLine=MOTOR&size=10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    api.doPost("accountant", BASE + "/runs/" + runId + "/recalculate", null)
        .andExpect(status().isOk());
    api.doPost("accountant", BASE + "/runs/" + runId + "/submit", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    api.doPost("accountant", BASE + "/runs/" + runId + "/approve", null)
        .andExpect(status().isForbidden());
    api.doPost("fmanager", BASE + "/runs/" + runId + "/reject", Map.of("reason", "Recheck"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PREVIEW"));
    api.doPost("accountant", BASE + "/runs/" + runId + "/submit", null).andExpect(status().isOk());
    api.doPost("fmanager", BASE + "/runs/" + runId + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    api.doPost("fmanager", BASE + "/runs/" + runId + "/post", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("POSTED"));
    api.doGet("fmanager", BASE + "/runs/" + runId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.movements").isArray());
    api.doGet("fmanager", BASE + "/runs/" + runId + "/takaful").andExpect(status().isOk());
    api.doGet("fmanager", BASE + "/runs" + c).andExpect(status().isOk());
    api.doGet("fmanager", BASE + "/summary" + c + "&asOf=2026-03-31")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStatus").value("POSTED"));
    api.doGet(
            "fmanager",
            BASE + "/triangles" + c + "&businessLine=MOTOR&asOf=2026-03-31&accidentPeriods=4")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows.length()").value(4));
    api.doPost("fmanager", BASE + "/runs/" + runId + "/cancel", Map.of("reason", "Test reversal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void permissionsAndValidationAreEnforced() throws Exception {
    String c = "?companyId=" + fx.companyId();
    api.doGet("uw", BASE + "/runs" + c).andExpect(status().isForbidden());
    api.doGet("uw", BASE + "/summary" + c + "&asOf=2026-03-31").andExpect(status().isForbidden());
    api.doPost("accountant", BASE + "/runs", Map.of("companyId", fx.companyId()))
        .andExpect(status().isBadRequest());
    api.doGet("accountant", BASE + "/takaful-setting" + c)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(false));
  }
}
