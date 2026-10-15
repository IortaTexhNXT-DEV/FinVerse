package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.PersonaMenus;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * User Access Maintenance (BRD-11) end to end through the HTTP API with the SIT/UAT users: UIDM-ISC
 * creates a user (joiner) and makes it a Screening Investigator over the provisioning interface
 * (SCIM); the user signs in with a password set by the administrator (local test account), must
 * change it, and holds exactly the investigator's permissions (the menu of the web client is built
 * from them); a group-profile change proposed outside working hours needs the Second Approver; a
 * new group profile is approved and implemented; UIDM-ISC changes the user's group profiles at once
 * and deactivates the user (leaver), which ends the user's sessions at once; the User Access Audit
 * Log shows each change with its from and to values, request number and who did it (BRD 3.002,
 * 3.003, 4.003.1; UAM-NFR-35, 36, 40).
 */
@IntegrationTest
class UserAccessEndToEndApiIT {

  private static final String REQUESTS = "/api/v1/nbadmin/access-requests";
  private static final String SCIM_USERS = "/integration/v1/scim/v2/Users";
  private static final String MAKER = "badmin";
  private static final String APPROVER = "uamapprover";
  private static final String SECOND = "secapprover";
  private static final String HOURS = "/api/v1/system/parameters/UAM_WORKING_HOURS";
  private static final String TEMPORARY = "Temporary!Passw0rd1";
  private static final String NEW_PASSWORD = "Access!Passw0rd9";
  private static final String BEARER = "Bearer ";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private static String newUserId() {
    return String.format(
        "e%09d", ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L));
  }

  private static MockHttpServletRequestBuilder uidm(MockHttpServletRequestBuilder request) {
    return request.with(
        jwt()
            .jwt(j -> j.subject("uidm-isc"))
            .authorities(new SimpleGrantedAuthority("SCOPE_identity.provision")));
  }

  @Test
  void aUserFromUidmIscSignsInChangesProfilesAndIsDeactivated() throws Exception {
    String userId = newUserId();
    joiner(userId);
    uidmAdds("SCR_INVESTIGATOR", userId, "UIDM-E2E-" + userId + "-1");

    // A local test account: the administrator sets a password that must be changed.
    Long id = jdbc.queryForObject("select id from sec_user where username = ?", Long.class, userId);
    api.doPost(
            "admin",
            "/api/v1/admin/users/" + id + "/reset-password",
            Json.of("newPassword", TEMPORARY))
        .andExpect(status().isNoContent());
    JsonNode first = login(userId, TEMPORARY);
    assertThat(first.get("mustChangePassword").asBoolean()).isTrue();
    bearer(
            post("/api/v1/auth/change-password"),
            first.get("accessToken").asText(),
            Json.of("currentPassword", TEMPORARY, "newPassword", NEW_PASSWORD))
        .andExpect(status().isNoContent());
    String token = login(userId, NEW_PASSWORD).get("accessToken").asText();
    assertThat(permissionsOf(token))
        .isEqualTo(PersonaMenus.load().get("SCR_INVESTIGATOR").permissions());
    bearer(get("/api/v1/screening/cases?companyId=" + data.company().getId()), token, null)
        .andExpect(status().isOk());
    bearer(get("/api/v1/screening/str?companyId=" + data.company().getId()), token, null)
        .andExpect(status().isForbidden());
    bearer(get(REQUESTS), token, null).andExpect(status().isForbidden());
    api.doGet("uamadmin", "/api/v1/admin/sessions?open=true&username=" + userId)
        .andExpect(jsonPath("$.content.length()", greaterThan(0)));

    String outsideNo = profileChangeOutsideWorkingHours();
    uidmAdds("UNIT_COMPLIANCE_COORD", userId, "UIDM-E2E-" + userId + "-2");
    assertThat(permissionsOf(login(userId, NEW_PASSWORD).get("accessToken").asText()))
        .contains("SCR_CASE_ASSIGN", "SCR_REPORT_VIEW");
    String profileNo = groupProfileImplemented();

    // Leaver: UIDM-ISC deactivates the user and every open session ends at once.
    mvc.perform(uidm(delete(SCIM_USERS + "/" + userId))).andExpect(status().isNoContent());
    api.doGet("uamadmin", "/api/v1/admin/sessions?open=true&username=" + userId)
        .andExpect(jsonPath("$.content.length()").value(0));
    bearer(get("/api/v1/auth/me"), token, null).andExpect(status().isUnauthorized());
    // A deactivated user gets the one sign-in failure message (the audit trail has the reason),
    // and the attempt does not count towards the lock-out.
    assertThat(login(userId, NEW_PASSWORD, status().isUnauthorized()).get("detail").asText())
        .isEqualTo("Invalid user name or password");

    assertTheAuditLog(userId);
    assertThat(auditLog(null)).contains(profileNo, outsideNo);
  }

  /** UIDM-ISC creates the user (joiner) over the provisioning interface. */
  private void joiner(String userId) throws Exception {
    mvc.perform(
            uidm(post(SCIM_USERS))
                .contentType("application/scim+json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "schemas",
                            List.of("urn:ietf:params:scim:schemas:core:2.0:User"),
                            "userName",
                            userId,
                            "name",
                            Map.of("givenName", "End To End", "familyName", "Investigator"),
                            "emails",
                            List.of(Map.of("value", userId + "@bdo.test", "primary", true)),
                            "active",
                            true,
                            "urn:bibs:params:scim:schemas:extension:bdo:2.0:User",
                            Map.of(
                                "windowsId",
                                "BDO\\w" + userId,
                                "uidmRequestNo",
                                "UIDM-E2E-" + userId)))))
        .andExpect(status().isCreated());
  }

  /** A group-profile change proposed outside UAM_WORKING_HOURS needs a second approval. */
  private String profileChangeOutsideWorkingHours() throws Exception {
    String hours = parameter("UAM_WORKING_HOURS");
    String tomorrow =
        BusinessClock.today(Clock.systemUTC()).plusDays(1).getDayOfWeek().name().substring(0, 3);
    setHours("00:00-24:00," + tomorrow);
    try {
      long id = submit(newProfile("Moves the coordinators to read-only screening"));
      api.doGet(MAKER, REQUESTS + "/" + id)
          .andExpect(jsonPath("$.lifecycle.riskFlags", hasItem("OUTSIDE_HOURS")))
          .andExpect(jsonPath("$.lifecycle.secondApprovalRequired").value(true));
      api.doPost(APPROVER, REQUESTS + "/" + id + "/approve", Json.of("comment", "ok"))
          .andExpect(jsonPath("$.request.status").value("PENDING_SECOND"));
      api.doPost(APPROVER, REQUESTS + "/" + id + "/second-approve", Json.of("comment", "x"))
          .andExpect(status().isForbidden());
      String requestNo = api.read(api.doGet(SECOND, REQUESTS + "/" + id)).get("requestNo").asText();
      api.doGet(SECOND, "/api/v1/approvals/inbox")
          .andExpect(jsonPath("$[*].reference", hasItem(requestNo)));
      api.doPost(SECOND, REQUESTS + "/" + id + "/second-approve", Json.of("comment", "Checked"))
          .andExpect(status().isOk());
      return requestNo;
    } finally {
      setHours(hours == null ? "00:00-24:00,MON-SUN" : hours);
    }
  }

  private static Map<String, Object> newProfile(String justification) {
    String code = "E2E_PROFILE_" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    return Json.of(
        "type",
        "CREATE_ROLE",
        "roleCode",
        code,
        "roleName",
        "End-to-end profile " + code,
        "privilegeLevel",
        "STANDARD",
        "permissionsAdded",
        List.of("SCR_VIEW", "REPORT_VIEW"),
        "justification",
        justification);
  }

  /** The maker's new profile, approved and implemented (BRD 3.002). */
  private String groupProfileImplemented() throws Exception {
    long id = submit(newProfile("Read-only screening profile"));
    api.doPost(APPROVER, REQUESTS + "/" + id + "/approve", Json.of("comment", "ok"))
        .andExpect(jsonPath("$.request.status").value("FOR_IMPLEMENTATION"));
    api.doPost(MAKER, REQUESTS + "/" + id + "/implement", null).andExpect(status().isForbidden());
    return api.read(
            api.doPost("admin", REQUESTS + "/" + id + "/implement", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IMPLEMENTED")))
        .get("requestNo")
        .asText();
  }

  private void assertTheAuditLog(String userId) throws Exception {
    List<JsonNode> rows = new ArrayList<>();
    json.readTree(auditLog(userId))
        .get("rows")
        .forEach(
            r -> {
              if ("DETAIL".equals(r.get("kind").asText())) {
                rows.add(r.get("cells"));
              }
            });
    assertThat(rows)
        .anySatisfy(
            r -> {
              assertThat(r.path("activity").asText()).startsWith("Modify User Group Profile");
              assertThat(r.path("fromValue").asText()).isEqualTo("Screening Investigator");
              assertThat(r.path("toValue").asText()).contains("Unit Compliance Coordinator");
              assertThat(r.path("requestNo").asText()).isEqualTo("UIDM-E2E-" + userId + "-2");
              assertThat(r.path("doneBy").asText()).isEqualTo("UIDM-ISC");
            });
    assertThat(rows)
        .anySatisfy(
            r -> {
              assertThat(r.path("activity").asText()).startsWith("Deactivate User");
              assertThat(r.path("doneBy").asText()).isEqualTo("UIDM-ISC");
            });
  }

  private String auditLog(String userId) throws Exception {
    String today = BusinessClock.today(Clock.systemUTC()).toString();
    Map<String, String> params =
        userId == null
            ? Map.of("from", today, "to", today)
            : Map.of("from", today, "to", today, "user", userId);
    return api.doPost(APPROVER, "/api/v1/reports/UAM-AUDIT-LOG/run", params)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  /** UIDM-ISC adds the user to a group profile through the provisioning interface (SCIM). */
  private void uidmAdds(String group, String userId, String requestNo) throws Exception {
    mvc.perform(
            patch("/integration/v1/scim/v2/Groups/" + group)
                .with(
                    jwt()
                        .jwt(j -> j.subject("uidm-isc"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_identity.provision")))
                .header("X-UIDM-Request-No", requestNo)
                .contentType("application/scim+json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "Operations",
                            List.of(
                                Map.of(
                                    "op", "add",
                                    "path", "members",
                                    "value", List.of(Map.of("value", userId))))))))
        .andExpect(status().isOk());
  }

  /** The maker submits a group-profile change to the checkers; returns its id. */
  private long submit(Map<String, Object> body) throws Exception {
    return api.read(
            api.doPost(MAKER, REQUESTS, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")))
        .get("id")
        .asLong();
  }

  private String parameter(String key) throws Exception {
    for (JsonNode p : api.read(api.doGet("admin", "/api/v1/system/parameters"))) {
      if (key.equals(p.get("key").asText())) {
        return p.get("value").asText();
      }
    }
    return null;
  }

  /** UAM_WORKING_HOURS is a security parameter: the change waits for Information Security. */
  private void setHours(String value) throws Exception {
    api.doPut("admin", HOURS, Json.of("value", value))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingValue").value(value));
    api.doPost("admin", HOURS + "/approve", null).andExpect(status().isForbidden());
    api.doPost("infosec", HOURS + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(value))
        .andExpect(jsonPath("$.pendingValue").doesNotExist());
  }

  private JsonNode login(String username, String password) throws Exception {
    return login(username, password, status().isOk());
  }

  private JsonNode login(
      String username, String password, org.springframework.test.web.servlet.ResultMatcher expected)
      throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(expected)
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private ResultActions bearer(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String token,
      Object body)
      throws Exception {
    request.header(HttpHeaders.AUTHORIZATION, BEARER + token);
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
    return mvc.perform(request);
  }

  private Set<String> permissionsOf(String token) throws Exception {
    JsonNode me =
        json.readTree(
            bearer(get("/api/v1/auth/me"), token, null)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    Set<String> permissions = new TreeSet<>();
    me.get("permissions").forEach(p -> permissions.add(p.asText().toUpperCase(Locale.ROOT)));
    return permissions;
  }
}
