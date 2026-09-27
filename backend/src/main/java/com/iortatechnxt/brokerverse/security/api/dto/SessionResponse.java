package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.security.domain.UserSession;
import java.time.Instant;

/**
 * A sign-in session of the session log (UAM-NFR-35; FR-UA-004). The session id is the token id; it
 * cannot be used to sign in.
 *
 * @param sessionId session (token) id
 * @param username user
 * @param issuedAt sign-in
 * @param lastSeenAt last activity (updated at most every five minutes)
 * @param expiresAt end of the token
 * @param endedAt end of the session, null while open
 * @param endReason LOGOUT, IDLE_TIMEOUT, EXPIRED, ADMIN_ENDED or LOCKED
 * @param open whether the session is open now
 */
public record SessionResponse(
    String sessionId,
    String username,
    Instant issuedAt,
    Instant lastSeenAt,
    Instant expiresAt,
    Instant endedAt,
    String endReason,
    boolean open) {

  /**
   * Maps a session.
   *
   * @param s session
   * @param now time the list is shown
   * @return response
   */
  public static SessionResponse from(UserSession s, Instant now) {
    return new SessionResponse(
        s.getSessionId(),
        s.getUsername(),
        s.getIssuedAt(),
        s.getLastSeenAt(),
        s.getExpiresAt(),
        s.getEndedAt(),
        s.getEndReason() == null ? null : s.getEndReason().name(),
        s.isOpen(now));
  }
}
