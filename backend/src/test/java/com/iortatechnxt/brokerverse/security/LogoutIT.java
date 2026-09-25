package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.SecurityProperties;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Logout (UAM BRD-11): the token is revoked on every instance (denylist keyed by {@code jti}) and
 * the logout is audited; tokens issued before token ids existed stay valid until they expire.
 */
@IntegrationTest
class LogoutIT {

  private static final String BEARER = "Bearer ";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private JwtTokenService tokens;
  @Autowired private SecurityProperties properties;

  @Test
  void logoutRevokesTheTokenAndIsAudited() throws Exception {
    String token = login("auditor");
    assertThat(tokens.parse(token).orElseThrow().tokenId()).isNotBlank();
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Correlation-Id"));
    long before = logouts("auditor");

    mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, BEARER + token))
        .andExpect(status().isUnauthorized());
    assertThat(logouts("auditor")).isEqualTo(before + 1);

    String another = login("auditor");
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + another))
        .andExpect(status().isOk());
  }

  @Test
  void aTokenWithoutTokenIdIsAcceptedUntilItExpires() throws Exception {
    String legacy =
        Jwts.builder()
            .issuer("inxt-brokerverse")
            .subject("auditor")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8)))
            .compact();
    assertThat(tokens.parse(legacy).orElseThrow().tokenId()).isNull();
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + legacy))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, BEARER + legacy))
        .andExpect(status().isNoContent());
    assertThat(tokens.parse("not-a-token")).isEmpty();
  }

  private String login(String username) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"Brokerverse@2026\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  private long logouts(String username) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from audit_log where action = 'LOGOUT' and entity_id = ?",
            Long.class,
            username);
    return count == null ? 0 : count;
  }
}
