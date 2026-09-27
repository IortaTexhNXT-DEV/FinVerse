package com.iortatechnxt.brokerverse.security.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * Sessions not ended yet (expired ones included), for the session sweep.
   *
   * @return open sessions
   */
  List<UserSession> findByEndedAtIsNull();

  /**
   * Sessions of a user not ended yet.
   *
   * @param username user
   * @return open sessions
   */
  List<UserSession> findByUsernameIgnoreCaseAndEndedAtIsNull(String username);

  /**
   * Sessions of a user (session list).
   *
   * @param username user
   * @param pageable page and order
   * @return sessions
   */
  Page<UserSession> findByUsernameIgnoreCase(String username, Pageable pageable);

  /**
   * Sessions open at a time (not ended, not expired).
   *
   * @param now time
   * @param pageable page and order
   * @return open sessions
   */
  Page<UserSession> findByEndedAtIsNullAndExpiresAtAfter(Instant now, Pageable pageable);

  /**
   * Sessions of a user open at a time.
   *
   * @param username user
   * @param now time
   * @param pageable page and order
   * @return open sessions
   */
  Page<UserSession> findByUsernameIgnoreCaseAndEndedAtIsNullAndExpiresAtAfter(
      String username, Instant now, Pageable pageable);

  /**
   * User names with an open session (the "Online" status, UQ13).
   *
   * @param now time
   * @return user names
   */
  @Query(
      "select distinct s.username from UserSession s"
          + " where s.endedAt is null and s.expiresAt > :now")
  List<String> findOnlineUsernames(@Param("now") Instant now);
}
