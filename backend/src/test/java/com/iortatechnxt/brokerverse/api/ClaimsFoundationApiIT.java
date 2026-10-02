package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of the Claims Handling foundation (BRD-7, wave CL0): the Claims Unit Head maintains
 * the Claims lists through their owner permission BCL_SETUP (CLAIMS_BROKING_DESIGN 12.1;
 * BRCLM.010/014/017/036) without the global LOV_MANAGE, and nobody else gains list rights.
 */
@IntegrationTest
class ClaimsFoundationApiIT {

  private static final String ADJUSTERS = "/api/v1/lov/BCL_ADJUSTER/values";

  @Autowired private Api api;

  private static Map<String, Object> value(String code, String label) {
    return Map.of("code", code, "label", label, "sortOrder", 900, "effectiveFrom", "2026-01-01");
  }

  @Test
  void theUnitHeadMaintainsTheClaimsListsWithTheSetupPermission() throws Exception {
    String code = "API_ADJ_" + (System.nanoTime() % 1_000_000);
    api.doGet("clmuh", ADJUSTERS)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(25)));
    Long id =
        api.read(
                api.doPost("clmuh", ADJUSTERS, value(code, "Api Adjusters, Inc."))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING_AUTHORIZATION")))
            .get("id")
            .asLong();
    api.doPut("clmuh", "/api/v1/lov/values/" + id, value(code, "Api Adjusters Corp."))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Api Adjusters Corp."));
    // The checker is another user: the maker cannot authorize its own change.
    api.doPost("clmuh", "/api/v1/lov/values/" + id + "/authorize", Map.of())
        .andExpect(status().is4xxClientError());
    api.doPost("approver", "/api/v1/lov/values/" + id + "/authorize", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    api.doPost("clmuh", "/api/v1/lov/values/" + id + "/deactivate", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  void otherListsAndOtherUsersKeepTheirRights() throws Exception {
    api.doGet("clmuh", "/api/v1/lov/DOCUMENT_TYPE/values").andExpect(status().isForbidden());
    api.doPost("clmuh", "/api/v1/lov/DOCUMENT_TYPE/values", value("API_NOPE", "Nope"))
        .andExpect(status().isForbidden());
    api.doGet("clmofficer", ADJUSTERS).andExpect(status().isForbidden());
    api.doPost("clmofficer", ADJUSTERS, value("API_NOPE", "Nope"))
        .andExpect(status().isForbidden());
    api.doGet("clmofficer", "/api/v1/lov/BCL_CLAIM_STATUS/options")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(18));
    api.doGet("badmin", "/api/v1/lov/types")
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.code == 'BCL_CLAIM_STATUS')].ownerPermission").value("BCL_SETUP"));
    api.doPost("clmofficer", "/api/v1/lov/values/999999999/deactivate", Map.of())
        .andExpect(status().isForbidden());
  }
}
