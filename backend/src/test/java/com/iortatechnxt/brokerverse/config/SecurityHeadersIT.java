package com.iortatechnxt.brokerverse.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What a penetration test probes first on the user APIs: the security headers, CORS for an origin
 * not allowed, an unsupported method or body type, an unbounded page and an anonymous call, each
 * answered without detail of the server.
 */
@IntegrationTest
class SecurityHeadersIT {

  private static final String OPTIONS_PATH = "/api/v1/auth/sign-in-options";

  @Autowired private MockMvc mvc;
  @Autowired private Api api;

  @Test
  void everyAnswerCarriesTheSecurityHeaders() throws Exception {
    mvc.perform(get(OPTIONS_PATH).secure(true))
        .andExpect(status().isOk())
        .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
        .andExpect(
            header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
        .andExpect(header().string("Cache-Control", containsString("no-store")));
  }

  @Test
  void anOriginNotAllowedGetsNoCorsGrant() throws Exception {
    mvc.perform(
            options("/api/v1/auth/me")
                .header("Origin", "https://attacker.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void anonymousCallsAreRefusedWithoutDetail() throws Exception {
    mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer not-a-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void wrongMethodsTypesAndPagesAreAnsweredWithoutAStackTrace() throws Exception {
    api.doPost("admin", "/api/v1/admin/users", "{}")
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
        .andExpect(content().string(not(containsString("at com."))));
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("user=admin"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().string(not(containsString("Exception"))));
    api.doGet("admin", "/api/v1/admin/sessions?size=1000000")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAGE_SIZE_TOO_LARGE"));
    api.doGet("admin", "/api/v1/admin/sessions?size=50").andExpect(status().isOk());
  }
}
