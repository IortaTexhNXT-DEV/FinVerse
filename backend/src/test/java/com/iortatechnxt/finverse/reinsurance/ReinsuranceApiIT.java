package com.iortatechnxt.finverse.reinsurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Full-stack API tests of the reinsurance module (security, CSRF, validation, serialization). */
@IntegrationTest
class ReinsuranceApiIT {

  private static final int YEAR = 2037;
  private static final String BASE = "/api/v1/reinsurance";

  @Autowired private Api api;
  @Autowired private RiFixtures fx;

  private Map<String, Object> treaty(String code) {
    return Json.of(
        "companyId", fx.companyId(),
        "code", code,
        "name", "API quota share " + YEAR,
        "treatyType", "QUOTA_SHARE",
        "businessLine", "FIRE",
        "uwYear", YEAR,
        "periodFrom", YEAR + "-01-01",
        "periodTo", YEAR + "-12-31",
        "currency", "PHP",
        "quotaSharePct", 30,
        "levyPct", 1,
        "participants",
            List.of(
                Json.of("reinsurerCode", "R-0001", "sharePct", 70, "commissionPct", 30),
                Json.of("reinsurerCode", "R-0004", "sharePct", 30, "commissionPct", 30)));
  }

  @Test
  void treatyLifecycleThroughTheApi() throws Exception {
    Map<String, Object> body = treaty("API-QS-37");
    JsonNode created =
        api.read(
            api.doPost(RiFixtures.MAKER, BASE + "/treaties", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION")));
    long id = created.get("id").asLong();

    api.doPost(RiFixtures.MAKER, BASE + "/treaties/" + id + "/authorize", null)
        .andExpect(status().isForbidden());
    api.doPost("auditor", BASE + "/treaties", treaty("API-QS-X")).andExpect(status().isForbidden());
    api.doPost(RiFixtures.MAKER, BASE + "/treaties", Json.of("companyId", fx.companyId()))
        .andExpect(status().isBadRequest());
    api.doPut(RiFixtures.MAKER, BASE + "/treaties/" + id, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participants.length()").value(2));
    api.doPost(RiFixtures.CHECKER, BASE + "/treaties/" + id + "/authorize", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doGet(RiFixtures.MAKER, BASE + "/treaties/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statementFrequency").value("QUARTERLY"));

    Policy policy =
        fx.policy(fx.product("FIRE"), YEAR, "PHP", List.of(fx.risk("10000000", "20000")));
    api.doPost(RiFixtures.MAKER, BASE + "/cessions/policies/" + policy.getId(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].treatyPremium").value(6000.0));
    JsonNode cessions =
        api.read(
            api.doGet(
                    RiFixtures.MAKER,
                    BASE + "/cessions?companyId=" + fx.companyId() + "&policyNo=" + policy.getPolicyNo())
                .andExpect(status().isOk()));
    assertThat(cessions).hasSize(1);
    api.doGet(RiFixtures.MAKER, BASE + "/cessions/" + cessions.get(0).get("id").asLong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines.length()").value(3));

    statementsThroughTheApi();
  }

  private void statementsThroughTheApi() throws Exception {
    JsonNode soas =
        api.read(
            api.doPost(
                    RiFixtures.MAKER,
                    BASE + "/soas",
                    Json.of(
                        "companyId", fx.companyId(),
                        "treatyCode", "API-QS-37",
                        "reinsurerCode", "R-0004",
                        "year", 2026,
                        "quarter", 1))
                .andExpect(status().isOk()));
    long soaId = soas.get(0).get("id").asLong();
    api.doGet(RiFixtures.MAKER, BASE + "/soas/" + soaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layout.lines.length()").value(10))
        .andExpect(jsonPath("$.layout.amountInWords").isNotEmpty());
    api.doPost(RiFixtures.CHECKER, BASE + "/soas/" + soaId + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    api.doPost(
            RiFixtures.CHECKER,
            BASE + "/soas/" + soaId + "/settle",
            Json.of("settlementDate", "2026-04-25", "bankAccountCode", "1111"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SETTLED"));
    api.doPost(RiFixtures.CHECKER, BASE + "/soas/" + soaId + "/settle", Json.of())
        .andExpect(status().isBadRequest());
  }

  @Test
  void facultativeSlipAndAllocationRunThroughTheApi() throws Exception {
    Map<String, Object> limited = treaty("API-QS-38");
    limited.put("uwYear", 2038);
    limited.put("periodFrom", "2038-01-01");
    limited.put("periodTo", "2038-12-31");
    limited.put("treatyLimit", 1000000);
    long treatyId =
        api.read(
                api.doPost(RiFixtures.MAKER, BASE + "/treaties", limited)
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost(RiFixtures.CHECKER, BASE + "/treaties/" + treatyId + "/authorize", null)
        .andExpect(status().isOk());
    Policy policy =
        fx.policy(fx.product("FIRE"), 2038, "PHP", List.of(fx.risk("5000000", "10000")));
    String period = "companyId=" + fx.companyId() + "&from=2026-03-10&to=2026-03-10";
    api.doGet(RiFixtures.MAKER, BASE + "/allocation/preview?" + period)
        .andExpect(status().isOk());
    api.doPost(
            RiFixtures.MAKER,
            BASE + "/allocation/runs",
            Json.of("companyId", fx.companyId(), "fromDate", "2026-03-10", "toDate", "2026-03-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    api.doGet(RiFixtures.MAKER, BASE + "/cessions?companyId=" + fx.companyId() + "&from=2026-03-10&to=2026-03-10")
        .andExpect(status().isOk());

    JsonNode provisional =
        api.read(
            api.doGet(
                    RiFixtures.MAKER,
                    BASE + "/fac-placements?companyId=" + fx.companyId() + "&status=PROVISIONAL")
                .andExpect(status().isOk()));
    JsonNode slip = null;
    for (JsonNode p : provisional) {
      if (policy.getPolicyNo().equals(p.get("policyNo").asText())) {
        slip = p;
      }
    }
    assertThat(slip).isNotNull();
    assertThat(slip.get("facSi").decimalValue()).isEqualByComparingTo("4000000");
    long id = slip.get("id").asLong();
    api.doPut(
            RiFixtures.MAKER,
            BASE + "/fac-placements/" + id + "/participants",
            Json.of(
                "participants",
                List.of(Json.of("reinsurerCode", "R-0003", "sharePct", 100, "commissionPct", 10))))
        .andExpect(status().isOk());
    api.doPost(RiFixtures.MAKER, BASE + "/fac-placements/" + id + "/submit", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    api.doPost(RiFixtures.CHECKER, BASE + "/fac-placements/" + id + "/approve", Json.of("date", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PLACED"));
    api.doPost(RiFixtures.MAKER, BASE + "/fac-placements/" + id + "/close", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
    api.doGet(RiFixtures.MAKER, BASE + "/fac-placements/" + id).andExpect(status().isOk());
    api.doPost(RiFixtures.MAKER, BASE + "/fac-placements/" + id + "/reject", null)
        .andExpect(status().isForbidden());
  }

  @Test
  void claimSharesAreListedAndCaughtUp() throws Exception {
    api.doGet(
            RiFixtures.MAKER,
            BASE + "/claims/movements?companyId=" + fx.companyId() + "&from=2026-01-01&to=2026-12-31")
        .andExpect(status().isOk());
    api.doPost(
            RiFixtures.MAKER,
            BASE + "/claims/catch-up",
            Json.of("companyId", fx.companyId(), "fromDate", "2026-01-01", "toDate", "2026-12-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processed").value(0));
  }
}
