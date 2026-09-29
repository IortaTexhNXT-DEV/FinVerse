package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.RefreshCookies;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The token model (V1180): a short access token and a rotating refresh token in an HttpOnly cookie
 * bound to the session; the refresh stops at sign-out, idle timeout and reuse of a replaced token.
 */
@IntegrationTest
class SignInTokensIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String INITIAL = "Tokens!Passw0rd1";
  private static final String BEARER = "Bearer ";
  private static final String REFRESH = "/api/v1/auth/refresh";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService admin;
  @Autowired private AsUser as;
  @Autowired private JwtTokenService tokens;
  @Autowired private UserSessionLog sessions;
  @Autowired private SystemParameterService parameters;

  private String newUser() {
    String username = "tok" + IDS.incrementAndGet();
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Token Tester", null, null, null, Set.of("AUDITOR"), true),
                INITIAL));
    return username;
  }

  private MvcResult login(String username) throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("username", username, "password", INITIAL))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private ResultActions refresh(Cookie cookie) throws Exception {
    return mvc.perform(
        post(REFRESH).cookie(cookie).header(RefreshCookies.REQUEST_HEADER, "BrokerVerse"));
  }

  private JsonNode body(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private ResultActions me(String token) throws Exception {
    // MockMvc keeps the security context of an earlier request of the test; each check starts
    // clean.
    TestSecurityContextHolder.clearContext();
    return mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + token));
  }

  @Test
  void theAccessTokenIsShortAndRenewedWithARotatingCookie() throws Exception {
    MvcResult signedIn = login(newUser());
    JsonNode answer = body(signedIn);
    String access = answer.get("accessToken").asText();
    Instant accessEnd = Instant.parse(answer.get("accessTokenExpiresAt").asText());
    Instant sessionEnd = Instant.parse(answer.get("expiresAt").asText());
    assertThat(Duration.between(Instant.now(), accessEnd))
        .isLessThanOrEqualTo(Duration.ofMinutes(15));
    assertThat(Duration.between(Instant.now(), sessionEnd)).isGreaterThan(Duration.ofHours(7));
    String setCookie = signedIn.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .startsWith(RefreshCookies.NAME + "=")
        .contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth");
    assertThat(answer.toString())
        .doesNotContain(signedIn.getResponse().getCookie(RefreshCookies.NAME).getValue());
    Cookie first = signedIn.getResponse().getCookie(RefreshCookies.NAME);

    mvc.perform(post(REFRESH).cookie(first)).andExpect(status().isUnauthorized());

    MvcResult renewed = refresh(first).andExpect(status().isOk()).andReturn();
    String next = body(renewed).get("accessToken").asText();
    assertThat(next).isNotEqualTo(access);
    me(next).andExpect(status().isOk());
    Cookie second = renewed.getResponse().getCookie(RefreshCookies.NAME);
    assertThat(second.getValue()).isNotEqualTo(first.getValue());
    String sessionId = tokens.parse(next).orElseThrow().sessionId();
    assertThat(tokens.parse(access).orElseThrow().sessionId()).isEqualTo(sessionId);

    // The replaced cookie still works during the grace period (another tab renewing at once) ...
    refresh(first).andExpect(status().isOk());
    // ... but not later: the session is ended as the token may have been stolen.
    jdbc.update(
        "update sec_user_session set refreshed_at = now() - interval '5 minute' where session_id = ?",
        sessionId);
    refresh(first).andExpect(status().isUnauthorized());
    assertThat(sessions.find(sessionId).orElseThrow().getEndReason())
        .isEqualTo(SessionEndReason.TOKEN_REUSED);
    refresh(second).andExpect(status().isUnauthorized());
    me(next).andExpect(status().isUnauthorized());
  }

  @Test
  void theRefreshStopsAtSignOutAndAfterInactivity() throws Exception {
    String username = newUser();
    MvcResult signedIn = login(username);
    String access = body(signedIn).get("accessToken").asText();
    Cookie cookie = signedIn.getResponse().getCookie(RefreshCookies.NAME);
    MvcResult out =
        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, BEARER + access))
            .andExpect(status().isNoContent())
            .andReturn();
    assertThat(out.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    refresh(cookie).andExpect(status().isUnauthorized());

    MvcResult idle = login(username);
    String idleSession =
        tokens.parse(body(idle).get("accessToken").asText()).orElseThrow().sessionId();
    jdbc.update(
        "update sec_user_session set last_seen_at = now() - interval '3 hour' where session_id = ?",
        idleSession);
    refresh(idle.getResponse().getCookie(RefreshCookies.NAME)).andExpect(status().isUnauthorized());
    assertThat(sessions.find(idleSession).orElseThrow().getEndReason())
        .isEqualTo(SessionEndReason.IDLE_TIMEOUT);
  }

  @Test
  void theLifeOfTheAccessTokenIsAParameter() throws Exception {
    String before = parameters.get("ACCESS_TOKEN_MINUTES").getValue();
    as.run("admin", () -> parameters.update("ACCESS_TOKEN_MINUTES", "5"));
    try {
      JsonNode answer = body(login(newUser()));
      Instant accessEnd = Instant.parse(answer.get("accessTokenExpiresAt").asText());
      assertThat(Duration.between(Instant.now(), accessEnd))
          .isLessThanOrEqualTo(Duration.ofMinutes(5));
    } finally {
      as.run("admin", () -> parameters.update("ACCESS_TOKEN_MINUTES", before));
    }
  }
}
