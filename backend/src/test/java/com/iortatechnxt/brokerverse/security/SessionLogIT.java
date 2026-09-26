package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import com.iortatechnxt.brokerverse.security.service.AuthSessionSweepJob;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The session log of BRD-11 wave U1-B (UAM-NFR-35; FR-UA-002, FR-UA-004): sessions listed, ended by
 * an administrator (the token is refused), by the inactivity sign-out, by the sweep (idle, expired)
 * and by a lock; the users online; the own contact details on My Profile (UQ17).
 */
@IntegrationTest
class SessionLogIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String PASSWORD = "Brokerverse@2026";
  private static final String INITIAL = "Initial!Passw0rd";
  private static final String BEARER = "Bearer ";
  private static final String ME = "/api/v1/auth/me";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private JwtTokenService tokens;
  @Autowired private UserSessionLog sessions;
  @Autowired private UserAdminService admin;
  @Autowired private AuthSessionSweepJob sweep;

  private String login(String username, String password) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  private String sessionId(String token) {
    return tokens.parse(token).orElseThrow().tokenId();
  }

  private UserSession session(String id) {
    return sessions.find(id).orElseThrow();
  }

  @Test
  void anAdministratorListsAndEndsSessionsAndTheTokenIsRefused() throws Exception {
    String token = login("auditor", PASSWORD);
    String id = sessionId(token);
    api.doGet("admin", "/api/v1/admin/sessions/online")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@ == 'auditor')]").exists());
    api.doGet("admin", "/api/v1/admin/sessions?username=auditor&open=true&size=200")
        .andExpect(jsonPath("$.content[?(@.sessionId == '" + id + "')].open").value(true));

    api.doPost("admin", "/api/v1/admin/sessions/" + id + "/end", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.endReason").value("ADMIN_ENDED"));
    mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isUnauthorized());
    api.doPost("admin", "/api/v1/admin/sessions/" + id + "/end", null)
        .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ENDED"));
    api.doGet("auditor", "/api/v1/admin/sessions").andExpect(status().isForbidden());
  }

  @Test
  void theInactivitySignOutIsRecordedWithItsReason() throws Exception {
    String token = login("auditor", PASSWORD);
    mvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].username").value("auditor"));
    mvc.perform(
            post("/api/v1/auth/logout")
                .param("reason", "IDLE_TIMEOUT")
                .header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isNoContent());
    assertThat(session(sessionId(token)).getEndReason()).isEqualTo(SessionEndReason.IDLE_TIMEOUT);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where action = 'LOGOUT' and entity_id = 'auditor'"
                    + " and summary = 'Logged out after inactivity'",
                Integer.class))
        .isPositive();
  }

  @Test
  void theSweepEndsIdleAndExpiredSessions() throws Exception {
    String idle = sessionId(login("auditor", PASSWORD));
    String expired = sessionId(login("auditor", PASSWORD));
    String active = sessionId(login("auditor", PASSWORD));
    jdbc.update(
        "update sec_user_session set last_seen_at = now() - interval '2 hour' where session_id = ?",
        idle);
    jdbc.update(
        "update sec_user_session set issued_at = now() - interval '10 hour',"
            + " last_seen_at = now() - interval '9 hour', expires_at = now() - interval '2 hour'"
            + " where session_id = ?",
        expired);
    sweep.execute(LocalDate.now());
    assertThat(session(idle).getEndReason()).isEqualTo(SessionEndReason.IDLE_TIMEOUT);
    assertThat(session(expired).getEndReason()).isEqualTo(SessionEndReason.EXPIRED);
    assertThat(session(active).getEndedAt()).isNull();
  }

  @Test
  void aLockEndsTheOpenSessionsOfTheUser() throws Exception {
    String username = "u1bs" + IDS.incrementAndGet();
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Session Tester", null, null, null, Set.of("AUDITOR"), true),
                INITIAL));
    String open = sessionId(login(username, INITIAL));
    for (int i = 0; i < 3; i++) {
      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      json.writeValueAsString(
                          Map.of("username", username, "password", "wrong-password"))))
          .andExpect(status().isUnauthorized());
    }
    assertThat(session(open).getEndReason()).isEqualTo(SessionEndReason.LOCKED);
  }

  @Test
  void usersChangeTheirOwnContactDetails() throws Exception {
    String username = "u1bp" + IDS.incrementAndGet();
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Profile Tester", null, null, null, Set.of("AUDITOR"), true),
                INITIAL));
    String token = login(username, INITIAL);
    mvc.perform(
            put(ME)
                .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("email", username + "@example.ph", "mobileNo", "+63 917 123 4567"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mobileNo").value("+63 917 123 4567"));
    mvc.perform(
            put(ME)
                .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "not-an-address", "mobileNo", "x"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.email").exists())
        .andExpect(jsonPath("$.errors.mobileNo").exists());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_access_change_log where subject = ?"
                    + " and activity = 'MODIFY_USER' and attribute in ('email', 'mobileNo')"
                    + " and done_by = ?",
                Integer.class,
                username,
                username))
        .isEqualTo(2);
  }
}
