package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Object-level access on the User Access APIs (OWASP API1, IDOR): ids of users, sessions and group
 * profiles reach nobody without the right, and the auditor reads without changing anything.
 */
@IntegrationTest
class ObjectAccessIT {

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;

  private long adminId() {
    return jdbc.queryForObject("select id from sec_user where username = 'admin'", Long.class);
  }

  @Test
  void aUserWithoutTheRightReachesNoOtherUserById() throws Exception {
    long admin = adminId();
    for (String path :
        new String[] {
          "/api/v1/admin/users",
          "/api/v1/admin/users/" + admin + "/group-profiles",
          "/api/v1/admin/users/" + admin + "/access-history",
          "/api/v1/admin/sessions?username=admin",
          "/api/v1/admin/identity/users/admin/profile"
        }) {
      api.doGet("requestor", path).andExpect(status().isForbidden());
    }
    api.doPost("requestor", "/api/v1/admin/sessions/users/admin/end", null)
        .andExpect(status().isForbidden());
    api.doPost("requestor", "/api/v1/admin/users/" + admin + "/unlock", null)
        .andExpect(status().isForbidden());
  }

  @Test
  void ownSessionsOnlyAreListedToTheUser() throws Exception {
    JsonNode page = api.read(api.doGet("requestor", "/api/v1/auth/sessions?page=0&size=50"));
    page.path("content")
        .forEach(s -> assertThat(s.path("username").asText()).isEqualToIgnoringCase("requestor"));
  }

  @Test
  void theAuditorReadsTheUsersButChangesNothing() throws Exception {
    long admin = adminId();
    api.doGet("uamauditor", "/api/v1/admin/users").andExpect(status().isOk());
    api.doGet("uamauditor", "/api/v1/admin/users/" + admin + "/group-profiles")
        .andExpect(status().isOk());
    api.doGet("uamauditor", "/api/v1/admin/users/" + admin + "/data-scope")
        .andExpect(status().isOk());
    api.doGet("uamauditor", "/api/v1/admin/sessions?username=admin").andExpect(status().isOk());
    api.doPost("uamauditor", "/api/v1/admin/users/" + admin + "/unlock", null)
        .andExpect(status().isForbidden());
    api.doPost("uamauditor", "/api/v1/admin/sessions/users/admin/end", null)
        .andExpect(status().isForbidden());
    api.doPut(
            "uamauditor",
            "/api/v1/admin/users/" + admin + "/data-scope",
            Json.of("allCompanies", true))
        .andExpect(status().isForbidden());
    api.doPost(
            "uamauditor",
            "/api/v1/admin/users/" + admin + "/reset-password",
            Json.of("newPassword", "Str0ng!Passw0rd"))
        .andExpect(status().isForbidden());
    api.doGet("uamauditor", "/api/v1/admin/identity/status").andExpect(status().isForbidden());
  }
}
