package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticators;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryResult;
import com.iortatechnxt.brokerverse.security.service.directory.LocalPasswordAuthenticator;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login with lockout: consecutive failures up to the business parameter {@code
 * LOGIN_MAX_FAILED_ATTEMPTS} (BDOI NFR: 3; {@code brokerverse.security.max-failed-attempts} when
 * the parameter is missing) lock the account until an administrator unlocks it. Failures are
 * counted on the shared counter ({@link LoginAttemptTracker}) so concurrent attempts on several
 * instances all count; the user record keeps the count. Logout revokes the token ({@link
 * TokenRevocationStore}). Every success, failure and logout is written to the audit trail, and
 * every issued token opens a session in the session log that the logout ends ({@link
 * UserSessionLog}, UAM-NFR-35).
 *
 * <p>The password is checked by the {@link
 * com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticator} of the sign-in
 * mode {@code AUTH_MODE} (FR-UA-003): LOCAL finds the user by user name, DIRECTORY by Windows ID
 * and shows the directory's message. The lockout, audit and session log are the same in both modes.
 * In LOCAL mode the login response says when the password must be changed first: after an
 * administrator reset or creation, or once it is older than {@code PASSWORD_MAX_AGE_DAYS}
 * (UAM-NFR-36); the web client asks for the new password before it opens the home page.
 */
@Service
public class AuthService {

  private static final String ENTITY = "AppUser";

  private final DirectoryAuthenticators authenticators;
  private final AppUserRepository users;
  private final JwtTokenService tokens;
  private final AuditTrailService audit;
  private final SystemParameterService parameters;
  private final SecurityProperties properties;
  private final Clock clock;
  private final LoginAttemptTracker attempts;
  private final TokenRevocationStore revocations;
  private final UserSessionLog sessions;
  private final AuthPasswordPolicy passwordPolicy;

  /**
   * Creates the service.
   *
   * @param authenticators password check of each sign-in mode
   * @param users user repository
   * @param tokens token service
   * @param audit audit trail
   * @param parameters business parameters (lockout threshold)
   * @param properties security settings (default lockout threshold)
   * @param clock clock
   * @param attempts shared failed-login counter
   * @param revocations token denylist
   * @param sessions session log
   * @param passwordPolicy password rules (sign-in mode, expiry)
   */
  @SuppressWarnings("java:S107") // collaborators of the sign-in
  public AuthService(
      DirectoryAuthenticators authenticators,
      AppUserRepository users,
      JwtTokenService tokens,
      AuditTrailService audit,
      SystemParameterService parameters,
      SecurityProperties properties,
      Clock clock,
      LoginAttemptTracker attempts,
      TokenRevocationStore revocations,
      UserSessionLog sessions,
      AuthPasswordPolicy passwordPolicy) {
    this.authenticators = authenticators;
    this.users = users;
    this.tokens = tokens;
    this.audit = audit;
    this.parameters = parameters;
    this.properties = properties;
    this.clock = clock;
    this.attempts = attempts;
    this.revocations = revocations;
    this.sessions = sessions;
    this.passwordPolicy = passwordPolicy;
  }

  /**
   * Authenticates a user and issues a token.
   *
   * @param username user name (LOCAL) or Windows ID (DIRECTORY)
   * @param password password
   * @return token, profile and whether the password must be changed first
   */
  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = AuthenticationException.class)
  public LoginResponse login(String username, String password) {
    AuthMode mode = passwordPolicy.mode();
    AppUser user = findUser(mode, username).orElse(null);
    if (user == null) {
      audit.recordIndependently(
          username, ENTITY, username, AuditAction.LOGIN_FAILED, "Unknown user" + suffix(mode));
      throw new BadCredentialsException(LocalPasswordAuthenticator.INVALID);
    }
    if (user.isLocked()) {
      audit.recordIndependently(
          username, ENTITY, username, AuditAction.LOGIN_FAILED, "Account locked");
      throw new LockedException("Account is locked. Contact your administrator.");
    }
    DirectoryResult result =
        authenticators.authenticate(
            mode,
            mode == AuthMode.LOCAL ? user.getUsername() : username,
            password == null ? new char[0] : password.toCharArray());
    if (!result.succeeded()) {
      refuse(user, username, mode, result);
    }
    if (user.getFailedAttempts() > 0) {
      attempts.reset(user.getUsername());
    }
    user.recordSuccessfulLogin(clock.instant());
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.LOGIN,
        "Logged in" + suffix(mode));
    JwtTokenService.IssuedToken token = tokens.issue(user.getUsername());
    sessions.open(token.tokenId(), user.getUsername(), token.expiresAt());
    return new LoginResponse(
        token.token(),
        token.expiresAt(),
        UserProfileResponse.from(user),
        passwordPolicy.changeReason(user, clock.instant()).orElse(null));
  }

  /**
   * Signs the caller out: the token is revoked until it expires (every instance refuses it), its
   * session is ended, the sign-out time is kept on the user and the logout is audited (UAM-NFR-35).
   *
   * @param token the caller's bearer token
   */
  @Transactional
  public void logout(String token) {
    logout(token, SessionEndReason.LOGOUT);
  }

  /**
   * Signs the caller out with the reason the web client gives: the user's own sign-out (LOGOUT),
   * the inactivity sign-out (IDLE_TIMEOUT, FR-UA-002) or the end of the token (EXPIRED). Any other
   * reason is recorded as LOGOUT.
   *
   * @param token the caller's bearer token
   * @param reason why the session ends
   */
  @Transactional
  public void logout(String token, SessionEndReason reason) {
    SessionEndReason recorded =
        reason == SessionEndReason.IDLE_TIMEOUT || reason == SessionEndReason.EXPIRED
            ? reason
            : SessionEndReason.LOGOUT;
    JwtTokenService.TokenClaims claims =
        tokens.parse(token).orElseThrow(() -> new BadCredentialsException("Invalid token"));
    if (claims.tokenId() != null && claims.expiresAt() != null) {
      revocations.revoke(claims.tokenId(), claims.username(), claims.expiresAt());
    }
    sessions.end(claims.tokenId(), recorded);
    users
        .findByUsernameIgnoreCase(claims.username())
        .ifPresent(u -> u.recordLogout(clock.instant()));
    audit.record(
        ENTITY,
        claims.username(),
        AuditAction.LOGOUT,
        switch (recorded) {
          case IDLE_TIMEOUT -> "Logged out after inactivity";
          case EXPIRED -> "Logged out at the end of the session";
          default -> "Logged out";
        });
  }

  private Optional<AppUser> findUser(AuthMode mode, String userId) {
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }
    return mode == AuthMode.DIRECTORY
        ? users.findByWindowsIdIgnoreCase(userId.trim())
        : users.findByUsernameIgnoreCase(userId);
  }

  /**
   * Refuses the sign-in: a directory that cannot answer counts nothing (service message); a refused
   * password counts towards the lockout, and a lock in the directory is shown as it is.
   */
  private void refuse(AppUser user, String username, AuthMode mode, DirectoryResult result) {
    switch (result.outcome()) {
      case ERROR -> {
        audit.recordIndependently(
            username,
            ENTITY,
            username,
            AuditAction.LOGIN_FAILED,
            "Sign-in service unavailable" + suffix(mode) + ": " + result.message());
        throw new BusinessRuleException("SIGN_IN_UNAVAILABLE", result.message());
      }
      case LOCKED -> {
        audit.recordIndependently(
            username, ENTITY, username, AuditAction.LOGIN_FAILED, "Locked in the directory");
        throw new LockedException(result.message());
      }
      default -> {
        user.recordFailedLogins(
            attempts.recordFailure(user.getUsername(), user.getFailedAttempts()),
            parameters.intValue(
                SystemParameterService.LOGIN_MAX_FAILED_ATTEMPTS, properties.maxFailedAttempts()));
        audit.recordIndependently(
            username,
            ENTITY,
            username,
            AuditAction.LOGIN_FAILED,
            "Failed login attempt " + user.getFailedAttempts() + suffix(mode));
        if (user.isLocked()) {
          sessions.endAll(user.getUsername(), SessionEndReason.LOCKED);
        }
        throw new BadCredentialsException(result.message());
      }
    }
  }

  private static String suffix(AuthMode mode) {
    return mode == AuthMode.DIRECTORY ? " (directory sign-in)" : "";
  }
}
