package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.security.api.dto.LoginRequest;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordResetConfirmRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordResetLinkRequest;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordStatusResponse;
import com.iortatechnxt.brokerverse.security.api.dto.ProfileUpdateRequest;
import com.iortatechnxt.brokerverse.security.api.dto.SessionResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordService;
import com.iortatechnxt.brokerverse.security.service.AuthProfileService;
import com.iortatechnxt.brokerverse.security.service.AuthService;
import com.iortatechnxt.brokerverse.security.service.AuthSessionService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Authentication endpoints (FR-UA-001 to 005): login, logout, the current profile and its contact
 * details, the own password change and password status, the own sessions, and the anonymous "Forgot
 * password?" flow ({@code /password-reset/**}).
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

  /**
   * Creates the controller.
   *
   * @param authService authentication
   * @param passwords self-service passwords
   * @param profiles own profile
   * @param sessions session list
   * @param clock clock
   */
  public AuthController(
      AuthService authService,
      AuthPasswordService passwords,
      AuthProfileService profiles,
      AuthSessionService sessions,
      Clock clock) {
    this.authService = authService;
    this.passwords = passwords;
    this.profiles = profiles;
    this.sessions = sessions;
    this.clock = clock;
  }

  /**
   * Logs in.
   *
   * @param request credentials
   * @return token, profile and whether the password must be changed first
   */
  @PostMapping("/login")
  @PreAuthorize(ANYONE)
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.username(), request.password());
  }

  /**
   * Logs out: revokes the caller's token on every instance, ends its session and audits the logout.
   *
   * @param authorization the {@code Authorization: Bearer} header of the caller
   * @param reason LOGOUT (default), IDLE_TIMEOUT (inactivity sign-out) or EXPIRED (end of token)
   */
  @PostMapping("/logout")
  @PreAuthorize(SIGNED_IN)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestParam(required = false) String reason) {
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
   * "Forgot password?": e-mails a single-use link to the registered address. The answer is the same
   * for every user ID.
   *
   * @param request user ID
   */
  @PostMapping("/password-reset/request")
  @PreAuthorize(ANYONE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestReset(@Valid @RequestBody PasswordResetLinkRequest request) {
    passwords.requestReset(request.userId());
  }

  /**
   * Checks a reset link before the new password is entered.
   *
   * @param request token of the link
   * @return user and expiry of the link
   */
  @PostMapping("/password-reset/check")
  @PreAuthorize(ANYONE)
  public Map<String, Object> checkReset(@Valid @RequestBody PasswordResetLinkRequest request) {
    AuthPasswordService.LinkStatus link = passwords.checkLink(request.token());
    return Map.of("username", link.username(), "expiresAt", link.expiresAt());
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
