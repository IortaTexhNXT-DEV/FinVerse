package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.domain.UserSession;
import com.iortatechnxt.brokerverse.security.domain.UserSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  private static final int MAX_PAGE_SIZE = 200;

  private final UserSessionRepository sessions;
  private final AppUserRepository users;
  private final Clock clock;

  /**
   * Creates the log.
   *
   * @param sessions sessions
   * @param users users (the sweep ends the sessions of locked and disabled users)
   * @param clock clock
   */
  public UserSessionLog(UserSessionRepository sessions, AppUserRepository users, Clock clock) {
    this.sessions = sessions;
    this.users = users;
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

  /**
   * Checks the session of a token on a request and records the activity (at most every {@value
   * #TOUCH_INTERVAL_SECONDS} seconds). A token without an id, or issued before the session log, has
   * no session and is {@link SessionState#UNTRACKED}.
   *
   * @param sessionId token id, may be null
   * @return the state of the session
   */
  public SessionState check(String sessionId) {
    if (sessionId == null) {
      return SessionState.UNTRACKED;
    }
    return sessions
        .findBySessionId(sessionId)
        .map(
            s -> {
              if (s.getEndedAt() != null) {
                return SessionState.ENDED;
              }
              s.touch(clock.instant(), TOUCH_INTERVAL_SECONDS);
              return SessionState.OPEN;
            })
        .orElse(SessionState.UNTRACKED);
  }

  /**
   * Ends every open session of a user (account locked, disabled, or ended by an administrator).
   *
   * @param username user
   * @param reason why they end
   * @return the sessions ended
   */
  public List<UserSession> endAll(String username, SessionEndReason reason) {
    Instant now = clock.instant();
    return sessions.findByUsernameIgnoreCaseAndEndedAtIsNull(username).stream()
        .filter(s -> s.end(now, reason))
        .toList();
  }

  /**
   * Ends the sessions nobody will end any more (UAM-NFR-35): expired tokens (EXPIRED), sessions
   * without a request for longer than the idle limit, the browser closed without signing out
   * (IDLE_TIMEOUT), and the sessions of locked (LOCKED) and disabled (ADMIN_ENDED) users.
   *
   * @param idleLimit inactivity after which a session is ended
   * @return sessions ended, by reason
   */
  public Map<SessionEndReason, Long> sweep(Duration idleLimit) {
    Instant now = clock.instant();
    List<UserSession> open = sessions.findByEndedAtIsNull();
    Map<String, Optional<AppUser>> owners =
        open.stream()
            .map(UserSession::getUsername)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), users::findByUsernameIgnoreCase));
    return open.stream()
        .map(s -> sweepReason(s, owners.get(s.getUsername()).orElse(null), now, idleLimit))
        .flatMap(Optional::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  private static Optional<SessionEndReason> sweepReason(
      UserSession s, AppUser owner, Instant now, Duration idleLimit) {
    SessionEndReason reason = null;
    if (owner != null && owner.isLocked()) {
      reason = SessionEndReason.LOCKED;
    } else if (owner == null || !owner.isEnabled()) {
      reason = SessionEndReason.ADMIN_ENDED;
    } else if (!now.isBefore(s.getExpiresAt())) {
      reason = SessionEndReason.EXPIRED;
    } else if (!now.isBefore(s.getLastSeenAt().plus(idleLimit))) {
      reason = SessionEndReason.IDLE_TIMEOUT;
    }
    Instant when = reason == SessionEndReason.EXPIRED ? s.getExpiresAt() : now;
    return reason != null && s.end(when, reason) ? Optional.of(reason) : Optional.empty();
  }

  /**
   * Sessions for the session list, newest first.
   *
   * @param username user; null for every user
   * @param openOnly only the sessions open now
   * @param page page number (0-based)
   * @param size page size (at most 200)
   * @return sessions
   */
  @Transactional(readOnly = true)
  public Page<UserSession> list(String username, boolean openOnly, int page, int size) {
    PageRequest request =
        PageRequest.of(
            Math.max(0, page),
            Math.clamp(size, 1, MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "issuedAt"));
    if (openOnly) {
      Instant now = clock.instant();
      return username == null
          ? sessions.findByEndedAtIsNullAndExpiresAtAfter(now, request)
          : sessions.findByUsernameIgnoreCaseAndEndedAtIsNullAndExpiresAtAfter(
              username, now, request);
    }
    return username == null
        ? sessions.findAll(request)
        : sessions.findByUsernameIgnoreCase(username, request);
  }

  /**
   * User names with an open session (the "Online" status, UQ13).
   *
   * @return user names
   */
  @Transactional(readOnly = true)
  public List<String> onlineUsernames() {
    return sessions.findOnlineUsernames(clock.instant());
  }

  /**
   * One session.
   *
   * @param sessionId token id
   * @return session
   */
  @Transactional(readOnly = true)
  public Optional<UserSession> find(String sessionId) {
    return sessions.findBySessionId(sessionId);
  }

  /** State of the session of a presented token. */
  public enum SessionState {
    /** No session is kept for the token (no token id, or issued before the session log). */
    UNTRACKED,
    /** The session is open. */
    OPEN,
    /** The session was ended (sign-out, administrator, lock, idle timeout, expiry). */
    ENDED
  }
}
