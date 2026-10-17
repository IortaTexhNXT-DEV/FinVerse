package com.iortatechnxt.brokerverse.submitted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A policy created and edited by hand on the screens (FR-SP-003, gap G11): the Policy Review
 * Officer creates a Non-CBG Corporate policy from the Masterlist (New Policy), the record is
 * VALIDATED with the source MANUAL, the same policy cannot be created twice, an edit keeps the
 * changed fields in the history and the record is tagged Renewable by hand.
 */
@IntegrationTest
class ManualPolicyIT {

  private static final String REVIEWER = "polreview";
  private static final String POLICIES = "/api/v1/submitted/policies";

  @Autowired private Api api;
  @Autowired private TestData data;

  private static Map<String, Object> policy(String policyNo, String assured, String sumInsured) {
    LocalDate inception = LocalDate.now();
    Map<String, Object> terms = new HashMap<>();
    terms.put("insurerCode", "INS-MPI");
    terms.put("policyNo", policyNo);
    terms.put("inceptionDate", inception.toString());
    terms.put("expiryDate", inception.plusYears(1).toString());
    terms.put("sumInsured", sumInsured);
    terms.put("totalPremium", "18500.00");
    terms.put("currency", "PHP");
    Map<String, Object> body = new HashMap<>();
    body.put("segment", "NONCBG_CORPORATE");
    body.put("businessType", "NB");
    body.put(
        "assured", Json.of("assuredName", assured, "mailingAddress", "Ortigas Center, Pasig City"));
    body.put("terms", terms);
    body.put("risk", Json.of("propertyLocation", "Ortigas Center, Pasig City"));
    body.put("tracking", null);
    return body;
  }

  @Test
  void aPolicyIsCreatedEditedAndTaggedByHand() throws Exception {
    String policyNo = "NCC-" + System.nanoTime();
    JsonNode created =
        api.read(
            api.doPost(
                    REVIEWER,
                    POLICIES + "?companyId=" + data.company().getId(),
                    policy(policyNo, "Pasig Logistics Corporation", "25000000.00"))
                .andExpect(status().isOk()));
    long id = created.path("row").path("id").asLong();
    assertThat(created.path("row").path("status").asText()).isEqualTo("VALIDATED");
    assertThat(created.path("origin").path("sourceCode").asText()).isEqualTo("MANUAL");

    api.doPost(
            REVIEWER,
            POLICIES + "?companyId=" + data.company().getId(),
            policy(policyNo, "Pasig Logistics Corporation", "25000000.00"))
        .andExpect(status().isUnprocessableEntity());

    JsonNode edited =
        api.read(
            api.doPut(
                    REVIEWER,
                    POLICIES + "/" + id,
                    policy(policyNo, "Pasig Logistics Corp.", "26000000.00"))
                .andExpect(status().isOk()));
    assertThat(edited.path("data").path("assured").path("assuredName").asText())
        .isEqualTo("Pasig Logistics Corp.");
    JsonNode history =
        api.read(api.doGet(REVIEWER, POLICIES + "/" + id + "/history").andExpect(status().isOk()));
    assertThat(history.findValuesAsText("newValue")).contains("Pasig Logistics Corp.");

    JsonNode tagged =
        api.read(
            api.doPost(
                    REVIEWER,
                    POLICIES + "/" + id + "/renewal-tag",
                    Json.of("tag", "RENEWABLE", "reason", null))
                .andExpect(status().isOk()));
    assertThat(tagged.path("row").path("renewalTag").asText()).isEqualTo("RENEWABLE");
  }

  @Test
  void aPolicyWithoutAssuredIsRefused() throws Exception {
    Map<String, Object> body = policy("NCC-" + System.nanoTime(), " ", "1000000.00");
    api.doPost(REVIEWER, POLICIES + "?companyId=" + data.company().getId(), body)
        .andExpect(status().is4xxClientError());
  }
}
