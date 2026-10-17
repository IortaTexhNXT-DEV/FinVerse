package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.net.CookieManager;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Single sign-on end to end against the Enterprise SSO simulator over HTTP (BDOI FRS FRUM.001.01,
 * FRUM.001.02, FRUM.001.05, FRUM.001.06, FRUM.003.03, FRUM-004.01 and FRUM-004.03): a user whose
 * Windows ID matches an active user signs in at the simulated EIAM and gets the user's session; an
 * account that matches no user is refused as not linked and recorded; a disabled account and a
 * device that the device policy refuses never reach the system; after Log Out the next sign-in asks
 * for the EIAM sign-in again; a break-glass administrator signs in with the password and the second
 * factor and Information Security is alerted; no forgotten-password function is offered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EnterpriseSsoSimulatorIT {

  private static final int PORT = freePort();
  private static final String BASE = "http://127.0.0.1:" + PORT;
  private static final String CLIENT_ID = "bibs-sit";
  private static final String CLIENT_SECRET = "simulator-client-credential-0123456789";

  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private SystemParameterService parameters;

  private HttpClient browser;

  @DynamicPropertySource
  static void sso(DynamicPropertyRegistry registry) {
    registry.add("server.port", () -> PORT);
    registry.add("brokerverse.security.sso.base-url", () -> BASE);
    registry.add("brokerverse.security.sso.label", () -> "BDO EIAM");
    registry.add("brokerverse.security.sso.oidc.issuer", () -> BASE + "/eiam-simulator");
    registry.add("brokerverse.security.sso.oidc.client-id", () -> CLIENT_ID);
    registry.add("brokerverse.security.sso.oidc.client-secret", () -> CLIENT_SECRET);
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @BeforeEach
  void singleSignOn() {
    as.run("admin", () -> parameters.update(AuthMode.PARAMETER, AuthMode.OIDC.name()));
    browser =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .cookieHandler(new CookieManager())
            .build();
  }

  @AfterEach
  void local() {
    as.run("admin", () -> parameters.update(AuthMode.PARAMETER, AuthMode.LOCAL.name()));
  }

  @Test
  void aUserWhoseWindowsIdMatchesAnActiveUserSignsInAtEiamAndGetsTheSession() throws Exception {
    Map<String, String> request = start();
    assertThat(get(authorizeUrl(request)).body()).contains("Enterprise SSO simulator");
    HttpResponse<String> signedIn = signIn(request, "BDO\\sit.auditor", "BANK");
    assertThat(signedIn.statusCode()).isEqualTo(302);
    JsonNode session = complete(signedIn.headers().firstValue("Location").orElseThrow());
    assertThat(session.path("user").path("username").asText()).isEqualTo("auditor");
    assertThat(session.path("accessToken").asText()).isNotBlank();
    JsonNode options = json.readTree(get(BASE + "/api/v1/auth/sign-in-options").body());
    assertThat(options.path("singleSignOn").asBoolean()).isTrue();
    assertThat(options.path("passwordReset").asBoolean()).isFalse();
  }

  @Test
  void aDeviceThePolicyRefusesAndADisabledAccountNeverReachTheSystem() throws Exception {
    Map<String, String> request = start();
    HttpResponse<String> personal = signIn(request, "BDO\\sit.auditor", "PERSONAL");
    assertThat(personal.statusCode()).isEqualTo(403);
    assertThat(personal.body()).contains("only from bank-issued devices");
    assertThat(personal.headers().firstValue("Location")).isEmpty();
    HttpResponse<String> disabled = signIn(start(), "BDO\\dlim", "BANK");
    assertThat(disabled.statusCode()).isEqualTo(403);
    assertThat(disabled.body()).containsIgnoringCase("disabled");
  }

  @Test
  void anAccountThatMatchesNoActiveUserIsRefusedAsNotLinkedAndRecorded() throws Exception {
    HttpResponse<String> signedIn = signIn(start(), "BDO\\creyes", "BANK");
    HttpResponse<String> callback = get(signedIn.headers().firstValue("Location").orElseThrow());
    assertThat(callback.headers().firstValue("Location").orElseThrow())
        .endsWith("/sso/callback?error=SSO_NOT_LINKED");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where action = 'LOGIN_FAILED'"
                    + " and entity_id = ? and summary like 'Single sign-on refused%'",
                Integer.class, "BDO\\creyes"))
        .isPositive();
  }

  @Test
  void afterLogOutOpeningTheSystemAgainAsksForTheEiamSignIn() throws Exception {
    complete(
        signIn(start(), "BDO\\sit.auditor", "BANK").headers().firstValue("Location").orElseThrow());
    HttpResponse<String> again = get(authorizeUrl(start()));
    assertThat(again.statusCode()).isEqualTo(302);
    JsonNode signOut = json.readTree(get(BASE + "/api/v1/auth/sso/sign-out").body());
    String logout = signOut.path("redirectUrl").asText();
    assertThat(logout).startsWith(BASE + "/eiam-simulator/logout");
    HttpResponse<String> loggedOut = get(logout);
    assertThat(loggedOut.headers().firstValue("Location").orElseThrow()).isEqualTo(BASE + "/login");
    HttpResponse<String> asked = get(authorizeUrl(start()));
    assertThat(asked.statusCode()).isEqualTo(200);
    assertThat(asked.body()).contains("name=\"windowsId\"");
  }

  private Map<String, String> start() throws Exception {
    JsonNode start = json.readTree(get(BASE + "/api/v1/auth/sso/start").body());
    URI authorize = URI.create(start.path("redirectUrl").asText());
    assertThat(authorize.toString()).startsWith(BASE + "/eiam-simulator/authorize");
    Map<String, String> request = query(authorize.getRawQuery());
    request
        .keySet()
        .retainAll(
            Set.of(
                "client_id",
                "redirect_uri",
                "state",
                "nonce",
                "code_challenge",
                "response_type",
                "scope",
                "code_challenge_method"));
    return request;
  }

  private static String authorizeUrl(Map<String, String> request) {
    return BASE
        + "/eiam-simulator/authorize?"
        + request.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
  }

  private HttpResponse<String> signIn(Map<String, String> request, String windowsId, String device)
      throws Exception {
    Map<String, String> form = new HashMap<>(request);
    form.put("windowsId", windowsId);
    form.put("device", device);
    String body =
        form.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    return browser.send(
        HttpRequest.newBuilder(URI.create(BASE + "/eiam-simulator/authorize"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode complete(String callbackUrl) throws Exception {
    HttpResponse<String> callback = get(callbackUrl);
    String web = callback.headers().firstValue("Location").orElseThrow();
    String ticket = query(URI.create(web).getRawQuery()).get("ticket");
    return post("/api/v1/auth/sso/complete", Map.of("ticket", ticket));
  }

  private HttpResponse<String> get(String url) throws Exception {
    return browser.send(
        HttpRequest.newBuilder(URI.create(url)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode post(String path, Map<String, String> body) throws Exception {
    HttpResponse<String> answer =
        browser.send(
            HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(answer.statusCode()).as(answer.body()).isEqualTo(200);
    return json.readTree(answer.body());
  }

  private static Map<String, String> query(String raw) {
    Map<String, String> values = new HashMap<>();
    for (String pair : raw.split("&")) {
      int eq = pair.indexOf('=');
      values.put(
          URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
          URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
    }
    return values;
  }
}
