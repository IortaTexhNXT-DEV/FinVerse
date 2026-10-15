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
  void requestsAreDraftedSubmittedAndReadOverHttp() throws Exception {
    String name = AccessRequestIT.username();
    long id =
        api.read(
                api.doPost(
                        REQUESTOR,
                        "/api/v1/nbadmin/access-requests?draft=true",
                        Json.of(
                            "type",
                            "CREATE_USER",
                            "username",
                            name,
                            "fullName",
                            "Api Draft",
                            "windowsId",
                            "WA" + name))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.details.windowsId").value("WA" + name)))
            .get("id")
            .asLong();
    String base = "/api/v1/nbadmin/access-requests/" + id;
    api.doPut(
            REQUESTOR,
            base,
            Json.of(
                "type",
                "CREATE_USER",
                "username",
                name,
                "fullName",
                "Api Draft",
                "justification",
                "Joined Marketing"))
        .andExpect(jsonPath("$.justification").value("Joined Marketing"));
    api.doGet("badmin", base).andExpect(status().isForbidden());
    api.doGet(REQUESTOR, "/api/v1/nbadmin/approvers?subject=" + name)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.username == 'uamapprover')].fullName").exists());
    api.doPost(
            REQUESTOR, base + "/submit", Json.of("approvers", List.of(APPROVER), "remarks", "Go"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.lifecycle.assignedApprover").value(APPROVER));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-requests?scope=MINE&text=" + name)
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doGet(APPROVER, "/api/v1/nbadmin/access-requests?scope=ASSIGNED&text=" + name)
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
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-settings")
        .andExpect(jsonPath("$.directRoleEdit").value(false))
        .andExpect(jsonPath("$.roleApplyOnApproval").value(false));
    api.doGet("ao", "/api/v1/nbadmin/access-requests").andExpect(status().isForbidden());
  }

  @Test
  void aRequestCarriesNoGroupProfilesAndTheBulkRequestsAreGone() throws Exception {
    api.doPost(
            REQUESTOR,
            "/api/v1/nbadmin/access-requests?draft=true",
            Json.of(
                "type",
                "MODIFY_ROLES",
                "username",
                "mkttl",
                "roleCodes",
                List.of("MKT_AO"),
                "justification",
                "Covers Marketing"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ACCESS_GROUP_PROFILES_BY_UIDM"));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-batches").andExpect(status().isNotFound());
  }
}
