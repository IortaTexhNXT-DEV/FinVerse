package com.iortatechnxt.brokerverse.security.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link UserSession}. */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

  /**
   * The session of a token id.
   *
   * @param sessionId token id ({@code jti})
   * @return session
   */
  Optional<UserSession> findBySessionId(String sessionId);

  /**
   * The latest sessions of a user, newest first.
   *
   * @param username user
   * @return up to 50 sessions
   */
  List<UserSession> findTop50ByUsernameIgnoreCaseOrderByIssuedAtDesc(String username);

  /**
   * Whether a user has an open session (not ended and not expired).
   *
   * @param username user
   * @param now time
   * @return true when online
   */
  boolean existsByUsernameIgnoreCaseAndEndedAtIsNullAndExpiresAtAfter(String username, Instant now);
}
