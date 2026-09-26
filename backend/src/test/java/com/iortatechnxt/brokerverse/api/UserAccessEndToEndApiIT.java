package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.PersonaMenus;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * User Access Maintenance (BRD-11) end to end through the HTTP API with the SIT/UAT users (wave
 * U2): the Requestor enrols a new user as Screening Investigator and the Approver approves; the
 * user signs in with the temporary password, must change it, and holds exactly the investigator's
 * permissions (the menu of the web client is built from them); a change of the user's group
 * profiles submitted outside working hours needs the Second Approver; the Business Administrator's
 * group-profile request is implemented by the System Administrator; the deactivation ends the
 * user's sessions at once; the User Access Audit Log shows each change with its from and to values
 * and request number (BRD 1.002-1.008, 2.002, 3.002, 3.003, 4.003.1; UAM-NFR-35, 36, 40).
 */
@IntegrationTest
class UserAccessEndToEndApiIT {

  private static final String REQUESTS = "/api/v1/nbadmin/access-requests";
  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";
  private static final String SECOND = "secapprover";
  private static final String HOURS = "/api/v1/system/parameters/UAM_WORKING_HOURS";
  private static final String NEW_PASSWORD = "Access!Passw0rd9";
  private static final String BEARER = "Bearer ";
  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;

  private static String newUserId() {
    return String.format(
        "e%09d", ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L));
  }

  @Test
  void aUserIsEnrolledSignsInChangesAndIsDeactivated() throws Exception {
    String userId = newUserId();

    // Enrolment request by the Requestor, approved by the chosen Approver.
    long enrolId =
        submit(
            REQUESTOR,
            Json.of(
                "type",
                "CREATE_USER",
                "username",
                userId,
                "fullName",
                "End To End Investigator",
                "email",
                userId + "@example.ph",
                "roleCodes",
                List.of("SCR_INVESTIGATOR"),
                "windowsId",
                "W" + userId,
                "justification",
                "Joins the screening investigators"));
    api.doPost(REQUESTOR, REQUESTS + "/" + enrolId + "/approve", Json.of())
        .andExpect(status().isForbidden());
    String temporary =
        api.read(
                api.doPost(
                        APPROVER, REQUESTS + "/" + enrolId + "/approve", Json.of("comment", "ok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.request.status").value("APPROVED")))
            .get("temporaryPassword")
            .asText();
    assertThat(temporary).isNotBlank();

    // First sign-in: the temporary password must be changed; the token carries the role's rights.
    JsonNode first = login(userId, temporary);
    assertThat(first.get("mustChangePassword").asBoolean()).isTrue();
    assertThat(first.get("passwordChangeReason").asText()).isEqualTo("RESET");
    bearer(
            post("/api/v1/auth/change-password"),
            first.get("accessToken").asText(),
            Json.of("currentPassword", temporary, "newPassword", NEW_PASSWORD))
        .andExpect(status().isNoContent());
    JsonNode second = login(userId, NEW_PASSWORD);
    assertThat(second.get("mustChangePassword").asBoolean()).isFalse();
    String token = second.get("accessToken").asText();
    assertThat(permissionsOf(token))
        .isEqualTo(PersonaMenus.load().get("SCR_INVESTIGATOR").permissions());
    bearer(get("/api/v1/screening/cases?companyId=" + data.company().getId()), token, null)
        .andExpect(status().isOk());
    bearer(get("/api/v1/screening/str?companyId=" + data.company().getId()), token, null)
        .andExpect(status().isForbidden());
    bearer(get(REQUESTS), token, null).andExpect(status().isForbidden());
    api.doGet("admin", "/api/v1/admin/sessions?open=true&username=" + userId)
        .andExpect(jsonPath("$.content.length()", greaterThan(0)));

    String modifyNo = modifyOutsideWorkingHours(userId);
    assertThat(permissionsOf(login(userId, NEW_PASSWORD).get("accessToken").asText()))
        .contains("SCR_CASE_ASSIGN", "SCR_REPORT_VIEW");
    String profileNo = groupProfileImplemented();

    // Deactivation: approved, applied, and every open session ends at once.
    long disableId =
        submit(
            REQUESTOR,
            Json.of(
                "type", "DISABLE_USER",
                "username", userId,
                "reasonCode", "RESIGNED",
                "justification", "Resigned"));
    String disableNo =
        api.read(
                api.doPost(
                        APPROVER, REQUESTS + "/" + disableId + "/approve", Json.of("comment", "ok"))
                    .andExpect(jsonPath("$.request.status").value("APPROVED")))
            .get("request")
            .get("requestNo")
            .asText();
    api.doGet("admin", "/api/v1/admin/sessions?open=true&username=" + userId)
        .andExpect(jsonPath("$.content.length()").value(0));
    api.doGet("admin", "/api/v1/admin/sessions?username=" + userId)
        .andExpect(jsonPath("$.content[*].endReason", hasItem("ADMIN_ENDED")));
    bearer(get("/api/v1/auth/me"), token, null).andExpect(status().isUnauthorized());
    login(userId, NEW_PASSWORD, status().is4xxClientError());

    assertTheAuditLog(userId, modifyNo, disableNo);
    assertThat(auditLog(null)).contains(profileNo);
  }

  /** A group-profile change submitted outside UAM_WORKING_HOURS needs a second approval. */
  private String modifyOutsideWorkingHours(String userId) throws Exception {
    String hours = parameter("UAM_WORKING_HOURS");
    String tomorrow = LocalDate.now(MANILA).plusDays(1).getDayOfWeek().name().substring(0, 3);
    setHours("00:00-24:00," + tomorrow);
    try {
      long id =
          submit(
              REQUESTOR,
              Json.of(
                  "type",
                  "MODIFY_USER",
                  "username",
                  userId,
                  "roleCodes",
                  List.of("SCR_INVESTIGATOR", "UNIT_COMPLIANCE_COORD"),
                  "justification",
                  "Covers the unit coordinator desk"));
      api.doGet(REQUESTOR, REQUESTS + "/" + id)
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
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.request.status").value("APPROVED"));
      return requestNo;
    } finally {
      setHours(hours == null ? "00:00-24:00,MON-SUN" : hours);
    }
  }

  /** The Business Administrator's new profile, approved and implemented (BRD 3.002). */
  private String groupProfileImplemented() throws Exception {
    String code = "E2E_PROFILE_" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    long id =
        submit(
            "badmin",
            Json.of(
                "type", "CREATE_ROLE",
                "roleCode", code,
                "roleName", "End-to-end profile",
                "privilegeLevel", "STANDARD",
                "permissionsAdded", List.of("SCR_VIEW", "REPORT_VIEW"),
                "justification", "Read-only screening profile"));
    api.doPost(APPROVER, REQUESTS + "/" + id + "/approve", Json.of("comment", "ok"))
        .andExpect(jsonPath("$.request.status").value("FOR_IMPLEMENTATION"));
    api.doPost("badmin", REQUESTS + "/" + id + "/implement", null)
        .andExpect(status().isForbidden());
    return api.read(
            api.doPost("admin", REQUESTS + "/" + id + "/implement", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IMPLEMENTED")))
        .get("requestNo")
        .asText();
  }

  private void assertTheAuditLog(String userId, String modifyNo, String disableNo)
      throws Exception {
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
              assertThat(r.get("activity").asText()).startsWith("Modify User Group Profile");
              assertThat(r.get("fromValue").asText()).isEqualTo("Screening Investigator");
              assertThat(r.get("toValue").asText()).contains("Unit Compliance Coordinator");
              assertThat(r.get("requestNo").asText()).isEqualTo(modifyNo);
              assertThat(r.get("approvedBy").asText()).isIn(APPROVER, SECOND);
            });
    assertThat(rows)
        .anySatisfy(
            r -> {
              assertThat(r.get("activity").asText()).startsWith("Deactivate User");
              assertThat(r.get("requestNo").asText()).isEqualTo(disableNo);
            });
  }

  private String auditLog(String userId) throws Exception {
    String today = LocalDate.now(MANILA).toString();
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

  /** Creates and submits a request to the seed Approver; returns its id. */
  private long submit(String user, Map<String, Object> body) throws Exception {
    body.put("approvers", List.of(APPROVER));
    return api.read(
            api.doPost(user, REQUESTS, body)
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

  private void setHours(String value) throws Exception {
    api.doPut("admin", HOURS, Json.of("value", value)).andExpect(status().isOk());
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
