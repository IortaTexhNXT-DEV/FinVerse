package com.iortatechnxt.brokerverse.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The security chain of {@code /integration/**}: API gateway tokens checked against a locally
 * generated key set (served over HTTP by the test), issuer, audience, validity and the scopes of
 * each API rule; user tokens refused there and gateway tokens refused on the user APIs.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@TestPropertySource(
    properties = {
      "brokerverse.integration.security.issuer=" + IntegrationApiSecurityIT.ISSUER,
      "brokerverse.integration.security.audiences=bibs-api,bibs-other",
      "brokerverse.integration.security.apis.ping.scopes=bibs.ping",
      "brokerverse.integration.security.apis.receipts.path=/integration/v1/receipts/**",
      "brokerverse.integration.security.apis.receipts.scopes=bibs.receipts.read,bibs.receipts.write"
    })
class IntegrationApiSecurityIT {

  static final String ISSUER = "https://gateway.bdo.example/oauth";

  private static final String BEARER = "Bearer ";
  private static final String PING = "/integration/v1/ping";
  private static final RSAKey GATEWAY_KEY = rsaKey("gateway-1");
  private static final RSAKey OTHER_KEY = rsaKey("other");
  private static final HttpServer JWKS = jwksServer();

  @Autowired private MockMvc mvc;

  @DynamicPropertySource
  static void jwks(DynamicPropertyRegistry registry) {
    registry.add(
        "brokerverse.integration.security.jwk-set-uri",
        () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
  }

  @AfterAll
  static void stopJwks() {
    JWKS.stop(0);
  }

  @Test
  void aValidGatewayTokenWithTheScopeReachesTheApi() throws Exception {
    mvc.perform(get(PING).header(HttpHeaders.AUTHORIZATION, BEARER + token(claims())))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.client").value("egl-client"))
        .andExpect(jsonPath("$.scopes[0]").value("bibs.ping"));
  }

  @Test
  void aCallWithoutTokenIsUnauthorized() throws Exception {
    mvc.perform(get(PING))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
  }

  @Test
  void tokensFailingIssuerAudienceValidityOrSignatureAreUnauthorized() throws Exception {
    Instant now = Instant.now();
    List<String> refused =
        List.of(
            token(claims().issuer("https://someone-else.example").build()),
            token(claims().audience("another-system").build()),
            token(
                claims()
                    .issueTime(Date.from(now.minus(Duration.ofHours(2))))
                    .expirationTime(Date.from(now.minus(Duration.ofHours(1))))
                    .build()),
            sign(claims().build(), OTHER_KEY));
    for (String token : refused) {
      mvc.perform(get(PING).header(HttpHeaders.AUTHORIZATION, BEARER + token))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void aTokenWithoutTheRequiredScopesIsForbidden() throws Exception {
    mvc.perform(
            get(PING)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    BEARER + token(claims().claim("scope", "bibs.other").build())))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/integration/v1/receipts/2026")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    BEARER + token(claims().claim("scope", "bibs.receipts.read").build())))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/integration/v1/receipts/2026")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    BEARER
                        + token(
                            claims()
                                .claim("scope", "bibs.receipts.read bibs.receipts.write")
                                .build())))
        .andExpect(status().isNotFound());
  }

  @Test
  void aPathWithoutRuleIsRefusedEvenWithAValidToken() throws Exception {
    mvc.perform(
            get("/integration/v1/unlisted")
                .header(HttpHeaders.AUTHORIZATION, BEARER + token(claims())))
        .andExpect(status().isForbidden());
  }

  @Test
  void aUserTokenIsNotAcceptedOnTheIntegrationApis() throws Exception {
    String userToken = login("auditor");
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + userToken))
        .andExpect(status().isOk());
    mvc.perform(get(PING).header(HttpHeaders.AUTHORIZATION, BEARER + userToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void aGatewayTokenIsNotAcceptedOnTheUserApis() throws Exception {
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER + token(claims())))
        .andExpect(status().isUnauthorized());
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
            .getContentAsString(StandardCharsets.UTF_8);
    return JsonPath.read(body, "$.accessToken");
  }

  private static JWTClaimsSet.Builder claims() {
    Instant now = Instant.now();
    return new JWTClaimsSet.Builder()
        .issuer(ISSUER)
        .audience("bibs-api")
        .subject("egl-client")
        .claim("client_id", "egl-client")
        .claim("scope", "bibs.ping")
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plus(Duration.ofMinutes(5))));
  }

  private static String token(JWTClaimsSet.Builder claims) throws JOSEException {
    return sign(claims.build(), GATEWAY_KEY);
  }

  private static String token(JWTClaimsSet claims) throws JOSEException {
    return sign(claims, GATEWAY_KEY);
  }

  private static String sign(JWTClaimsSet claims, RSAKey key) throws JOSEException {
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }

  private static RSAKey rsaKey(String keyId) {
    try {
      return new RSAKeyGenerator(2048).keyID(keyId).generate();
    } catch (JOSEException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static HttpServer jwksServer() {
    try {
      HttpServer server =
          HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      byte[] body =
          new JWKSet(GATEWAY_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
      server.createContext(
          "/jwks",
          exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
              out.write(body);
            }
          });
      server.start();
      return server;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
