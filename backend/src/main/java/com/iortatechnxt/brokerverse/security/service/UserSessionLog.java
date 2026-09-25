package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import com.iortatechnxt.brokerverse.security.domain.UserSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-in session log (UAM-NFR-35): a session per issued access token ({@code jti}), opened at
 * sign-in and ended at sign-out (or by an administrator, a lock, the idle timeout or the expiry).
 * It gives the "online" status of a user (UQ13) and prepares a single-session rule (UQ09). Tokens
 * issued before token ids existed have no session.
 */
@Service
@Transactional
public class UserSessionLog {

  /** Minimum seconds between two "last seen" updates of a session. */
  public static final long TOUCH_INTERVAL_SECONDS = 300;

  private final UserSessionRepository sessions;
  private final Clock clock;

  /**
   * Creates the log.
   *
   * @param sessions sessions
   * @param clock clock
   */
  public UserSessionLog(UserSessionRepository sessions, Clock clock) {
    this.sessions = sessions;
    this.clock = clock;
  }

  /**
   * Opens the session of a new token.
   *
   * @param sessionId token id ({@code jti})
   * @param username user
   * @param expiresAt token expiry
   * @return the session
   */
  public UserSession open(String sessionId, String username, Instant expiresAt) {
    return sessions.save(new UserSession(sessionId, username, clock.instant(), expiresAt));
  }

  /**
   * Records activity on a session, at most every {@value #TOUCH_INTERVAL_SECONDS} seconds.
   *
   * @param sessionId token id
   * @return true when the time was recorded
   */
  public boolean touch(String sessionId) {
    return sessionId != null
        && sessions
            .findBySessionId(sessionId)
            .map(s -> s.touch(clock.instant(), TOUCH_INTERVAL_SECONDS))
            .orElse(false);
  }

  /**
   * Ends a session.
   *
   * @param sessionId token id
   * @param reason why it ends
   * @return the session when it was open
   */
  public Optional<UserSession> end(String sessionId, SessionEndReason reason) {
    if (sessionId == null) {
      return Optional.empty();
    }
    return sessions.findBySessionId(sessionId).filter(s -> s.end(clock.instant(), reason));
  }

  /**
   * Whether a user has an open session.
   *
   * @param username user
   * @return true when online
   */
  @Transactional(readOnly = true)
  public boolean isOnline(String username) {
    return sessions.existsByUsernameIgnoreCaseAndEndedAtIsNullAndExpiresAtAfter(
        username, clock.instant());
  }

  /**
   * The latest sessions of a user, newest first.
   *
   * @param username user
   * @return up to 50 sessions
   */
  @Transactional(readOnly = true)
  public List<UserSession> sessionsOf(String username) {
    return sessions.findTop50ByUsernameIgnoreCaseOrderByIssuedAtDesc(username);
  }
}
