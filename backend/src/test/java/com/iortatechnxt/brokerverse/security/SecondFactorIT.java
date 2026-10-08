package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.RefreshCookies;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaPolicy;
import com.iortatechnxt.brokerverse.security.service.mfa.Totp;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * The second factor (V1181): enrolment of an authenticator app at the first sign-in when the policy
 * requires it, the code after the password (never the same code twice, a wrong code counts towards
 * the lockout), the recovery codes, the policy levels, and the administrator reset under four eyes.
 */
@IntegrationTest
class SecondFactorIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String INITIAL = "Second!Passw0rd1";
  private static final String BEARER = "Bearer ";
  private static final String MFA = "/api/v1/auth/mfa";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService admin;
  @Autowired private AsUser as;

  /**
   * Time step of the code that confirmed the last enrolment (the replay check reuses exactly that
   * code).
   */
  private long enrolStep;

  @Autowired private Api api;
  @Autowired private SystemParameterService parameters;

  private String policyBefore;

  @BeforeEach
  void requireTheSecondFactorOfEveryone() {
    policyBefore = parameters.get(MfaPolicy.PARAMETER).getValue();
    setPolicy("ALL");
  }

  @AfterEach
  void restorePolicy() {
    setPolicy(policyBefore);
  }

  private void setPolicy(String value) {
    as.run("admin", () -> parameters.update(MfaPolicy.PARAMETER, value));
  }

  private String newUser(String role) {
    String username = "mfa" + IDS.incrementAndGet();
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Second Factor Tester", null, null, null, Set.of(role), true),
                INITIAL));
    return username;
  }

  private JsonNode post(String path, Map<String, Object> body, int expected) throws Exception {
    MvcResult result =
        mvc.perform(
                MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body)))
            .andReturn();
    assertThat(result.getResponse().getStatus()).as(path).isEqualTo(expected);
    String content = result.getResponse().getContentAsString();
    return content.isEmpty() ? null : json.readTree(content);
  }

  private JsonNode login(String username) throws Exception {
    return post("/api/v1/auth/login", Map.of("username", username, "password", INITIAL), 200);
  }

  private static byte[] secret(JsonNode enrolment) {
    return base32(enrolment.get("secret").asText().replace(" ", ""));
  }

  private static String code(byte[] secret, int offset) {
    return Totp.code(secret, Totp.step(Instant.now()) + offset);
  }

  /** Enrols a user at sign-in and returns the secret. */
  private byte[] enrol(String username) throws Exception {
    JsonNode first = login(username);
    assertThat(first.get("mfaStep").asText()).isEqualTo("ENROL");
    assertThat(first.has("accessToken")).isFalse();
    String challenge = first.get("mfaChallenge").asText();
    JsonNode enrolment = post(MFA + "/enrolment/start", Map.of("challenge", challenge), 200);
    assertThat(enrolment.get("qrCode").asText()).startsWith("data:image/svg+xml;base64,");
    assertThat(enrolment.get("otpauthUri").asText())
        .startsWith("otpauth://totp/")
        .contains("issuer=");
    byte[] secret = secret(enrolment);
    post(MFA + "/enrolment/confirm", Map.of("challenge", challenge, "code", "000000"), 422);
    enrolStep = Totp.step(Instant.now());
    MvcResult confirmed =
        mvc.perform(
                MockMvcRequestBuilders.post(MFA + "/enrolment/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("challenge", challenge, "code", Totp.code(secret, enrolStep)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recoveryCodes.length()").value(10))
            .andExpect(jsonPath("$.signIn.accessToken").isNotEmpty())
            .andReturn();
    assertThat(confirmed.getResponse().getCookie(RefreshCookies.NAME)).isNotNull();
    String stored =
        jdbc.queryForObject(
            "select secret_cipher from sec_user_mfa where username = ? and status = 'ACTIVE'",
            String.class,
            username);
    assertThat(stored)
        .startsWith("v1:")
        .doesNotContain(enrolment.get("secret").asText().replace(" ", ""));
    return secret;
  }

  @Test
  void aRequiredSecondFactorIsEnrolledThenAskedAfterThePassword() throws Exception {
    String username = newUser("AUDITOR");
    byte[] secret = enrol(username);

    JsonNode second = login(username);
    assertThat(second.get("mfaStep").asText()).isEqualTo("VERIFY");
    String challenge = second.get("mfaChallenge").asText();
    TestSecurityContextHolder.clearContext();
    MvcResult withChallenge =
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + challenge))
            .andReturn();
    assertThat(withChallenge.getResponse().getStatus())
        .as(withChallenge.getResponse().getContentAsString())
        .isEqualTo(401);
    // The code of the enrolment was used: it is never accepted again. A wrong code counts.
    post(
        MFA + "/verify", Map.of("challenge", challenge, "code", Totp.code(secret, enrolStep)), 401);
    Integer failed =
        jdbc.queryForObject(
            "select failed_attempts from sec_user where username = ?", Integer.class, username);
    assertThat(failed).isEqualTo(1);
    JsonNode verified =
        post(
            MFA + "/verify",
            Map.of("challenge", challenge, "code", Totp.code(secret, enrolStep + 1)),
            200);
    String access = verified.get("accessToken").asText();
    mvc.perform(get(MFA + "/me").header(HttpHeaders.AUTHORIZATION, BEARER + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enrolled").value(true))
        .andExpect(jsonPath("$.required").value(true))
        .andExpect(jsonPath("$.recoveryCodesLeft").value(10));
    post(MFA + "/verify", Map.of("challenge", "not-a-challenge", "code", code(secret, 1)), 401);
  }

  @Test
  void aRecoveryCodeWorksOnce() throws Exception {
    String username = newUser("AUDITOR");
    JsonNode first = login(username);
    String challenge = first.get("mfaChallenge").asText();
    byte[] secret = secret(post(MFA + "/enrolment/start", Map.of("challenge", challenge), 200));
    JsonNode confirmed =
        post(
            MFA + "/enrolment/confirm",
            Map.of("challenge", challenge, "code", code(secret, 0)),
            200);
    String recovery = confirmed.get("recoveryCodes").get(0).asText();

    String next = login(username).get("mfaChallenge").asText();
    post(MFA + "/verify", Map.of("challenge", next, "code", recovery.toUpperCase()), 200);
    post(MFA + "/verify", Map.of("challenge", next, "code", recovery), 401);
  }

  @Test
  void thePolicyDecidesWhoIsAsked() throws Exception {
    String standard = newUser("AUDITOR");
    String privileged = newUser("SYSADMIN");
    setPolicy("PRIVILEGED");
    assertThat(login(standard).get("accessToken").asText()).isNotBlank();
    assertThat(login(privileged).get("mfaStep").asText()).isEqualTo("ENROL");
    setPolicy("OFF");
    assertThat(login(privileged).get("accessToken").asText()).isNotBlank();
    setPolicy("SOMETHING_ELSE");
    assertThat(login(standard).get("mfaStep").asText()).isEqualTo("ENROL");
  }

  @Test
  void anAdministratorResetNeedsASecondAdministrator() throws Exception {
    String username = newUser("AUDITOR");
    enrol(username);
    String approver = "infosec";
    Map<String, Object> body = new HashMap<>();
    body.put("username", username);
    body.put("reason", "Phone lost");
    JsonNode request =
        api.read(
            api.doPost("admin", "/api/v1/admin/mfa/reset-requests", body)
                .andExpect(status().isCreated()));
    long id = request.get("id").asLong();
    api.doPost("admin", "/api/v1/admin/mfa/reset-requests", body)
        .andExpect(jsonPath("$.code").value("MFA_RESET_PENDING"));
    api.doPost("admin", "/api/v1/admin/mfa/reset-requests/" + id + "/approve", null)
        .andExpect(status().isForbidden());
    // An approver gives the reason of a rejection.
    api.doPost(approver, "/api/v1/admin/mfa/reset-requests/" + id + "/reject", Map.of())
        .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
    api.doGet(approver, "/api/v1/admin/mfa/users")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.username == '" + username + "')].resetPending").value(true));
    api.doPost(approver, "/api/v1/admin/mfa/reset-requests/" + id + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_user_mfa where username = ?", Integer.class, username))
        .isZero();
    assertThat(login(username).get("mfaStep").asText()).isEqualTo("ENROL");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_id = ? and summary like"
                    + " 'Approved the reset of the second factor%'",
                Integer.class, username))
        .isEqualTo(1);
  }

  private static byte[] base32(String text) {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    byte[] out = new byte[text.length() * 5 / 8];
    int buffer = 0;
    int bits = 0;
    int index = 0;
    for (char c : text.toCharArray()) {
      buffer = (buffer << 5) | alphabet.indexOf(c);
      bits += 5;
      if (bits >= 8) {
        out[index++] = (byte) (buffer >> (bits - 8));
        bits -= 8;
      }
    }
    return out;
  }
}
