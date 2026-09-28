package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.service.SignInResult;
import com.iortatechnxt.brokerverse.security.service.sso.SamlServiceProvider;
import com.iortatechnxt.brokerverse.security.service.sso.SsoException;
import com.iortatechnxt.brokerverse.security.service.sso.SsoSignIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single sign-on (AUTH_MODE OIDC or SAML), all anonymous: the start (address of the provider's
 * page), the provider's answers (OpenID Connect redirect, SAML POST to the assertion consumer
 * service), which send the browser back to the web client's {@code /sso/callback} page with a
 * one-time ticket or an error code, the redemption of the ticket for the session, and the SAML
 * metadata of BrokerVerse.
 */
@RestController
@RequestMapping("/api/v1/auth/sso")
public class SsoController {

  private static final Logger LOG = LoggerFactory.getLogger(SsoController.class);
  private static final String ANYONE = "permitAll()";
  private static final int MAX_SAML_RESPONSE = 256 * 1024;

  private final SsoSignIn sso;
  private final SamlServiceProvider saml;
  private final RefreshCookies cookies;

  /**
   * Creates the controller.
   *
   * @param sso single sign-on
   * @param saml SAML service provider
   * @param cookies refresh token cookie
   */
  public SsoController(SsoSignIn sso, SamlServiceProvider saml, RefreshCookies cookies) {
    this.sso = sso;
    this.saml = saml;
    this.cookies = cookies;
  }

  /**
   * Starts a sign-in at the identity provider.
   *
   * @return the address of the provider's sign-in page
   */
  @GetMapping("/start")
  @PreAuthorize(ANYONE)
  public Start start() {
    try {
      return new Start(sso.start());
    } catch (SsoException ex) {
      LOG.warn("Single sign-on not started: {}", ex.getMessage());
      throw new BusinessRuleException(ex.getCode(), "Single sign-on is not available", ex);
    }
  }

  /**
   * The OpenID Connect redirect of the provider.
   *
   * @param code authorisation code
   * @param state state of the request
   * @param error error of the provider
   * @return redirect to the web client's callback page
   */
  @GetMapping("/oidc/callback")
  @PreAuthorize(ANYONE)
  public ResponseEntity<Void> oidcCallback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error) {
    return seeOther(sso.oidcCallback(code, state, error));
  }

  /**
   * The SAML Response posted by the provider (HTTP-POST binding).
   *
   * @param samlResponse the SAMLResponse form field
   * @param relayState the RelayState form field
   * @return redirect to the web client's callback page
   */
  @PostMapping(value = "/saml/acs", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize(ANYONE)
  public ResponseEntity<Void> samlAcs(
      @RequestParam(name = "SAMLResponse", required = false) String samlResponse,
      @RequestParam(name = "RelayState", required = false) String relayState) {
    if (samlResponse != null && samlResponse.length() > MAX_SAML_RESPONSE) {
      return seeOther(sso.webCallback() + "?error=" + SsoException.INVALID);
    }
    return seeOther(sso.samlResponse(samlResponse, relayState));
  }

  /**
   * The SAML metadata of BrokerVerse, to register it at the provider.
   *
   * @return metadata XML
   */
  @GetMapping(value = "/saml/metadata", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize(ANYONE)
  public String samlMetadata() {
    return saml.metadata();
  }

  /**
   * Exchanges the one-time ticket for the session (or the second factor when configured).
   *
   * @param request the ticket
   * @param http request
   * @param response response (the refresh token cookie)
   * @return the sign-in answer
   */
  @PostMapping("/complete")
  @PreAuthorize(ANYONE)
  public LoginResponse complete(
      @Valid @RequestBody Ticket request, HttpServletRequest http, HttpServletResponse response) {
    SignInResult result = sso.redeem(request.ticket());
    cookies.write(result, http, response);
    return result.response();
  }

  private static ResponseEntity<Void> seeOther(String location) {
    return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
  }

  /**
   * Start of a sign-in.
   *
   * @param redirectUrl address of the provider's sign-in page
   */
  public record Start(String redirectUrl) {}

  /**
   * A one-time ticket.
   *
   * @param ticket ticket from the callback address
   */
  public record Ticket(@NotBlank @Size(max = 100) String ticket) {}
}
