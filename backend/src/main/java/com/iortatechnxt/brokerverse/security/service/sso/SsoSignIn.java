package com.iortatechnxt.brokerverse.security.service.sso;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.SsoRequest;
import com.iortatechnxt.brokerverse.security.domain.SsoRequestRepository;
import com.iortatechnxt.brokerverse.security.domain.SsoTicket;
import com.iortatechnxt.brokerverse.security.domain.SsoTicketRepository;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordPolicy;
import com.iortatechnxt.brokerverse.security.service.SecureTokens;
import com.iortatechnxt.brokerverse.security.service.SignInCompletion;
import com.iortatechnxt.brokerverse.security.service.SignInMethod;
import com.iortatechnxt.brokerverse.security.service.SignInResult;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single sign-on at the client's identity provider ({@code AUTH_MODE} OIDC or SAML):
 *
 * <ol>
 *   <li>{@link #start} records the request (state, PKCE verifier and nonce, or the SAML request ID)
 *       and gives the address of the provider's sign-in page;
 *   <li>the provider's answer ({@link #oidcCallback}, {@link #samlResponse}) is validated, the
 *       identity is linked to an existing, active and unlocked user (never created: just-in-time
 *       linking only), the optional group check applies, and a one-time ticket (2 minutes) is given
 *       to the web client in the address of its callback page;
 *   <li>{@link #redeem} exchanges the ticket for the session, through the same steps as a password
 *       sign-in ({@link SignInCompletion}: second factor when configured, audit, session log).
 * </ol>
 *
 * <p>A refused sign-in sends the web client an error code only; the reason is audited and logged.
 */
@Service
public class SsoSignIn {

  /** Validity of a request sent to the provider. */
  public static final Duration REQUEST_VALIDITY = Duration.ofMinutes(10);

  /** Validity of the one-time ticket. */
  public static final Duration TICKET_VALIDITY = Duration.ofMinutes(2);

  private static final Logger LOG = LoggerFactory.getLogger(SsoSignIn.class);
  private static final String ENTITY = "AppUser";
  private static final int REQUEST_ID_BYTES = 20;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final SsoProperties properties;
  private final OidcClient oidc;
  private final SamlServiceProvider saml;
  private final SsoRequestRepository requests;
  private final SsoTicketRepository tickets;
  private final AppUserRepository users;
  private final AuthPasswordPolicy passwordPolicy;
  private final SignInCompletion completion;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param properties single sign-on settings
   * @param oidc OpenID Connect client
   * @param saml SAML service provider
   * @param requests requests sent to the provider
   * @param tickets one-time tickets
   * @param users users
   * @param passwordPolicy sign-in mode
   * @param completion shared sign-in steps
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the single sign-on
  public SsoSignIn(
      SsoProperties properties,
      OidcClient oidc,
      SamlServiceProvider saml,
      SsoRequestRepository requests,
      SsoTicketRepository tickets,
      AppUserRepository users,
      AuthPasswordPolicy passwordPolicy,
      SignInCompletion completion,
      AuditTrailService audit,
      Clock clock) {
    this.properties = properties;
    this.oidc = oidc;
    this.saml = saml;
    this.requests = requests;
    this.tickets = tickets;
    this.users = users;
    this.passwordPolicy = passwordPolicy;
    this.completion = completion;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Starts a sign-in at the identity provider.
   *
   * @return the address of the provider's sign-in page
   * @throws SsoException NOT_CONFIGURED when single sign-on is not the mode or not configured
   */
  @Transactional
  public String start() {
    AuthMode mode = passwordPolicy.mode();
    Instant now = clock.instant();
    String state = SecureTokens.newToken();
    if (mode == AuthMode.OIDC && oidc.configured()) {
      String verifier = SecureTokens.newToken() + SecureTokens.newToken();
      String nonce = SecureTokens.newToken();
      requests.save(
          new SsoRequest(
              SecureTokens.sha256(state),
              SignInMethod.OIDC,
              verifier,
              nonce,
              null,
              now,
              now.plus(REQUEST_VALIDITY)));
      return oidc.authorizationUrl(state, nonce, verifier);
    }
    if (mode == AuthMode.SAML && saml.configured()) {
      String requestId = "_" + HexFormat.of().formatHex(randomBytes());
      requests.save(
          new SsoRequest(
              SecureTokens.sha256(state),
              SignInMethod.SAML,
              null,
              null,
              requestId,
              now,
              now.plus(REQUEST_VALIDITY)));
      return saml.redirectUrl(requestId, state);
    }
    throw new SsoException(
        SsoException.NOT_CONFIGURED, "Single sign-on is not the sign-in mode or not configured");
  }

  /**
   * Completes an OpenID Connect sign-in with the provider's answer.
   *
   * @param code authorisation code, null when the provider refused
   * @param state state of the request
   * @param error error of the provider, null on success
   * @return the address of the web client's callback page (with a ticket or an error code)
   */
  @Transactional(noRollbackFor = SsoException.class)
  public String oidcCallback(String code, String state, String error) {
    try {
      SsoRequest request = request(state, SignInMethod.OIDC);
      if (error != null && !error.isBlank()) {
        throw new SsoException(SsoException.PROVIDER_ERROR, "The provider answered " + error);
      }
      if (code == null || code.isBlank()) {
        throw new SsoException(SsoException.INVALID, "No authorisation code");
      }
      SsoIdentity identity = oidc.exchange(code, request.getCodeVerifier(), request.getNonce());
      return ticketUrl(link(identity, SignInMethod.OIDC), SignInMethod.OIDC);
    } catch (SsoException ex) {
      return failed(ex, SignInMethod.OIDC);
    }
  }

  /**
   * Completes a SAML sign-in with the Response posted by the provider.
   *
   * @param samlResponse the SAMLResponse form field
   * @param relayState the RelayState form field (our state)
   * @return the address of the web client's callback page (with a ticket or an error code)
   */
  @Transactional(noRollbackFor = SsoException.class)
  public String samlResponse(String samlResponse, String relayState) {
    try {
      if (samlResponse == null || samlResponse.isBlank()) {
        throw new SsoException(SsoException.INVALID, "No SAMLResponse");
      }
      SsoRequest request =
          requests
              .findByStateHash(SecureTokens.sha256(relayState == null ? "" : relayState))
              .filter(r -> SignInMethod.SAML.equals(r.getProtocol()))
              .orElseThrow(
                  () -> new SsoException(SsoException.INVALID, "Unknown or missing RelayState"));
      Instant now = clock.instant();
      SsoIdentity identity =
          saml.validate(
              samlResponse,
              inResponseTo -> inResponseTo.equals(request.getRequestId()) && request.use(now));
      return ticketUrl(link(identity, SignInMethod.SAML), SignInMethod.SAML);
    } catch (SsoException ex) {
      return failed(ex, SignInMethod.SAML);
    }
  }

  /**
   * Exchanges the one-time ticket for the session (or for the second factor when configured).
   *
   * @param ticket ticket from the callback address
   * @return the answer of the sign-in
   */
  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = AuthenticationException.class)
  public SignInResult redeem(String ticket) {
    Instant now = clock.instant();
    SsoTicket found =
        tickets
            .findByTicketHash(SecureTokens.sha256(ticket == null ? "" : ticket.trim()))
            .filter(t -> t.use(now))
            .orElseThrow(() -> new BadCredentialsException("The sign-in link is not valid"));
    AppUser user =
        users
            .findByUsernameIgnoreCase(found.getUsername())
            .orElseThrow(() -> completion.refuse(found.getUsername(), "Unknown user"));
    completion.refuseLockedOrDeactivated(user, user.getUsername());
    return completion.afterFirstFactor(user, found.getMethod(), null);
  }

  /**
   * Removes the requests and tickets past their validity (the session sweep).
   *
   * @return rows removed
   */
  @Transactional
  public int purgeExpired() {
    Instant now = clock.instant();
    return requests.deleteExpired(now) + tickets.deleteExpired(now);
  }

  /**
   * The address of the identity provider's sign-out page, which returns to the Login page of the
   * system (BDOI FRS FRUM.001.05: with single sign-on, Log Out ends the BDO session too).
   *
   * @return the address, empty when the provider has none or single sign-on is not on
   */
  public Optional<String> signOutUrl() {
    if (passwordPolicy.mode() != AuthMode.OIDC) {
      return Optional.empty();
    }
    return oidc.signOutUrl(properties.base() + "/login");
  }

  /**
   * The web client's callback page.
   *
   * @return address
   */
  public String webCallback() {
    return properties.base() + SsoProperties.WEB_CALLBACK;
  }

  private SsoRequest request(String state, String protocol) {
    SsoRequest request =
        requests
            .findByStateHash(SecureTokens.sha256(state == null ? "" : state))
            .filter(r -> protocol.equals(r.getProtocol()))
            .orElseThrow(() -> new SsoException(SsoException.INVALID, "Unknown or missing state"));
    if (!request.use(clock.instant())) {
      throw new SsoException(SsoException.INVALID, "The request was used already or has expired");
    }
    return request;
  }

  /** The active, unlocked user of an identity; never created. */
  private AppUser link(SsoIdentity identity, String method) {
    AppUser user =
        users
            .findByUsernameIgnoreCase(identity.username())
            .or(() -> users.findByWindowsIdIgnoreCase(identity.username()))
            .orElseThrow(
                () ->
                    notLinked(
                        identity.username(),
                        "no user " + identity.username() + " in " + BrandAssets.SYSTEM_NAME));
    if (!user.isEnabled() || user.isLocked()) {
      throw notLinked(
          user.getUsername(), user.isLocked() ? "account locked" : "account deactivated");
    }
    if (!properties.groupRoles().isEmpty() && !groupAllowed(user, identity)) {
      throw notLinked(
          user.getUsername(), "the provider asserts no group mapped to a role of the user");
    }
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.LOGIN,
        "Identity provider accepted the sign-in (" + method + ")");
    return user;
  }

  private boolean groupAllowed(AppUser user, SsoIdentity identity) {
    Set<String> held =
        user.getRoles().stream()
            .filter(Role::isActive)
            .map(r -> r.getCode().toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet());
    return identity.groups().stream()
        .map(properties.groupRoles()::get)
        .filter(Objects::nonNull)
        .anyMatch(role -> held.contains(role.trim().toUpperCase(Locale.ROOT)));
  }

  private SsoException notLinked(String username, String reason) {
    audit.recordIndependently(
        username, ENTITY, username, AuditAction.LOGIN_FAILED, "Single sign-on refused: " + reason);
    return new SsoException(SsoException.NOT_LINKED, reason);
  }

  private String ticketUrl(AppUser user, String method) {
    String ticket = SecureTokens.newToken();
    Instant now = clock.instant();
    tickets.save(
        new SsoTicket(
            SecureTokens.sha256(ticket),
            user.getUsername(),
            method,
            now,
            now.plus(TICKET_VALIDITY)));
    return webCallback() + "?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8);
  }

  private String failed(SsoException ex, String method) {
    LOG.warn("Single sign-on ({}) refused: {}", method, ex.getMessage());
    return webCallback() + "?error=" + ex.getCode();
  }

  private static byte[] randomBytes() {
    byte[] bytes = new byte[REQUEST_ID_BYTES];
    RANDOM.nextBytes(bytes);
    return bytes;
  }
}
