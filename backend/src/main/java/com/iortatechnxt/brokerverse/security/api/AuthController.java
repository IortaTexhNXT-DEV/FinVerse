package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.security.api.dto.LoginRequest;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordResetConfirmRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordResetLinkRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordStatusResponse;
import com.iortatechnxt.brokerverse.security.api.dto.ProfileUpdateRequest;
import com.iortatechnxt.brokerverse.security.api.dto.RefreshResponse;
import com.iortatechnxt.brokerverse.security.api.dto.SessionResponse;
import com.iortatechnxt.brokerverse.security.api.dto.SignInOptionsResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordPolicy;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordService;
import com.iortatechnxt.brokerverse.security.service.AuthProfileService;
import com.iortatechnxt.brokerverse.security.service.AuthService;
import com.iortatechnxt.brokerverse.security.service.AuthSessionService;
import com.iortatechnxt.brokerverse.security.service.PasswordResetRules;
import com.iortatechnxt.brokerverse.security.service.SignInResult;
import com.iortatechnxt.brokerverse.security.service.SignInSessions;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.sso.SsoProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints (FR-UA-001 to 005): the sign-in options, login, the renewal of the
 * access token (refresh token in an HttpOnly cookie), logout, the current profile and its contact
 * details, the own password change and password status, the own sessions, and the anonymous "Forgot
 * password?" flow ({@code /password-reset/**}). The second factor is in {@link MfaController}, the
 * single sign-on in {@link SsoController}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String BEARER = "Bearer ";
  private static final String SIGNED_IN = "isAuthenticated()";
  private static final String ANYONE = "permitAll()";

  private final AuthService authService;
  private final AuthPasswordService passwords;
  private final AuthProfileService profiles;
  private final AuthSessionService sessions;
  private final Clock clock;
  private final SignInSessions signInSessions;
  private final RefreshCookies cookies;
  private final AuthPasswordPolicy passwordPolicy;
  private final SsoProperties sso;
  private final String environment;
  private final PasswordResetRules resetRules;

  /**
   * Creates the controller.
   *
   * @param authService authentication
   * @param passwords self-service passwords
   * @param profiles own profile
   * @param sessions session list
   * @param clock clock
   * @param signInSessions token model (refresh)
   * @param cookies refresh token cookie
   * @param passwordPolicy sign-in mode
   * @param sso single sign-on settings
   * @param environment {@code brokerverse.environment}, shown on the sign-in page
   * @param resetRules whether "Forgot password?" is offered (PASSWORD_RESET_BDOI_RULES)
   */
  @SuppressWarnings("java:S107") // endpoints of the sign-in
  public AuthController(
      AuthService authService,
      AuthPasswordService passwords,
      AuthProfileService profiles,
      AuthSessionService sessions,
      Clock clock,
      SignInSessions signInSessions,
      RefreshCookies cookies,
      AuthPasswordPolicy passwordPolicy,
      SsoProperties sso,
      @Value("${brokerverse.environment:local}") String environment,
      PasswordResetRules resetRules) {
    this.resetRules = resetRules;
    this.authService = authService;
    this.passwords = passwords;
    this.profiles = profiles;
    this.sessions = sessions;
    this.clock = clock;
    this.signInSessions = signInSessions;
    this.cookies = cookies;
    this.passwordPolicy = passwordPolicy;
    this.sso = sso;
    this.environment = environment.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * How users sign in on this deployment (anonymous).
   *
   * @return sign-in options
   */
  @GetMapping("/sign-in-options")
  @PreAuthorize(ANYONE)
  public SignInOptionsResponse signInOptions() {
    AuthMode mode = passwordPolicy.mode();
    return new SignInOptionsResponse(
        mode.name(),
        mode.singleSignOn(),
        mode.singleSignOn() ? sso.label() : null,
        !mode.singleSignOn(),
        resetRules.selfServiceOffered(),
        environment);
  }

  /**
   * Logs in with a password: opens the session (the refresh token in an HttpOnly cookie) or asks
   * for the second factor.
   *
   * @param request credentials
   * @param http request
   * @param response response (the cookie)
   * @return token and profile, or the second factor asked for
   */
  @PostMapping("/login")
  @PreAuthorize(ANYONE)
  public LoginResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest http,
      HttpServletResponse response) {
    SignInResult result =
        authService.login(request.username(), request.password(), request.deviceToken());
    cookies.write(result, http, response);
    return result.response();
  }

  /**
   * Renews the access token with the refresh token cookie (sliding session). The request must carry
   * the header {@code X-Requested-With}; the refresh token is replaced on each renewal.
   *
   * @param requestedWith header set by the web client
   * @param http request (the cookie)
   * @param response response (the new cookie)
   * @return the new access token
   */
  @PostMapping("/refresh")
  @PreAuthorize(ANYONE)
  public RefreshResponse refresh(
      @RequestHeader(name = RefreshCookies.REQUEST_HEADER, required = false) String requestedWith,
      HttpServletRequest http,
      HttpServletResponse response) {
    if (requestedWith == null || requestedWith.isBlank()) {
      throw new BadCredentialsException(SignInSessions.SESSION_OVER);
    }
    SignInSessions.Tokens tokens;
    try {
      tokens = signInSessions.refresh(RefreshCookies.read(http).orElse(null));
    } catch (BadCredentialsException ex) {
      cookies.clear(http, response);
      throw ex;
    }
    if (tokens.refreshToken() != null) {
      cookies.write(tokens.refreshToken(), tokens.sessionExpiresAt(), http, response);
    }
    return new RefreshResponse(
        tokens.accessToken(), tokens.accessExpiresAt(), tokens.sessionExpiresAt());
  }

  /**
   * Logs out: revokes the caller's token on every instance, ends its session (the refresh token
   * stops working), removes the cookie and audits the logout.
   *
   * @param authorization the {@code Authorization: Bearer} header of the caller
   * @param reason LOGOUT (default), IDLE_TIMEOUT (inactivity sign-out) or EXPIRED (end of session)
   * @param http request
   * @param response response (the cookie is removed)
   */
  @PostMapping("/logout")
  @PreAuthorize(SIGNED_IN)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestParam(required = false) String reason,
      HttpServletRequest http,
      HttpServletResponse response) {
    cookies.clear(http, response);
    authService.logout(
        authorization.startsWith(BEARER) ? authorization.substring(BEARER.length()) : authorization,
        endReason(reason));
  }

  /**
   * Returns the current user's profile.
   *
   * @return profile
   */
  @GetMapping("/me")
  @PreAuthorize(SIGNED_IN)
  public UserProfileResponse me() {
    return UserProfileResponse.from(profiles.me());
  }

  /**
   * Changes the current user's e-mail address and mobile number (UQ17).
   *
   * @param request contact details
   * @return profile
   */
  @PutMapping("/me")
  @PreAuthorize(SIGNED_IN)
  public UserProfileResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
    return UserProfileResponse.from(profiles.updateContact(request.email(), request.mobileNo()));
  }

  /**
   * The password rules and the current user's password dates.
   *
   * @return status
   */
  @GetMapping("/password-status")
  @PreAuthorize(SIGNED_IN)
  public PasswordStatusResponse passwordStatus() {
    return passwords.status();
  }

  /**
   * Changes the current user's password (history, minimum age; UAM-NFR-36).
   *
   * @param request passwords
   */
  @PostMapping("/change-password")
  @PreAuthorize(SIGNED_IN)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
    passwords.changeOwnPassword(request.currentPassword(), request.newPassword());
  }

  /**
   * The current user's sessions, newest first.
   *
   * @param page page number
   * @param size page size
   * @return sessions
   */
  @GetMapping("/sessions")
  @PreAuthorize(SIGNED_IN)
  public PageResponse<SessionResponse> mySessions(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Instant now = clock.instant();
    return PageResponse.of(sessions.mine(page, size), s -> SessionResponse.from(s, now));
  }

  /**
   * "Forgot password?": e-mails a single-use link to the registered address, when the link is
   * offered (local sign-in, PASSWORD_RESET_BDOI_RULES off). The answer is the same for every user
   * ID.
   *
   * @param request user ID
   */
  @PostMapping("/password-reset/request")
  @PreAuthorize(ANYONE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestReset(@Valid @RequestBody PasswordResetLinkRequest request) {
    if (resetRules.selfServiceOffered()) {
      passwords.requestReset(request.userId());
    }
  }

  /**
   * Checks a reset link before the new password is entered. The answer carries the expiry only,
   * never the user name, so a link found by someone else does not reveal whose it is.
   *
   * @param request token of the link
   * @return expiry of the link
   */
  @PostMapping("/password-reset/check")
  @PreAuthorize(ANYONE)
  public Map<String, Object> checkReset(@Valid @RequestBody PasswordResetLinkRequest request) {
    AuthPasswordService.LinkStatus link = passwords.checkLink(request.token());
    return Map.of("expiresAt", link.expiresAt());
  }

  /**
   * Sets a new password with a reset link (the link works once).
   *
   * @param request token and new password
   */
  @PostMapping("/password-reset/confirm")
  @PreAuthorize(ANYONE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
    passwords.resetWithLink(request.token(), request.newPassword());
  }

  private static SessionEndReason endReason(String reason) {
    if (SessionEndReason.IDLE_TIMEOUT.name().equals(reason)) {
      return SessionEndReason.IDLE_TIMEOUT;
    }
    return SessionEndReason.EXPIRED.name().equals(reason)
        ? SessionEndReason.EXPIRED
        : SessionEndReason.LOGOUT;
  }
}
