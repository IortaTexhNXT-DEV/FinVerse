package com.iortatechnxt.brokerverse.identity.api;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.service.EiamSimulator;
import com.iortatechnxt.brokerverse.identity.service.IdentityRefused;
import com.iortatechnxt.brokerverse.identity.service.IdentitySimulator;
import com.iortatechnxt.brokerverse.security.service.sso.OidcClient;
import com.iortatechnxt.brokerverse.security.service.sso.SsoProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * The sign-in pages and the OpenID Connect endpoints of the Enterprise SSO simulator (SIT and UAT
 * only): discovery, the sign-in page (account and device), the token endpoint, the key set and the
 * sign-out page. With {@code AUTH_MODE} OIDC and the issuer set to {@code
 * <address>/eiam-simulator}, the single sign-on of the system runs end to end against it.
 */
@RestController
@RequestMapping(EiamSimulator.PATH)
@ConditionalOnProperty(name = "brokerverse.identity.simulator", havingValue = "true")
public class EiamSimulatorController {

  private static final String COOKIE = "EIAMSIM";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String STATE = "state";
  private static final String NONCE = "nonce";
  private static final String CHALLENGE = "code_challenge";
  private static final String CLIENT_ID = "client_id";
  private static final String BASIC = "Basic ";
  private static final String SIGN_IN_PATH = "/login";
  private static final String INVALID_REDIRECT = "INVALID_REDIRECT";
  private static final String TOKEN_REFUSED = "The token request was refused";

  private static final Duration SESSION_LIFE = Duration.ofHours(8);
  private static final int TOKEN_SECONDS = 300;
  private static final int PAGE_BUFFER = 512;

  private final EiamSimulator simulator;
  private final IdentitySimulator accounts;
  private final SsoProperties sso;

  /**
   * Creates the controller.
   *
   * @param simulator the simulated identity provider
   * @param accounts the accounts of the simulator
   * @param sso single sign-on settings (the public address)
   */
  public EiamSimulatorController(
      EiamSimulator simulator, IdentitySimulator accounts, SsoProperties sso) {
    this.simulator = simulator;
    this.accounts = accounts;
    this.sso = sso;
  }

  /**
   * The discovery document.
   *
   * @return document
   */
  @GetMapping("/.well-known/openid-configuration")
  public Map<String, Object> discovery() {
    String issuer = simulator.issuer();
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("issuer", issuer);
    doc.put("authorization_endpoint", issuer + "/authorize");
    doc.put("token_endpoint", issuer + "/token");
    doc.put("jwks_uri", issuer + "/jwks");
    doc.put("end_session_endpoint", issuer + "/logout");
    doc.put("response_types_supported", new String[] {"code"});
    doc.put("subject_types_supported", new String[] {"public"});
    doc.put("id_token_signing_alg_values_supported", new String[] {"RS256"});
    doc.put("code_challenge_methods_supported", new String[] {"S256"});
    return doc;
  }

  /**
   * The key set of the ID token signatures.
   *
   * @return key set
   */
  @GetMapping("/jwks")
  public Map<String, Object> jwks() {
    return simulator.keySet();
  }

  /**
   * The sign-in page; with an open simulator session the user is signed in at once (single
   * sign-on).
   *
   * @param params the authorisation request
   * @param session the simulator's session cookie, if any
   * @return the page, or the redirect with the code
   */
  @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> authorize(
      @RequestParam Map<String, String> params,
      @CookieValue(name = COOKIE, required = false) String session) {
    EiamSimulator.AuthorizationRequest request = request(params);
    String open = simulator.sessionUser(session).orElse(null);
    if (open != null) {
      try {
        return redirect(request, simulator.signIn(open, true));
      } catch (IdentityRefused ex) {
        return page(refusalText(ex), HttpStatus.FORBIDDEN);
      }
    }
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(signInPage(params));
  }

  /**
   * Signs a user in with the account and the device chosen on the page.
   *
   * @param params the authorisation request and the choices
   * @param http request (secure or not)
   * @param response response (the session cookie)
   * @return the redirect with the code, or the refusal page of EIAM
   */
  @PostMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> signIn(
      @RequestParam Map<String, String> params,
      HttpServletRequest http,
      HttpServletResponse response) {
    EiamSimulator.AuthorizationRequest request = request(params);
    try {
      DirectoryAccount account =
          simulator.signIn(params.get("windowsId"), "BANK".equals(params.get("device")));
      response.addHeader(
          HttpHeaders.SET_COOKIE,
          sessionCookie(simulator.openSession(account.windowsId()), SESSION_LIFE, http).toString());
      return redirect(request, account);
    } catch (IdentityRefused ex) {
      return page(refusalText(ex), HttpStatus.FORBIDDEN);
    }
  }

  /**
   * The token endpoint (client secret basic, PKCE).
   *
   * @param params form
   * @param authorization Basic credentials of the client
   * @return the tokens, or the OAuth error
   */
  @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> token(
      @RequestParam Map<String, String> params,
      @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    String[] client = basic(authorization, params);
    try {
      String idToken =
          simulator.token(params.get("code"), params.get("code_verifier"), client[0], client[1]);
      Map<String, Object> answer = new LinkedHashMap<>();
      answer.put("access_token", idToken);
      answer.put("id_token", idToken);
      answer.put("token_type", "Bearer");
      answer.put("expires_in", TOKEN_SECONDS);
      return ResponseEntity.ok(answer);
    } catch (IdentityRefused ex) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", tokenError(ex), "error_description", TOKEN_REFUSED));
    }
  }

  /**
   * The sign-out page: ends the simulator session and returns to the system.
   *
   * @param redirect address to return to, within the system
   * @param session the simulator's session cookie, if any
   * @param http request (secure or not)
   * @param response response (the cookie removed)
   * @return the redirect, or the signed-out page
   */
  @GetMapping(value = "/logout", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> logout(
      @RequestParam(name = "post_logout_redirect_uri", required = false) String redirect,
      @CookieValue(name = COOKIE, required = false) String session,
      HttpServletRequest http,
      HttpServletResponse response) {
    simulator.endSession(session);
    response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO, http).toString());
    String home = sso.base() + SIGN_IN_PATH;
    if (home.equals(redirect)) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(home)).build();
    }
    return page("You are signed out of the Enterprise SSO simulator.", HttpStatus.OK);
  }

  private EiamSimulator.AuthorizationRequest request(Map<String, String> params) {
    String redirect = sso.base() + OidcClient.CALLBACK_PATH;
    if (!redirect.equals(params.get(REDIRECT_URI))) {
      throw new IdentityRefused(INVALID_REDIRECT, "The redirect address is not registered");
    }
    return new EiamSimulator.AuthorizationRequest(
        params.get(CLIENT_ID),
        redirect,
        params.get(STATE),
        params.get(NONCE),
        params.get(CHALLENGE));
  }

  private ResponseEntity<String> redirect(
      EiamSimulator.AuthorizationRequest request, DirectoryAccount account) {
    String code = simulator.code(account, request);
    String target =
        request.redirectUri()
            + (request.redirectUri().contains("?") ? "&" : "?")
            + "code="
            + encode(code)
            + "&state="
            + encode(request.state());
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
  }

  private String signInPage(Map<String, String> params) {
    StringBuilder options = new StringBuilder(PAGE_BUFFER);
    for (DirectoryAccount a : accounts.accounts()) {
      options
          .append("<option value=\"")
          .append(HtmlUtils.htmlEscape(a.windowsId()))
          .append("\">")
          .append(
              HtmlUtils.htmlEscape(a.fullName() + " - " + a.windowsId() + " (" + a.status() + ")"))
          .append("</option>");
    }
    StringBuilder hidden = new StringBuilder(PAGE_BUFFER);
    for (String name : new String[] {CLIENT_ID, REDIRECT_URI, STATE, NONCE, CHALLENGE}) {
      hidden
          .append("<input type=\"hidden\" name=\"")
          .append(name)
          .append("\" value=\"")
          .append(HtmlUtils.htmlEscape(params.getOrDefault(name, "")))
          .append("\">");
    }
    return html(
        "<h1>Enterprise SSO simulator</h1><p>Sign in with an account of the simulator"
            + " (SIT and UAT only).</p><form method=\"post\" action=\""
            + EiamSimulator.PATH
            + "/authorize\">"
            + hidden
            + "<label>Account <select name=\"windowsId\">"
            + options
            + "</select></label><fieldset><legend>Device</legend><label><input type=\"radio\""
            + " name=\"device\" value=\"BANK\" checked> Bank-issued device</label><label><input"
            + " type=\"radio\" name=\"device\" value=\"PERSONAL\"> Personal device</label>"
            + "</fieldset><button type=\"submit\">Sign in</button></form>");
  }

  /** A fixed text per refusal code, so that no detail of the refusal reaches the page. */
  private static String refusalText(IdentityRefused ex) {
    return switch (ex.getCode()) {
      case "DEVICE_NOT_ALLOWED" ->
          "Access blocked: your organisation's policy allows sign-in only from bank-issued devices";
      case "ACCOUNT_INACTIVE" -> "Your account is inactive; contact the service desk";
      case "ACCOUNT_LOCKED" -> "Your account is locked; contact the service desk";
      case "ACCOUNT_DISABLED" -> "Your account is disabled; contact the service desk";
      case "ACCOUNT_DEACTIVATED" -> "Your account is deactivated; contact the service desk";
      case "ACCOUNT_REFUSED" -> "Your account cannot sign in; contact the service desk";
      case INVALID_REDIRECT -> "The redirect address is not registered";
      case "UNKNOWN_CLIENT" -> "The application is not registered";
      default -> "Sign-in was refused by the Enterprise SSO";
    };
  }

  /** The OAuth error code of a refused token request, from the standard codes only. */
  private static String tokenError(IdentityRefused ex) {
    return "invalid_client".equals(ex.getCode()) ? "invalid_client" : "invalid_grant";
  }

  private static ResponseEntity<String> page(String message, HttpStatus status) {
    return ResponseEntity.status(status)
        .contentType(MediaType.TEXT_HTML)
        .body(
            html(
                "<h1>Enterprise SSO simulator</h1><p role=\"alert\">"
                    + HtmlUtils.htmlEscape(message)
                    + "</p>"));
  }

  private static String html(String body) {
    return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Enterprise SSO"
        + " simulator</title><style>body{font-family:sans-serif;max-width:32rem;margin:3rem auto;}"
        + "label,fieldset,button{display:block;margin:1rem 0;}select{width:100%;}</style></head>"
        + "<body>"
        + body
        + "</body></html>";
  }

  private static String[] basic(String authorization, Map<String, String> params) {
    if (authorization != null && authorization.startsWith(BASIC)) {
      String decoded =
          new String(
              Base64.getDecoder().decode(authorization.substring(BASIC.length())),
              StandardCharsets.UTF_8);
      int colon = decoded.indexOf(':');
      if (colon > 0) {
        return new String[] {
          URLDecoder.decode(decoded.substring(0, colon), StandardCharsets.UTF_8),
          URLDecoder.decode(decoded.substring(colon + 1), StandardCharsets.UTF_8)
        };
      }
    }
    return new String[] {params.get(CLIENT_ID), params.get("client_secret")};
  }

  private static ResponseCookie sessionCookie(
      String value, Duration life, HttpServletRequest http) {
    return ResponseCookie.from(COOKIE, value)
        .httpOnly(true)
        .secure(http.isSecure())
        .sameSite("Lax")
        .path(EiamSimulator.PATH)
        .maxAge(life)
        .build();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
