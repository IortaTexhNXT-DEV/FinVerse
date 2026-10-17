package com.iortatechnxt.brokerverse.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Where local passwords are not used (production, or a deployment that switches them off), a
 * password sign-in, a change of password and a reset link are all refused, and the sign-in page
 * offers neither the password form nor "Forgot password?".
 */
@IntegrationTest
@TestPropertySource(properties = "brokerverse.security.local-password-sign-in=false")
class LocalPasswordSignInOffIT {

  private static final String PASSWORD = "Local!Passw0rd2026";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private UserAdminService admin;

  @Test
  void aPasswordSignInIsRefusedEvenWithTheRightPassword() throws Exception {
    String userId = "lp" + (System.nanoTime() % 100_000_000L);
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(userId, "Local Tester", null, null, null, Set.of("AUDITOR"), true),
                PASSWORD));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", userId, "password", PASSWORD))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void theSignInPageOffersNoPasswordFormAndNoReset() throws Exception {
    mvc.perform(get("/api/v1/auth/sign-in-options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.localPasswordSignIn").value(false))
        .andExpect(jsonPath("$.passwordSignIn").value(false))
        .andExpect(jsonPath("$.passwordReset").value(false));
  }

  @Test
  void theOwnPasswordCannotBeChanged() throws Exception {
    api.doGet("admin", "/api/v1/auth/password-status")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.localPassword").value(false));
    api.doPost(
            "admin",
            "/api/v1/auth/change-password",
            Map.of("currentPassword", "whatever", "newPassword", "Another!Passw0rd2026"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PASSWORD_MANAGED_BY_ORGANISATION"));
  }
}
