package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticators;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryResult;
import com.iortatechnxt.brokerverse.security.service.sso.SsoProperties;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password sign-in with lockout, and the sign-out.
 *
 * <p>Consecutive failures up to the business parameter {@code LOGIN_MAX_FAILED_ATTEMPTS} ({@code
 * brokerverse.security.max-failed-attempts} when the parameter is missing) lock the account until
 * an administrator unlocks it. Failures are counted on the shared counter ({@link
 * LoginAttemptTracker}) so concurrent attempts on several instances all count; the user record
 * keeps the count. A locked or deactivated user and a wrong password all get the same message, so
 * the answer reveals nothing about the account; the real reason is audited and logged, and the
 * attempt of a locked or deactivated user is not counted.
 *
 * <p>The password is checked by the {@link
 * com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticator} of the sign-in
 * mode {@code AUTH_MODE} (FR-UA-003): LOCAL finds the user by user name, DIRECTORY by Windows ID.
 * In OIDC and SAML mode the users sign in at the identity provider and only the break-glass
 * administrators ({@code brokerverse.security.sso.break-glass-users}) may use their local password.
 * After the password, {@link SignInCompletion} asks for the second factor or opens the session;
 * logout revokes the access token, ends the session (its refresh token stops working) and is
 * audited (UAM-NFR-35).
 */
@Service
public class AuthService {

  private static final String ENTITY = "AppUser";

  private final DirectoryAuthenticators authenticators;
  private final AppUserRepository users;
  private final JwtTokenService tokens;
  private final AuditTrailService audit;
  private final Clock clock;
  private final TokenRevocationStore revocations;
  private final UserSessionLog sessions;
  private final SignInCompletion completion;
  private final SsoProperties sso;
  private final SecurityStoreAlarm alarm;

  /**
   * Creates the service.
   *
   * @param authenticators password check of each sign-in mode
   * @param users user repository
   * @param tokens token service
   * @param audit audit trail
   * @param clock clock
   * @param revocations token denylist
   * @param sessions session log
   * @param completion second factor and session
   * @param sso single sign-on settings (break-glass administrators)
   * @param alarm reports an unreachable denylist at sign-out
   */
  @SuppressWarnings("java:S107") // collaborators of the sign-in
  public AuthService(
      DirectoryAuthenticators authenticators,
      AppUserRepository users,
      JwtTokenService tokens,
      AuditTrailService audit,
      Clock clock,
      TokenRevocationStore revocations,
      UserSessionLog sessions,
      SignInCompletion completion,
      SsoProperties sso,
      SecurityStoreAlarm alarm) {
    this.authenticators = authenticators;
    this.users = users;
    this.tokens = tokens;
    this.audit = audit;
    this.clock = clock;
    this.revocations = revocations;
    this.sessions = sessions;
    this.completion = completion;
    this.sso = sso;
    this.alarm = alarm;
  }

  /**
   * Checks a password and continues the sign-in (second factor or session).
   *
   * @param username user name (LOCAL, break-glass) or Windows ID (DIRECTORY)
   * @param password password
   * @return the answer: the session, or the second factor asked for
   */
  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = AuthenticationException.class)
  public SignInResult login(String username, String password) {
    return login(username, password, null);
  }

  /**
   * Checks a password and continues the sign-in (second factor or session).
   *
   * @param username user name (LOCAL, break-glass) or Windows ID (DIRECTORY)
   * @param password password
   * @param deviceToken token of a device remembered for the second factor, may be null
   * @return the answer: the session, or the second factor asked for
   */
  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = AuthenticationException.class)
  public SignInResult login(String username, String password, String deviceToken) {
    AuthMode mode = completion.mode();
    AppUser user = findUser(mode, username).orElse(null);
    if (user == null) {
      throw completion.refuse(username, "Unknown user" + suffix(mode));
    }
    if (mode.singleSignOn() && !sso.isBreakGlass(user.getUsername())) {
      throw completion.refuse(
          username, "Password sign-in refused: the users sign in at the identity provider");
    }
    completion.refuseLockedOrDeactivated(user, username);
    AuthMode checkedBy = mode == AuthMode.DIRECTORY ? AuthMode.DIRECTORY : AuthMode.LOCAL;
    DirectoryResult result =
        authenticators.authenticate(
            checkedBy,
            checkedBy == AuthMode.LOCAL ? user.getUsername() : username,
            password == null ? new char[0] : password.toCharArray());
    if (!result.succeeded()) {
      refuse(user, username, mode, result);
    }
    return completion.afterFirstFactor(user, SignInMethod.PASSWORD, deviceToken);
  }

  /**
   * Signs the caller out: the access token is revoked until it expires (every instance refuses it),
   * its session is ended (the refresh token stops working), the sign-out time is kept on the user
   * and the logout is audited (UAM-NFR-35).
   *
   * @param token the caller's bearer token
   */
  @Transactional
  public void logout(String token) {
    logout(token, SessionEndReason.LOGOUT);
  }

  /**
   * Signs the caller out with the reason the web client gives: the user's own sign-out (LOGOUT),
   * the inactivity sign-out (IDLE_TIMEOUT, FR-UA-002) or the end of the session (EXPIRED). Any
   * other reason is recorded as LOGOUT.
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
    try {
      revocations.revoke(claims.tokenId(), claims.username(), claims.expiresAt());
    } catch (RuntimeException ex) {
      alarm.raise("denylist", "the sign-out relies on the ended session in the session log", ex);
    }
    sessions.end(claims.sessionId(), recorded);
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
   * password counts towards the lockout; a lock in the directory gets the uniform message.
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
      case LOCKED ->
          throw completion.refuse(username, "Locked in the directory: " + result.message());
      default -> throw completion.countFailure(user, "Failed login attempt" + suffix(mode));
    }
  }

  private static String suffix(AuthMode mode) {
    return mode == AuthMode.DIRECTORY ? " (directory sign-in)" : "";
  }
}
