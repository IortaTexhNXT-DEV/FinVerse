package com.iortatechnxt.brokerverse.brokerclaims;

import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.LEAD;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.OFFICER;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Claims Handling endpoints of wave CL1-A through the full HTTP stack: every read endpoint
 * answers, a claim is recorded and worked through the API, and the permissions of the role matrix
 * hold (FR-CL-002).
 */
@IntegrationTest
class ClaimsHandlingApiIT {

  private static final String BASE = "/api/v1/broker-claims";

  @Autowired private ClaimsFixtures fx;
  @Autowired private Api api;

  private String c() {
    return "?companyId=" + fx.company();
  }

  private Map<String, Object> recordBody(String arn) {
    LocalDate today = LocalDate.now();
    Map<String, Object> loss = new HashMap<>();
    loss.put("lossDate", today.minusDays(2).toString());
    loss.put("reportedDate", today.minusDays(1).toString());
    loss.put("lossNature", "FIRE");
    loss.put("claimType", "PROPERTY");
    loss.put("lossDescription", "Kitchen fire");
    loss.put("catastropheCode", "FIRE");
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", fx.company());
    body.put("arn", arn);
    body.put("policyYear", 1);
    body.put("source", "BDOI_NOTICE");
    body.put("loss", loss);
    body.put("locations", List.of(Map.of("itemNo", 1, "description", "Kitchen")));
    return body;
  }

  @Test
  void theClaimEndpointsRecordReadAndWorkAClaim() throws Exception {
    OpsInvoice invoice = fx.propertyInvoice(2);
    String arn = invoice.getArn();
    api.doGet(OFFICER, BASE + "/covers" + c() + "&by=ARN&q=" + arn)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].arn").value(arn));
    api.doGet(OFFICER, BASE + "/covers" + c() + "&by=POLICY_NO&q=" + "FI-")
        .andExpect(status().isOk());
    api.doGet(OFFICER, BASE + "/covers" + c() + "&q=MC")
        .andExpect(status().isUnprocessableEntity());
    api.doGet(OFFICER, BASE + "/covers/" + arn + c())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.invoices[0].invoiceNo").value(invoice.getInvoiceNo()));
    api.doGet(
            OFFICER,
            BASE
                + "/covers/"
                + arn
                + "/claim-draft"
                + c()
                + "&policyYear=1&lossDate="
                + LocalDate.now())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.premium.status").value("UNPAID"))
        .andExpect(jsonPath("$.insurers.length()").value(2))
        .andExpect(jsonPath("$.lossInsidePeriod").value(true));

    JsonNode claim =
        api.read(api.doPost(OFFICER, BASE, recordBody(arn)).andExpect(status().isCreated()));
    long id = claim.get("id").asLong();
    assertThat(claim.get("claimNo").asText()).startsWith("BCL-");
    assertThat(claim.get("flags").get("unpaidPremium").asBoolean()).isTrue();
    assertThat(claim.get("flags").get("multiInsurer").asBoolean()).isTrue();
    assertThat(claim.get("flags").get("catastrophe").asBoolean()).isTrue();
    assertThat(claim.get("premium").get("canAuthorize").asBoolean()).isFalse();

    String one = BASE + "/" + id;
    for (String url :
        List.of(
            one + c(),
            BASE + "/search" + c() + "&q=" + arn,
            one + "/locations" + c(),
            one + "/insurers" + c(),
            one + "/insurers/reserve-history" + c(),
            one + "/updates" + c(),
            one + "/loss-advice" + c(),
            BASE + "/location-refs" + c() + "&q=" + arn,
            BASE + "/location-refs/by-cover/" + arn + c())) {
      api.doGet(OFFICER, url).andExpect(status().isOk());
    }
    api.doGet(OFFICER, one + "?companyId=999999").andExpect(status().isNotFound());

    JsonNode lines = api.read(api.doGet(OFFICER, one + "/insurers" + c()));
    long lead = lines.get(0).get("id").asLong();
    api.doPost(
            OFFICER,
            one + "/insurers/" + lead + "/number" + c(),
            Map.of("insurerClaimNo", "API-" + id, "confirmReuse", false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].insurerClaimNo").value("API-" + id));
    api.doPost(
            OFFICER,
            one + "/insurers/" + lead + "/reserve" + c(),
            Map.of("amount", 1000, "reason", "x"))
        .andExpect(status().isForbidden());
    api.doPost(
            TL,
            one + "/insurers/" + lead + "/reserve" + c(),
            Map.of("amount", 1000, "reason", "Estimate"))
        .andExpect(status().isOk());
    api.doPost(
            OFFICER,
            one + "/insurers/" + lead + "/adjuster" + c(),
            Map.of("adjusterCode", "CRAWFORD"))
        .andExpect(status().isForbidden());
    api.doPut(OFFICER, one + "/insurers/" + lead + "/share" + c(), Map.of("sharePct", 70))
        .andExpect(status().isOk());
    api.doPost(OFFICER, one + "/claimant" + c(), Map.of("claimantName", "X", "reason", "OTHERS"))
        .andExpect(status().isForbidden());
    api.doPost(
            TL, one + "/claimant" + c(), Map.of("claimantName", "Third Party", "reason", "OTHERS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.flags.claimantOverridden").value(true));
    api.doPost(
            OFFICER,
            one + "/reported-date" + c(),
            Map.of("reportedDate", LocalDate.now().toString(), "reason", "DATA_CORRECTION"))
        .andExpect(status().isOk());
    api.doPost(OFFICER, one + "/premium-check" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNPAID"));
    api.doPost(OFFICER, one + "/authorize" + c(), Map.of())
        .andExpect(status().isUnprocessableEntity());
    api.doPost(OFFICER, one + "/refresh-cover" + c(), Map.of()).andExpect(status().isOk());
    api.doPost(OFFICER, one + "/latest-version" + c(), Map.of())
        .andExpect(status().isUnprocessableEntity());
    api.doPost(
            OFFICER,
            one + "/updates" + c(),
            Map.of(
                "updateDate",
                LocalDate.now().toString(),
                "source",
                "EMAIL",
                "remarks",
                "Acknowledged"))
        .andExpect(status().isCreated());
    api.doPost(
            OFFICER,
            one + "/loss-advice" + c(),
            Map.of(
                "recipients",
                List.of(Map.of("insurerCode", LEAD, "to", List.of("claims@insurer.test")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].insurerCode").value(LEAD));
    api.doPost(
            OFFICER,
            BASE + "/location-refs",
            Map.of(
                "companyId",
                fx.company(),
                "arn",
                arn,
                "itemNo",
                2,
                "insurerCode",
                LEAD,
                "reference",
                "API-LOC"))
        .andExpect(status().isCreated());
    api.doPost(OFFICER, one + "/locations" + c(), Map.of("locations", List.of(Map.of("itemNo", 2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].references[0].reference").value("API-LOC"));
    api.doPut(OFFICER, one + "/locations/2" + c(), Map.of("description", "Garage"))
        .andExpect(status().isOk());
    api.doDelete(OFFICER, one + "/locations/2" + c()).andExpect(status().isOk());
    api.doPost(OFFICER, one + "/insurers" + c(), Map.of("insurerCode", " "))
        .andExpect(status().isUnprocessableEntity());
    api.doPut(
            OFFICER,
            one + "/loss" + c(),
            Map.of(
                "lossDate",
                LocalDate.now().minusDays(2).toString(),
                "lossNature",
                "FIRE",
                "claimType",
                "PROPERTY",
                "lossDescription",
                "Kitchen and dining"))
        .andExpect(status().isOk());
  }

  @Test
  void marketingSeesNoClaimsAndOfficersNoSetUp() throws Exception {
    api.doGet("ao", BASE + "/covers" + c() + "&q=ARN-2026").andExpect(status().isForbidden());
    api.doGet("ao", BASE + "/search" + c()).andExpect(status().isForbidden());
    api.doGet("clmrisk", BASE + "/location-refs" + c()).andExpect(status().isForbidden());
    api.doPost("clmrisk", BASE, recordBody("ARN-X")).andExpect(status().isForbidden());
  }
}
