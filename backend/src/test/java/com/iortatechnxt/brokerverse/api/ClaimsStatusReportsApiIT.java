package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.brokerclaims.ClaimFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of wave CL1-B of Claims Handling (BRD-7): claim progress and history, status
 * actions, diary, home, worklist, reassignment, loss experience and Claims Setup, with the rights of
 * the FRS access matrix (officers, TL, TH, UH, Marketing).
 */
@IntegrationTest
class ClaimsStatusReportsApiIT {

  private static final String BASE = "/api/v1/broker-claims/";

  @Autowired private Api api;
  @Autowired private ClaimFixtures fixtures;

  private String q() {
    return "?companyId=" + fixtures.company();
  }

  @Test
  void theReadEndpointsAnswer() throws Exception {
    ClaimFixtures.Spec spec = fixtures.spec("clmofficer", ClaimFixtures.today().minusDays(3));
    Long id = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
    String claim = BASE + id;
    api.doGet("clmofficer", claim + "/progress" + q())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status.code").value("NEW_COMPLETE_DOCS"))
        .andExpect(jsonPath("$.ages.overall").value(3));
    api.doGet("auditor", claim + "/history" + q())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusChanges.length()").value(1));
    api.doGet("clmofficer", claim + "/allowed-statuses" + q())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    api.doGet("clmofficer", claim + "/diary" + q()).andExpect(status().isOk());
    api.doGet("clmofficer", BASE + "diary/mine" + q()).andExpect(status().isOk());
    api.doGet("clmofficer", BASE + "home" + q())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tiles.length()").value(7));
    api.doGet("clmofficer", BASE + "worklist" + q() + "&tab=MINE&q=" + spec.claimNo())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].claimNo").value(spec.claimNo()));
    api.doGet("clmtl", BASE + "assignees").andExpect(status().isOk());
    api.doGet("ao", BASE + "experience?arn=" + spec.arn())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.claimCount").value(1));
    api.doGet("clmuh", BASE + "setup/attributes/BCL_CLAIM_STATUS")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(18)));
    api.doGet("clmuh", BASE + "setup/matrix").andExpect(status().isOk());
    api.doGet("clmuh", BASE + "setup/matrix/roles").andExpect(status().isOk());
    api.doGet("clmuh", BASE + "setup/handlers").andExpect(status().isOk());
    api.doGet("clmuh", BASE + "setup/users").andExpect(status().isOk());
    api.doGet("clmuh", BASE + "setup/lists")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'BCL_ADJUSTER')]").exists());
  }

  @Test
  void theRightsFollowTheAccessMatrix() throws Exception {
    Long id = fixtures.recorded(fixtures.spec("clmofficer", ClaimFixtures.today()), "NEW_COMPLETE_DOCS");
    String claim = BASE + id;
    api.doGet("ao", claim + "/progress" + q()).andExpect(status().isForbidden());
    api.doGet("ao", BASE + "worklist" + q()).andExpect(status().isForbidden());
    api.doPost("clmuh", claim + "/status" + q(), Map.of("statusCode", "INSURER_REVIEW"))
        .andExpect(status().isForbidden());
    api.doPost("clmofficer", claim + "/settlement" + q(), Map.of("typeCode", "CLOSED_DENIED"))
        .andExpect(status().isForbidden());
    api.doPost("clmofficer", claim + "/follow-up" + q(), Map.of("date", "2030-01-01"))
        .andExpect(status().isForbidden());
    api.doPost("clmofficer", BASE + "reassign" + q(), Map.of("claimIds", List.of(id), "handler", "clmtl"))
        .andExpect(status().isForbidden());
    api.doGet("clmth", BASE + "setup/matrix").andExpect(status().isForbidden());
    api.doPost("clmofficer", claim + "/status" + q(), Map.of("statusCode", "INSURER_CHECK_ISSUANCE"))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.detail").value("You are not allowed to set the status For Insurer's Issuance of Check"));
    api.doPost("clmtl", claim + "/status" + q(), Map.of("statusCode", "INSURER_REVIEW", "remark", "Sent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("IN_PROGRESS"));
    api.doPut("clmofficer", claim + "/action-plan" + q(), Map.of("text", "Chase the adjuster"))
        .andExpect(status().isOk());
    api.doPost("clmofficer", claim + "/diary" + q(), Map.of("entryType", "CALL", "text", "Called the insurer"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.typeLabel").value("Call"));
    api.doPost("clmtl", claim + "/settlement" + q(), Map.of("typeCode", "CLOSED_DENIED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("CLOSED"));
    api.doPost("clmtl", claim + "/reopen" + q(), Map.of("reasonCode", "OTHERS"))
        .andExpect(status().isForbidden());
    api.doPost("clmuh", claim + "/reopen" + q(), Map.of("reasonCode", "OTHERS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("IN_PROGRESS"));
  }
}
