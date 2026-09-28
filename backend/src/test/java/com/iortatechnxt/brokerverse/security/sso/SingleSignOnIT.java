package com.iortatechnxt.brokerverse.security.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Single sign-on against in-process identity providers (V1182): OpenID Connect with the
 * authorisation code and PKCE, and SAML 2.0 with a signed assertion; the identity is linked to an
 * existing user only, the ticket works once, and in single sign-on mode only the break-glass
 * administrators may use a password.
 */
@IntegrationTest
class SingleSignOnIT {

  private static final String BASE = "https://bibs.test";
  private static final String CALLBACK = BASE + "/sso/callback";
  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String INITIAL = "SingleSign!0nPass";
  private static final OidcTestProvider OIDC;
  private static final SamlTestIdp SAML = new SamlTestIdp();

  static {
    try {
      OIDC = new OidcTestProvider();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private UserAdminService admin;
  @Autowired private AsUser as;
  @Autowired private SystemParameterService parameters;

  @DynamicPropertySource
  static void provider(DynamicPropertyRegistry registry) {
    registry.add("brokerverse.security.sso.base-url", () -> BASE);
    registry.add("brokerverse.security.sso.label", () -> "Test Directory");
    registry.add("brokerverse.security.sso.break-glass-users", () -> "ssobreakglass");
    registry.add("brokerverse.security.sso.groups-claim", () -> "groups");
    registry.add("brokerverse.security.sso.oidc.issuer", OIDC::issuer);
    registry.add("brokerverse.security.sso.oidc.client-id", () -> OidcTestProvider.CLIENT_ID);
    registry.add(
        "brokerverse.security.sso.oidc.client-secret", () -> OidcTestProvider.CLIENT_SECRET);
    registry.add("brokerverse.security.sso.saml.idp-entity-id", () -> SamlTestIdp.ENTITY_ID);
    registry.add("brokerverse.security.sso.saml.idp-sso-url", () -> "https://idp.test/sso");
    registry.add("brokerverse.security.sso.saml.idp-certificate", SAML::publicKeyPem);
  }

  @AfterAll
  static void stop() {
    OIDC.close();
  }

  @AfterEach
  void backToLocal() {
    mode(AuthMode.LOCAL);
  }

  private void mode(AuthMode mode) {
    as.run("admin", () -> parameters.update(AuthMode.PARAMETER, mode.name()));
  }

  private String newUser(String username) {
    as.run(
        "admin",
        () ->
            admin.createUser(
                new UserRequest(
                    username, "Single Sign-on Tester", null, null, null, Set.of("AUDITOR"), true),
                INITIAL));
    return username;
  }

  private Map<String, String> start() throws Exception {
    JsonNode answer =
        json.readTree(
            mvc.perform(get("/api/v1/auth/sso/start"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    URI redirect = URI.create(answer.get("redirectUrl").asText());
    return OidcTestProvider.form(redirect.getRawQuery());
  }

  private String location(MvcResult result) {
    assertThat(result.getResponse().getStatus()).isEqualTo(303);
    return result.getResponse().getHeader(HttpHeaders.LOCATION);
  }

  private MvcResult complete(String location) throws Exception {
    String ticket = OidcTestProvider.form(URI.create(location).getRawQuery()).get("ticket");
    return mvc.perform(
            post("/api/v1/auth/sso/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("ticket", ticket))))
        .andReturn();
  }

  private String oidcSignIn(String username) throws Exception {
    Map<String, String> query = start();
    assertThat(query.get("code_challenge_method")).isEqualTo("S256");
    assertThat(query.get("redirect_uri")).isEqualTo(BASE + "/api/v1/auth/sso/oidc/callback");
    String code = "code-" + IDS.incrementAndGet();
    OIDC.register(
        code,
        new OidcTestProvider.Grant(
            username, query.get("nonce"), query.get("code_challenge"), OidcTestProvider.CLIENT_ID));
    return location(
        mvc.perform(
                get("/api/v1/auth/sso/oidc/callback")
                    .param("code", code)
                    .param("state", query.get("state")))
            .andReturn());
  }

  @Test
  void anOpenIdConnectSignInOpensTheSessionOfTheLinkedUser() throws Exception {
    String username = newUser("ssoo" + IDS.incrementAndGet());
    mode(AuthMode.OIDC);
    mvc.perform(get("/api/v1/auth/sign-in-options")).andExpect(status().isOk()).andReturn();
    String location = oidcSignIn(username);
    assertThat(location).startsWith(CALLBACK + "?ticket=");
    MvcResult signedIn = complete(location);
    assertThat(signedIn.getResponse().getStatus()).isEqualTo(200);
    JsonNode body = json.readTree(signedIn.getResponse().getContentAsString());
    assertThat(body.get("user").get("username").asText()).isEqualTo(username);
    assertThat(body.get("mustChangePassword").asBoolean()).isFalse();
    assertThat(complete(location).getResponse().getStatus()).isEqualTo(401);

    // A user unknown to BrokerVerse is never created.
    assertThat(oidcSignIn("nobody-" + IDS.incrementAndGet()))
        .isEqualTo(CALLBACK + "?error=SSO_NOT_LINKED");
    // A replayed or unknown state is refused.
    assertThat(
            location(
                mvc.perform(
                        get("/api/v1/auth/sso/oidc/callback")
                            .param("code", "x")
                            .param("state", "forged"))
                    .andReturn()))
        .isEqualTo(CALLBACK + "?error=SSO_INVALID");
  }

  @Test
  void aWrongNonceOrVerifierIsRefused() throws Exception {
    String username = newUser("ssov" + IDS.incrementAndGet());
    mode(AuthMode.OIDC);
    Map<String, String> query = start();
    OIDC.register(
        "bad-nonce",
        new OidcTestProvider.Grant(
            username, "other", query.get("code_challenge"), OidcTestProvider.CLIENT_ID));
    assertThat(
            location(
                mvc.perform(
                        get("/api/v1/auth/sso/oidc/callback")
                            .param("code", "bad-nonce")
                            .param("state", query.get("state")))
                    .andReturn()))
        .isEqualTo(CALLBACK + "?error=SSO_INVALID");
    Map<String, String> second = start();
    OIDC.register(
        "bad-verifier",
        new OidcTestProvider.Grant(
            username, second.get("nonce"), "not-the-challenge", OidcTestProvider.CLIENT_ID));
    assertThat(
            location(
                mvc.perform(
                        get("/api/v1/auth/sso/oidc/callback")
                            .param("code", "bad-verifier")
                            .param("state", second.get("state")))
                    .andReturn()))
        .isEqualTo(CALLBACK + "?error=SSO_PROVIDER_ERROR");
  }

  @Test
  void aSamlSignInOpensTheSessionAndAChangedAssertionIsRefused() throws Exception {
    String username = newUser("ssos" + IDS.incrementAndGet());
    mode(AuthMode.SAML);
    Map<String, String> query = start();
    String request = inflate(query.get("SAMLRequest"));
    Matcher id = Pattern.compile("ID=\"([^\"]+)\"").matcher(request);
    assertThat(id.find()).isTrue();
    assertThat(request)
        .contains("AssertionConsumerServiceURL=\"" + BASE + "/api/v1/auth/sso/saml/acs\"");
    SamlTestIdp.Claims claims =
        new SamlTestIdp.Claims(
            id.group(1),
            username,
            BASE,
            BASE + "/api/v1/auth/sso/saml/acs",
            Instant.now(),
            List.of());
    String signed = SAML.signAssertion(SamlTestIdp.xml(claims));
    String forged =
        Base64.getEncoder()
            .encodeToString(
                signed.replace(">" + username + "<", ">admin<").getBytes(StandardCharsets.UTF_8));
    assertThat(acs(forged, query.get("RelayState"))).isEqualTo(CALLBACK + "?error=SSO_INVALID");
    // The request was not used by the refused answer, so the genuine one still completes.
    String location =
        acs(
            Base64.getEncoder().encodeToString(signed.getBytes(StandardCharsets.UTF_8)),
            query.get("RelayState"));
    assertThat(location).startsWith(CALLBACK + "?ticket=");
    assertThat(complete(location).getResponse().getStatus()).isEqualTo(200);
    assertThat(
            acs(
                Base64.getEncoder().encodeToString(signed.getBytes(StandardCharsets.UTF_8)),
                query.get("RelayState")))
        .isEqualTo(CALLBACK + "?error=SSO_INVALID");
    mvc.perform(get("/api/v1/auth/sso/saml/metadata")).andExpect(status().isOk());
  }

  @Test
  void inSingleSignOnModeOnlyTheBreakGlassAdministratorsUseAPassword() throws Exception {
    String user = newUser("ssop" + IDS.incrementAndGet());
    newUserIfMissing("ssobreakglass");
    mode(AuthMode.OIDC);
    assertThat(passwordLogin(user).getResponse().getStatus()).isEqualTo(401);
    JsonNode breakGlass =
        json.readTree(passwordLogin("ssobreakglass").getResponse().getContentAsString());
    assertThat(breakGlass.get("mfaStep").asText()).isEqualTo("ENROL");
  }

  private void newUserIfMissing(String username) {
    try {
      newUser(username);
    } catch (RuntimeException ex) {
      // created by an earlier run on the same database
    }
  }

  private MvcResult passwordLogin(String username) throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("username", username, "password", INITIAL))))
        .andReturn();
  }

  private String acs(String samlResponse, String relayState) throws Exception {
    return location(
        mvc.perform(
                post("/api/v1/auth/sso/saml/acs")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("SAMLResponse", samlResponse)
                    .param("RelayState", relayState))
            .andReturn());
  }

  private static String inflate(String base64) throws Exception {
    Inflater inflater = new Inflater(true);
    inflater.setInput(Base64.getDecoder().decode(base64));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (!inflater.finished()) {
      int n = inflater.inflate(buffer);
      if (n == 0 && inflater.needsInput()) {
        break;
      }
      out.write(buffer, 0, n);
    }
    inflater.end();
    return out.toString(StandardCharsets.UTF_8);
  }
}
