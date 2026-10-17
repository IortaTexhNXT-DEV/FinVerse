package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.SignInPasswords;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BDOI FRS FRUM.001.03, FRUM.001.04 and FRUM.008.01: the inactivity warning and the time-out are
 * recorded in the audit trail as Inactivity and Timeout, with the source address and the role of
 * the user; the session policy gives the web client BDOI's dialog and the time-out page.
 */
@IntegrationTest
class SessionInactivityAuditIT {

  private static final String BEARER = "Bearer ";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private SignInPasswords passwords;

  @Test
  void theSessionPolicyCarriesBdoisDialogAndTheTimeoutPage() throws Exception {
    String token = login("auditor");
    mvc.perform(
            get("/api/v1/system/session-policy").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeoutMinutes").value(30))
        .andExpect(jsonPath("$.idleWarningMinutes").value(15))
        .andExpect(jsonPath("$.bdoiDialog").value(true))
        .andExpect(jsonPath("$.timeoutPage").value(true));
  }

  @Test
  void inactivityAndTimeoutAreAuditedWithTheAddressAndTheRole() throws Exception {
    String token = login("auditor");
    mvc.perform(
            post("/api/v1/auth/session/inactivity")
                .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                .with(r -> remote(r)))
        .andExpect(status().isNoContent());
    Map<String, Object> inactivity = latest("INACTIVITY");
    assertThat(inactivity.get("summary"))
        .isEqualTo("Inactive for 15 minutes; the inactivity warning was shown");
    assertThat(inactivity.get("ip_address")).isEqualTo("10.20.30.40");
    assertThat((String) inactivity.get("role_names")).isNotBlank();

    mvc.perform(
            post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                .param("reason", "IDLE_TIMEOUT"))
        .andExpect(status().isNoContent());
    assertThat(latest("TIMEOUT").get("summary")).isEqualTo("Session timed out after inactivity");
  }

  private static org.springframework.mock.web.MockHttpServletRequest remote(
      org.springframework.mock.web.MockHttpServletRequest request) {
    request.setRemoteAddr("10.20.30.40");
    return request;
  }

  private Map<String, Object> latest(String action) {
    return jdbc.queryForMap(
        "select summary, ip_address, role_names from audit_log where action = ?"
            + " and entity_id = 'auditor' order by id desc limit 1",
        action);
  }

  private String login(String username) throws Exception {
    String body =
        mvc.perform(passwords.login(username))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }
}
