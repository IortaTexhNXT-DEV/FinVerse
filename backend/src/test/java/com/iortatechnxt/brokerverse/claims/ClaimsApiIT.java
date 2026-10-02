package com.iortatechnxt.brokerverse.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/** The claims REST API through the full HTTP stack (security, CSRF, validation, serialization). */
@IntegrationTest
class ClaimsApiIT {

  private static final String BASE = "/api/v1/claims";
  private static final String MAKER = ClaimFixtures.MAKER;
  private static final String CHECKER = ClaimFixtures.CHECKER;
  private static final String DATE = "2026-04-08";

  @Autowired private Api api;
  @Autowired private ClaimFixtures fx;

  private JsonNode ok(ResultActions result) throws Exception {
    return api.read(result.andExpect(status().is2xxSuccessful()));
  }

  private long id(JsonNode node) {
    return node.get("id").asLong();
  }

  private JsonNode approve(String kind, long id) throws Exception {
    return ok(
        api.doPost(
            CHECKER, BASE + "/" + kind + "/" + id + "/approve", Json.of("accountingDate", DATE)));
  }

  @Test
  void claimLifecycleThroughTheApi() throws Exception {
    Policy policy = fx.policy("MOTOR");
    String company = String.valueOf(fx.companyId());
    JsonNode cover =
        ok(
            api.doGet(
                MAKER,
                BASE
                    + "/policy-cover?companyId="
                    + company
                    + "&policyNo="
                    + policy.getPolicyNo()
                    + "&lossDate=2026-04-02"));
    assertThat(cover.get("inForce").asBoolean()).isTrue();
    assertThat(cover.get("risks")).hasSize(1);

    ClaimRequest request = fx.request(policy, "90000", null);
    JsonNode claim = ok(api.doPost(MAKER, BASE, request));
    long claimId = id(claim);
    assertThat(claim.get("status").asText()).isEqualTo("REGISTERED");
    assertThat(claim.get("parties")).hasSize(2);

    JsonNode pending = ok(api.doGet(MAKER, BASE + "/" + claimId + "/reserves"));
    assertThat(
            approve("reserves", pending.get(0).get("id").asLong())
                .get("changeAmount")
                .decimalValue())
        .isEqualByComparingTo("90000");
    ok(
        api.doPost(
            MAKER,
            BASE + "/" + claimId + "/parties",
            Json.of("role", "THIRD_PARTY", "partyCode", "TP-0001")));

    long increase =
        id(
            ok(
                api.doPost(
                    MAKER,
                    BASE + "/" + claimId + "/reserves",
                    Json.of(
                        "side",
                        "PAYMENT",
                        "costType",
                        "LOSS",
                        "newEstimate",
                        95000,
                        "reason",
                        "More"))));
    ok(api.doPost(CHECKER, BASE + "/reserves/" + increase + "/reject", Json.of("reason", "No")));

    long lpo =
        id(
            ok(
                api.doPost(
                    MAKER,
                    BASE + "/" + claimId + "/lpos",
                    Json.of(
                        "garageCode",
                        "G-0002",
                        "cover",
                        "OD",
                        "gross",
                        41000,
                        "discount",
                        1000,
                        "description",
                        "Repair"))));
    assertThat(ok(api.doGet(MAKER, BASE + "/" + claimId + "/lpos"))).hasSize(1);
    assertThat(ok(api.doGet(MAKER, BASE + "/lpos?companyId=" + company)).size()).isPositive();
    ok(api.doPost(MAKER, BASE + "/lpos/" + lpo + "/cancel", Json.of("reason", "Duplicate")));

    long settlement =
        id(
            ok(
                api.doPost(
                    MAKER,
                    BASE + "/" + claimId + "/settlements",
                    Json.of(
                        "payeeCode",
                        "G-0002",
                        "costType",
                        "LOSS",
                        "settlementType",
                        "PARTIAL",
                        "assessedAmount",
                        40000,
                        "deductible",
                        1000,
                        "narration",
                        "Garage bill"))));
    assertThat(approve("settlements", settlement).get("netAmount").decimalValue())
        .isEqualByComparingTo("39000");
    assertThat(ok(api.doGet(MAKER, BASE + "/" + claimId + "/settlements"))).hasSize(1);

    ok(
        api.doPost(
            MAKER,
            BASE + "/" + claimId + "/reserves",
            Json.of("side", "RECOVERY", "costType", "LOSS", "newEstimate", 5000, "reason", "TP")));
    long recoveryEstimate =
        ok(api.doGet(MAKER, BASE + "/" + claimId + "/reserves")).get(2).get("id").asLong();
    approve("reserves", recoveryEstimate);
    long recovery =
        id(
            ok(
                api.doPost(
                    MAKER,
                    BASE + "/" + claimId + "/recoveries",
                    Json.of(
                        "recoveryType",
                        "SUBROGATION",
                        "bankAccountCode",
                        "1111",
                        "amount",
                        5000,
                        "narration",
                        "From TP insurer"))));
    approve("recoveries", recovery);
    assertThat(ok(api.doGet(MAKER, BASE + "/" + claimId + "/recoveries"))).hasSize(1);
    assertThat(ok(api.doGet(MAKER, BASE + "/" + claimId + "/movements")).size()).isGreaterThan(3);

    JsonNode closed =
        ok(
            api.doPost(
                CHECKER,
                BASE + "/" + claimId + "/close",
                Json.of("reason", "Done", "accountingDate", DATE)));
    assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
    assertThat(
            ok(api.doPost(CHECKER, BASE + "/" + claimId + "/reopen", Json.of("reason", "More")))
                .get("status")
                .asText())
        .isEqualTo("REOPENED");

    JsonNode page =
        ok(
            api.doGet(
                MAKER, BASE + "?companyId=" + company + "&q=" + claim.get("claimNo").asText()));
    assertThat(page.get("content")).hasSize(1);
    assertThat(
            ok(api.doGet(MAKER, BASE + "/" + claimId))
                .get("totals")
                .get("recovered")
                .decimalValue())
        .isEqualByComparingTo("5000");
  }

  @Test
  void declineEndpointsSecurityAndValidation() throws Exception {
    Policy policy = fx.policy("FIRE");
    long first = id(ok(api.doPost(MAKER, BASE, fx.request(policy, null, null))));
    assertThat(
            ok(api.doPost(
                    CHECKER, BASE + "/" + first + "/withdraw", Json.of("reason", "Withdrawn")))
                .get("status")
                .asText())
        .isEqualTo("WITHDRAWN");
    long second = id(ok(api.doPost(MAKER, BASE, fx.request(policy, null, null))));
    assertThat(
            ok(api.doPost(
                    CHECKER, BASE + "/" + second + "/repudiate", Json.of("reason", "Excluded")))
                .get("status")
                .asText())
        .isEqualTo("REJECTED");

    api.doPost("uw", BASE, fx.request(policy, null, null)).andExpect(status().isForbidden());
    api.doPost(MAKER, BASE + "/" + second + "/close", Json.of("reason", "x"))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(MAKER, BASE, Json.of("companyId", fx.companyId()))
        .andExpect(status().isBadRequest());
    api.doGet(MAKER, BASE + "/0").andExpect(status().isNotFound());
    api.doGet(MAKER, BASE + "/policy-cover?companyId=" + fx.companyId() + "&policyNo=NONE")
        .andExpect(status().isUnprocessableEntity());
  }
}
