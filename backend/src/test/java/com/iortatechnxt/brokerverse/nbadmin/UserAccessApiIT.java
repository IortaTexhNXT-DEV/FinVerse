package com.iortatechnxt.brokerverse.nbadmin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The User Access endpoints over HTTP. The bulk user requests are gone: UIDM-ISC orchestrates the
 * group profiles of the users, and a request carries none.
 */
@IntegrationTest
class UserAccessApiIT {

  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";

  @Autowired private Api api;

  @Test
  void groupProfileChangesAreDraftedSubmittedAndReadOverHttp() throws Exception {
    String code = "UAM_API_" + Math.floorMod(System.nanoTime(), 1_000_000_000L);
    long id =
        api.read(
                api.doPost(
                        REQUESTOR,
                        "/api/v1/nbadmin/access-requests?draft=true",
                        Json.of(
                            "type",
                            "CREATE_ROLE",
                            "roleCode",
                            code,
                            "roleName",
                            "Api Draft",
                            "permissionsAdded",
                            List.of("REPORT_VIEW")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.roleCode").value(code)))
            .get("id")
            .asLong();
    String base = "/api/v1/nbadmin/access-requests/" + id;
    api.doPut(
            REQUESTOR,
            base,
            Json.of(
                "type",
                "CREATE_ROLE",
                "roleCode",
                code,
                "roleName",
                "Api Draft",
                "permissionsAdded",
                List.of("REPORT_VIEW"),
                "justification",
                "Reports for Marketing"))
        .andExpect(jsonPath("$.justification").value("Reports for Marketing"));
    api.doGet("badmin", base).andExpect(status().isForbidden());
    api.doGet(REQUESTOR, "/api/v1/nbadmin/approvers").andExpect(status().isNotFound());
    api.doPost(REQUESTOR, base + "/submit", Json.of("remarks", "Go"))
        .andExpect(jsonPath("$.status").value("PENDING"));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-requests?scope=MINE&text=" + code)
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doGet(APPROVER, "/api/v1/nbadmin/access-requests?scope=ALL&text=" + code)
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doGet(REQUESTOR, base + "/history")
        .andExpect(jsonPath("$[0].action").value("SAVE"))
        .andExpect(jsonPath("$[2].action").value("SUBMIT"));
    api.doPost(REQUESTOR, base + "/approve", Json.of()).andExpect(status().isForbidden());
    api.doPost(APPROVER, base + "/cancel", Json.of("comment", "x"))
        .andExpect(jsonPath("$.code").value("ACCESS_NOT_REQUESTER"));
    api.doPost(REQUESTOR, base + "/cancel", Json.of("comment", "Wrong unit"))
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.lifecycle.cancelReason").value("Wrong unit"));
    api.doGet("ao", "/api/v1/nbadmin/access-requests").andExpect(status().isForbidden());
  }

  @Test
  void aRequestAboutAUserIsRefusedAndTheBulkRequestsAreGone() throws Exception {
    for (String type : List.of("CREATE_USER", "MODIFY_USER", "DISABLE_USER", "ENABLE_USER")) {
      api.doPost(
              REQUESTOR,
              "/api/v1/nbadmin/access-requests?draft=true",
              Json.of("type", type, "justification", "Covers Marketing"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("ACCESS_USERS_BY_UIDM"));
    }
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-batches").andExpect(status().isNotFound());
  }
}
