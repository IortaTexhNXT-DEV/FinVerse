package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.nbadmin.service.PasswordExpiryNoticeJob;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Sign-in and passwords of BRD-11 wave U1-B (FR-UA-003, FR-UA-005; UAM-NFR-36, 37): the forced
 * change after a reset or expiry in the login response, the history and minimum age rules, the
 * single-use "Forgot password?" link, the password expiry notice and DIRECTORY mode without its
 * adapter.
 */
@IntegrationTest
class SignInPasswordIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String INITIAL = "Initial!Passw0rd";
  private static final String P1 = "First!Passw0rd1";
  private static final String P2 = "Second!Passw0rd2";
  private static final String P3 = "Third!Passw0rd3";
  private static final String CHANGE = "/api/v1/auth/change-password";
  private static final String BEARER = "Bearer ";
  private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService admin;
  @Autowired private SystemParameterService parameters;
  @Autowired private PasswordExpiryNoticeJob expiryJob;
  @Autowired private AsUser as;

  private static String unique() {
    return "u1b" + IDS.incrementAndGet();
  }

  private String newUser(String email) {
    String username = unique();
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Sign-in Tester", email, null, null, Set.of("AUDITOR"), true),
                INITIAL));
    return username;
  }

  private ResultActions login(String username, String password) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", username, "password", password))));
  }

  private String token(String username, String password) throws Exception {
    JsonNode body = read(login(username, password).andExpect(status().isOk()));
    return body.get("accessToken").asText();
  }

  private JsonNode read(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private ResultActions change(String token, String current, String next) throws Exception {
    return mvc.perform(
        post(CHANGE)
            .header(HttpHeaders.AUTHORIZATION, BEARER + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                json.writeValueAsString(Map.of("currentPassword", current, "newPassword", next))));
  }

  private ResultActions anonymous(String path, Map<String, String> body) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/password-reset/" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
  }

  private void ageThePassword(String username, int days) {
    jdbc.update(
        "update sec_user set password_changed_at = now() - make_interval(days => ?)"
            + " where username = ?",
        days,
        username);
  }

  @Test
  void theLoginAsksForAChangeAfterAResetAndOnceThePasswordExpires() throws Exception {
    String username = newUser(null);
    JsonNode first = read(login(username, INITIAL).andExpect(status().isOk()));
    assertThat(first.get("mustChangePassword").asBoolean()).isTrue();
    assertThat(first.get("passwordChangeReason").asText()).isEqualTo("RESET");

    String token = first.get("accessToken").asText();
    change(token, INITIAL, P1).andExpect(status().isNoContent());
    JsonNode second = read(login(username, P1).andExpect(status().isOk()));
    assertThat(second.get("mustChangePassword").asBoolean()).isFalse();

    ageThePassword(username, 91);
    JsonNode expired = read(login(username, P1).andExpect(status().isOk()));
    assertThat(expired.get("passwordChangeReason").asText()).isEqualTo("EXPIRED");
    mvc.perform(
            get("/api/v1/auth/password-status")
                .header(HttpHeaders.AUTHORIZATION, BEARER + expired.get("accessToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changeDue").value(true))
        .andExpect(jsonPath("$.historyCount").value(8))
        .andExpect(jsonPath("$.authMode").value("LOCAL"));
  }

  @Test
  void historyAndMinimumAgeAreEnforced() throws Exception {
    String username = newUser(null);
    String token = token(username, INITIAL);
    change(token, INITIAL, P1).andExpect(status().isNoContent());
    change(token, P1, P2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PASSWORD_CHANGED_TOO_SOON"))
        .andExpect(jsonPath("$.detail").value("You changed your password less than a day ago"));

    ageThePassword(username, 2);
    change(token, P1, P2).andExpect(status().isNoContent());
    ageThePassword(username, 2);
    change(token, P2, P1)
        .andExpect(jsonPath("$.code").value("PASSWORD_REUSED"))
        .andExpect(
            jsonPath("$.detail").value("You used this password recently. Choose another one"));
    change(token, P2, "Weak").andExpect(status().isBadRequest());
    change(token, "wrong", P3).andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    change(token, P2, P3).andExpect(status().isNoContent());
    login(username, P3).andExpect(status().isOk());
  }

  @Test
  void theResetLinkWorksOnceAndNotAfterItExpires() throws Exception {
    String username = newUser("reset-" + IDS.get() + "@example.ph");
    anonymous("request", Map.of("userId", username)).andExpect(status().isAccepted());
    String link = latestLink(username);
    anonymous("check", Map.of("token", link))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(username));
    anonymous("confirm", Map.of("token", link, "newPassword", P1))
        .andExpect(status().isNoContent());
    JsonNode signedIn = read(login(username, P1).andExpect(status().isOk()));
    assertThat(signedIn.get("mustChangePassword").asBoolean()).isFalse();
    anonymous("confirm", Map.of("token", link, "newPassword", P2))
        .andExpect(jsonPath("$.code").value("RESET_LINK_INVALID"));

    anonymous("request", Map.of("userId", username)).andExpect(status().isAccepted());
    String second = latestLink(username);
    jdbc.update(
        "update sec_password_reset_token set requested_at = now() - interval '2 hour',"
            + " expires_at = now() - interval '1 hour' where lower(username) = lower(?)"
            + " and used_at is null",
        username);
    anonymous("check", Map.of("token", second))
        .andExpect(jsonPath("$.code").value("RESET_LINK_EXPIRED"));

    int before = resetMails();
    anonymous("request", Map.of("userId", "nobody-" + IDS.get())).andExpect(status().isAccepted());
    assertThat(resetMails()).isEqualTo(before);
  }

  @Test
  void usersWhosePasswordExpiresSoonAreTold() {
    String username = newUser("expiry-" + IDS.get() + "@example.ph");
    ageThePassword(username, 85);
    expiryJob.execute(LocalDate.now());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ?"
                    + " and title = 'Your password expires soon'",
                Integer.class,
                username))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = ? and recipients like ?",
                Integer.class,
                PasswordExpiryNoticeJob.JOB_NAME,
                "expiry-%"))
        .isPositive();
  }

  @Test
  void directoryModeWithoutItsAdapterRefusesSignInWithAServiceMessage() throws Exception {
    String username = newUser(null);
    jdbc.update(
        "update sec_user set windows_id = ? where username = ?", "WIN\\" + username, username);
    setMode(AuthMode.DIRECTORY);
    try {
      login("WIN\\" + username, INITIAL)
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("SIGN_IN_UNAVAILABLE"));
      login("WIN\\nobody", INITIAL).andExpect(status().isUnauthorized());
      anonymous("request", Map.of("userId", username)).andExpect(status().isAccepted());
    } finally {
      setMode(AuthMode.LOCAL);
    }
    login(username, INITIAL).andExpect(status().isOk());
  }

  private void setMode(AuthMode mode) {
    as.run(
        "admin",
        () -> {
          parameters.update(AuthMode.PARAMETER, mode.name());
          return null;
        });
  }

  private String latestLink(String username) {
    String body =
        jdbc.queryForObject(
            "select body from msg_outbound where purpose = 'PASSWORD_RESET' and entity_id = ?"
                + " order by id desc limit 1",
            String.class,
            username);
    Matcher m = TOKEN.matcher(body);
    assertThat(m.find()).isTrue();
    return m.group(1);
  }

  private int resetMails() {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from msg_outbound where purpose = 'PASSWORD_RESET'", Integer.class);
    return count == null ? 0 : count;
  }
}
