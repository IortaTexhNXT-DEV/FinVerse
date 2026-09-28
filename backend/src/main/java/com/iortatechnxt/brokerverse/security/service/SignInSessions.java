package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import com.iortatechnxt.brokerverse.security.domain.UserSessionRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The token model of a sign-in session (V1180): a short access token (parameter {@code
 * ACCESS_TOKEN_MINUTES}, 15 by default) and a rotating refresh token bound to the session.
 *
 * <ul>
 *   <li>{@link #open} opens the session in the session log with its absolute end ({@code
 *       brokerverse.security.token-validity}) and issues the first access token (its {@code jti} is
 *       the session id) and refresh token.
 *   <li>{@link #refresh} renews the access token while the session is in use and replaces the
 *       refresh token. The token just replaced is accepted for {@code refresh-grace} (a second tab
 *       renewing at the same moment gets an access token and keeps the new cookie); presented later
 *       it ends the session ({@code TOKEN_REUSED}, a stolen token). The refresh stops when the
 *       session was signed out, ended by an administrator or the sweep, idle for longer than {@code
 *       SESSION_TIMEOUT_MINUTES}, past its end, or when the user was locked or deactivated.
 * </ul>
 *
 * <p>Only the SHA-256 of a refresh token is stored. The renewal is not user activity: it does not
 * move the "last seen" time, so an idle session still ends.
 */
@Service
@Transactional(
    propagation = Propagation.REQUIRES_NEW,
    noRollbackFor = BadCredentialsException.class)
public class SignInSessions {

  /** Answer to a refresh that cannot renew the session: the user signs in again. */
  public static final String SESSION_OVER = "Your session has ended. Sign in again.";

  private static final String ENTITY = "AppUser";
  private static final int DEFAULT_IDLE_MINUTES = 30;

  private final UserSessionRepository sessions;
  private final AppUserRepository users;
  private final JwtTokenService tokens;
  private final SystemParameterService parameters;
  private final SecurityProperties properties;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sessions session log
   * @param users users (locked or deactivated users are not renewed)
   * @param tokens token service
   * @param parameters business parameters (token life, idle limit)
   * @param properties security settings (session life, grace)
   * @param audit audit trail (a reused refresh token)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the token model
  public SignInSessions(
      UserSessionRepository sessions,
      AppUserRepository users,
      JwtTokenService tokens,
      SystemParameterService parameters,
      SecurityProperties properties,
      AuditTrailService audit,
      Clock clock) {
    this.sessions = sessions;
    this.users = users;
    this.tokens = tokens;
    this.parameters = parameters;
    this.properties = properties;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Opens a session and issues its first tokens.
   *
   * @param username user
   * @param method how the user signed in: PASSWORD, OIDC or SAML
   * @param secondFactor whether a second factor was checked
   * @return the tokens
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Tokens open(String username, String method, boolean secondFactor) {
    Instant now = clock.instant();
    String sessionId = UUID.randomUUID().toString();
    Instant sessionEnd = now.plus(properties.tokenValidity());
    String refresh = SecureTokens.newToken();
    sessions.save(
        new UserSession(
            sessionId,
            username,
            now,
            sessionEnd,
            method,
            secondFactor,
            SecureTokens.sha256(refresh)));
    JwtTokenService.IssuedToken access =
        tokens.issueAccess(username, sessionId, sessionId, accessValidity(now, sessionEnd));
    return new Tokens(sessionId, username, access.token(), access.expiresAt(), refresh, sessionEnd);
  }

  /**
   * Renews the access token of a session with its refresh token.
   *
   * @param refreshToken refresh token from the cookie
   * @return the new access token, with a new refresh token unless the token was the one just
   *     replaced (grace)
   * @throws BadCredentialsException when the session cannot be renewed ({@value #SESSION_OVER})
   */
  public Tokens refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BadCredentialsException(SESSION_OVER);
    }
    Instant now = clock.instant();
    String hash = SecureTokens.sha256(refreshToken.trim());
    Optional<UserSession> current = sessions.findByRefreshHash(hash);
    if (current.isPresent()) {
      UserSession session = usable(current.get(), now);
      String next = SecureTokens.newToken();
      session.rotateRefresh(SecureTokens.sha256(next), now);
      return issue(session, next, now);
    }
    UserSession replaced =
        sessions
            .findFirstByPreviousRefreshHash(hash)
            .orElseThrow(() -> new BadCredentialsException(SESSION_OVER));
    if (replaced.getEndedAt() == null
        && replaced.getRefreshedAt() != null
        && now.isBefore(replaced.getRefreshedAt().plus(properties.refreshGrace()))) {
      return issue(usable(replaced, now), null, now);
    }
    if (replaced.end(now, SessionEndReason.TOKEN_REUSED)) {
      audit.recordIndependently(
          replaced.getUsername(),
          ENTITY,
          replaced.getUsername(),
          AuditAction.LOGOUT,
          "Session ended: a replaced refresh token was presented again");
    }
    throw new BadCredentialsException(SESSION_OVER);
  }

  /**
   * Life of a new access token: the parameter {@code ACCESS_TOKEN_MINUTES}, else {@code
   * brokerverse.security.access-token-validity}.
   *
   * @return life
   */
  public Duration accessValidity() {
    int minutes =
        parameters.intValue(
            "ACCESS_TOKEN_MINUTES", (int) properties.accessTokenValidity().toMinutes());
    return Duration.ofMinutes(Math.max(1, minutes));
  }

  private Duration accessValidity(Instant now, Instant sessionEnd) {
    Duration validity = accessValidity();
    Duration left = Duration.between(now, sessionEnd);
    return left.compareTo(validity) < 0 ? left : validity;
  }

  private Tokens issue(UserSession session, String refresh, Instant now) {
    JwtTokenService.IssuedToken access =
        tokens.issueAccess(
            session.getUsername(),
            session.getSessionId(),
            accessValidity(now, session.getExpiresAt()));
    return new Tokens(
        session.getSessionId(),
        session.getUsername(),
        access.token(),
        access.expiresAt(),
        refresh,
        session.getExpiresAt());
  }

  /** The session when it may be renewed; otherwise it is ended with the reason and refused. */
  private UserSession usable(UserSession session, Instant now) {
    if (session.getEndedAt() != null) {
      throw new BadCredentialsException(SESSION_OVER);
    }
    SessionEndReason reason = reasonToEnd(session, now);
    if (reason != null) {
      session.end(now, reason);
      throw new BadCredentialsException(SESSION_OVER);
    }
    return session;
  }

  private SessionEndReason reasonToEnd(UserSession session, Instant now) {
    if (!now.isBefore(session.getExpiresAt())) {
      return SessionEndReason.EXPIRED;
    }
    Duration idle =
        Duration.ofMinutes(
                parameters.intValue(
                    SystemParameterService.SESSION_TIMEOUT_MINUTES, DEFAULT_IDLE_MINUTES))
            .plusSeconds(UserSessionLog.TOUCH_INTERVAL_SECONDS);
    if (!now.isBefore(session.getLastSeenAt().plus(idle))) {
      return SessionEndReason.IDLE_TIMEOUT;
    }
    AppUser user = users.findByUsernameIgnoreCase(session.getUsername()).orElse(null);
    if (user == null || !user.isEnabled()) {
      return SessionEndReason.ADMIN_ENDED;
    }
    return user.isLocked() ? SessionEndReason.LOCKED : null;
  }

  /**
   * The tokens of a session.
   *
   * @param sessionId session id
   * @param username user
   * @param accessToken access token
   * @param accessExpiresAt expiry of the access token
   * @param refreshToken new refresh token, null when the current cookie stays
   * @param sessionExpiresAt absolute end of the session
   */
  public record Tokens(
      String sessionId,
      String username,
      String accessToken,
      Instant accessExpiresAt,
      String refreshToken,
      Instant sessionExpiresAt) {}
}
