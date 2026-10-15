package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
class UserAdminApiIT {

  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService users;
  @Autowired private AsUser as;

  private void login(String username, String password, int expectedStatus) throws Exception {
    mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().is(expectedStatus));
  }

  /** A local test account as UIDM-ISC provisions it: without group profiles. */
  private long testAccount(String username) {
    return as.run(
            "admin",
            () ->
                users.createUser(
                    new UserRequest(
                        username,
                        "Test " + username,
                        username + "@example.ph",
                        null,
                        null,
                        Set.of(),
                        true),
                    STRONG))
        .getId();
  }

  @Test
  void lockoutUnlockAndPasswordResetOfALocalTestAccount() throws Exception {
    String username = "tester" + ThreadLocalRandom.current().nextInt(1000, 9999);
    long id = testAccount(username);
    // Group profiles come from UIDM-ISC (here: SQL standing in for it).
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        id,
        "ACCOUNTANT");

    // LOGIN_MAX_FAILED_ATTEMPTS = 3 (BDOI NFR, V1000): the third failure locks the account.
    for (int i = 0; i < 3; i++) {
      login(username, "wrong-password", 401);
    }
    login(username, STRONG, 401);
    api.doPost("admin", "/api/v1/admin/users/" + id + "/unlock", null)
        .andExpect(jsonPath("$.locked").value(false));
    login(username, STRONG, 200);

    api.doPost(
            "admin",
            "/api/v1/admin/users/" + id + "/reset-password",
            Json.of("newPassword", "short"))
        .andExpect(status().isBadRequest());
    api.doPost(
            "admin",
            "/api/v1/admin/users/" + id + "/reset-password",
            Json.of("newPassword", "N3w!Password99"))
        .andExpect(status().isNoContent());
    login(username, "N3w!Password99", 200);
  }

  @Test
  void usersAreNotCreatedNorChangedThroughTheApi() throws Exception {
    long adminId = api.read(api.doGet("admin", "/api/v1/auth/me")).get("id").asLong();
    api.doPost(
            "admin",
            "/api/v1/admin/users",
            Json.of(
                "user",
                Json.of("username", "direct", "fullName", "Direct", "enabled", true),
                "initialPassword",
                Json.of("newPassword", STRONG)))
        .andExpect(status().is4xxClientError());
    api.doPut(
            "admin",
            "/api/v1/admin/users/" + adminId,
            Json.of(
                "username",
                "admin",
                "fullName",
                "Admin",
                "roleCodes",
                List.of("SYSADMIN", "FIN_MANAGER"),
                "enabled",
                true))
        .andExpect(status().is4xxClientError());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_user where username = 'direct'", Integer.class))
        .isZero();
  }

  /**
   * Roles change only by implementing an approved group-profile request (PQ17; UAM_DIRECT_ROLE_EDIT
   * = false since V1062): create through the Roles screen with the request number, change through
   * "Implement request".
   */
  @Test
  void rolesAndPermissionsAreMaintainedThroughApprovedRequests() throws Exception {
    String code = "TEST_ROLE_" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    api.doPost(
            "admin",
            "/api/v1/admin/roles",
            Json.of("code", code, "name", "Test", "permissions", List.of("REPORT_VIEW")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ROLE_EDIT_BY_REQUEST"));

    String createNo =
        approvedRequest(
            Json.of(
                "type",
                "CREATE_ROLE",
                "roleCode",
                code,
                "roleName",
                "Test",
                "permissionsAdded",
                List.of("REPORT_VIEW"),
                "justification",
                "New test profile",
                "approvers",
                List.of("uamapprover")));
    long id =
        api.read(
                api.doPost(
                        "admin",
                        "/api/v1/admin/roles?requestNo=" + createNo,
                        Json.of(
                            "code", code, "name", "Test", "permissions", List.of("REPORT_VIEW")))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost(
            "admin",
            "/api/v1/admin/roles?requestNo=" + createNo,
            Json.of("code", code, "name", "Test", "permissions", List.of()))
        .andExpect(jsonPath("$.code").value("ROLE_REQUEST_NOT_APPROVED"));
    api.doPut(
            "admin",
            "/api/v1/admin/roles/" + id,
            Json.of("code", code, "name", "Renamed", "permissions", List.of("REPORT_VIEW")))
        .andExpect(jsonPath("$.code").value("ROLE_EDIT_BY_REQUEST"));

    long changeId =
        api.read(
                api.doPost(
                    "badmin",
                    "/api/v1/nbadmin/access-requests",
                    Json.of(
                        "type",
                        "MODIFY_ROLE_PERMISSIONS",
                        "roleCode",
                        code,
                        "roleName",
                        "Renamed",
                        "permissionsAdded",
                        List.of("AUDIT_VIEW"),
                        "justification",
                        "Audit read access",
                        "approvers",
                        List.of("uamapprover"))))
            .get("id")
            .asLong();
    api.doPost(
            "uamapprover",
            "/api/v1/nbadmin/access-requests/" + changeId + "/approve",
            Json.of("comment", "ok"))
        .andExpect(jsonPath("$.request.status").value("FOR_IMPLEMENTATION"));
    api.doPost("admin", "/api/v1/nbadmin/access-requests/" + changeId + "/implement", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IMPLEMENTED"));
    JsonNode role = null;
    for (JsonNode r : api.read(api.doGet("admin", "/api/v1/admin/roles"))) {
      if (code.equals(r.get("code").asText())) {
        role = r;
      }
    }
    assertThat(role).isNotNull();
    assertThat(role.get("name").asText()).isEqualTo("Renamed");
    assertThat(role.get("permissions")).hasSize(2);
    api.doGet("admin", "/api/v1/admin/permissions").andExpect(status().isOk());
    api.doGet("accountant", "/api/v1/admin/roles").andExpect(status().isForbidden());
  }

  private String approvedRequest(Map<String, Object> body) throws Exception {
    long id =
        api.read(
                api.doPost("badmin", "/api/v1/nbadmin/access-requests", body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING")))
            .get("id")
            .asLong();
    return api.read(
            api.doPost(
                    "uamapprover",
                    "/api/v1/nbadmin/access-requests/" + id + "/approve",
                    Json.of("comment", "ok"))
                .andExpect(jsonPath("$.request.status").value("FOR_IMPLEMENTATION")))
        .get("request")
        .get("requestNo")
        .asText();
  }

  @Test
  void ownPasswordChangeVerifiesCurrentPassword() throws Exception {
    api.doPost(
            "auditor",
            "/api/v1/auth/change-password",
            Json.of("currentPassword", "wrong", "newPassword", STRONG))
        .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
  }
}
