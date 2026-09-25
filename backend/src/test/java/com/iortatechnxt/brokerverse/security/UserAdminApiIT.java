package com.iortatechnxt.brokerverse.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
class UserAdminApiIT {

  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;

  private static Map<String, Object> user(String username, List<String> roles) {
    return Json.of(
        "username",
        username,
        "fullName",
        "Test " + username,
        "email",
        username + "@example.ph",
        "authorizationLimit",
        100000,
        "roleCodes",
        roles,
        "enabled",
        true);
  }

  private void login(String username, String password, int expectedStatus) throws Exception {
    mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void userLifecycleLockoutUnlockAndPasswordReset() throws Exception {
    String username = "tester" + ThreadLocalRandom.current().nextInt(1000, 9999);
    var created =
        api.read(
            api.doPost(
                    "admin",
                    "/api/v1/admin/users",
                    Json.of(
                        "user",
                        user(username, List.of("ACCOUNTANT")),
                        "initialPassword",
                        Json.of("newPassword", STRONG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("ACCOUNTANT")));
    long id = created.get("id").asLong();

    api.doPut(
            "admin", "/api/v1/admin/users/" + id, user(username, List.of("ACCOUNTANT", "AUDITOR")))
        .andExpect(jsonPath("$.roles.length()").value(2));
    api.doPut("admin", "/api/v1/admin/users/" + id, user(username, List.of("NO_SUCH_ROLE")))
        .andExpect(jsonPath("$.code").value("UNKNOWN_ROLE"));

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
            Json.of("newPassword", "N3w!Password99"))
        .andExpect(status().isNoContent());
    login(username, "N3w!Password99", 200);
  }

  @Test
  void passwordPolicyAndDuplicatesAreEnforced() throws Exception {
    api.doPost(
            "admin",
            "/api/v1/admin/users",
            Json.of(
                "user",
                user("weakling", List.of("READ_ONLY")),
                "initialPassword",
                Json.of("newPassword", "short")))
        .andExpect(status().isBadRequest());
    api.doPost(
            "admin",
            "/api/v1/admin/users",
            Json.of(
                "user",
                user("checker", List.of("READ_ONLY")),
                "initialPassword",
                Json.of("newPassword", STRONG)))
        .andExpect(status().isConflict());
  }

  @Test
  void administratorCannotChangeOwnRoles() throws Exception {
    long adminId = api.read(api.doGet("admin", "/api/v1/auth/me")).get("id").asLong();
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
        .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE"));
  }

  @Test
  void rolesAndPermissionsCanBeMaintained() throws Exception {
    String code = "TEST_ROLE_" + ThreadLocalRandom.current().nextInt(100, 999);
    long id =
        api.read(
                api.doPost(
                        "admin",
                        "/api/v1/admin/roles",
                        Json.of(
                            "code", code, "name", "Test", "permissions", List.of("REPORT_VIEW")))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost(
            "admin",
            "/api/v1/admin/roles",
            Json.of("code", code, "name", "Test", "permissions", List.of()))
        .andExpect(status().isConflict());
    api.doPut(
            "admin",
            "/api/v1/admin/roles/" + id,
            Json.of(
                "code",
                code,
                "name",
                "Renamed",
                "permissions",
                List.of("REPORT_VIEW", "AUDIT_VIEW")))
        .andExpect(jsonPath("$.permissions.length()").value(2));
    api.doGet("admin", "/api/v1/admin/roles").andExpect(status().isOk());
    api.doGet("admin", "/api/v1/admin/permissions").andExpect(status().isOk());
    api.doGet("accountant", "/api/v1/admin/roles").andExpect(status().isForbidden());
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
