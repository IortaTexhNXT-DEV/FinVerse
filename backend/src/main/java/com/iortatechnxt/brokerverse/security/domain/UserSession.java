package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A sign-in session, keyed by the token id ({@code jti}) of its access token: issued, last seen,
 * ended and why (UAM-NFR-35 logout logging; the "online" status, UQ13; a later single-session rule,
 * UQ09).
 */
@Entity
@Table(name = "sec_user_session")
public class UserSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "session_id", nullable = false, updatable = false, length = 64)
  private String sessionId;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "end_reason", length = 20)
  private SessionEndReason endReason;

  @Column(name = "refresh_hash", length = 64)
  private String refreshHash;

  @Column(name = "previous_refresh_hash", length = 64)
  private String previousRefreshHash;

  @Column(name = "refreshed_at")
  private Instant refreshedAt;

  @Column(name = "sign_in_method", nullable = false, updatable = false, length = 10)
  private String signInMethod = "PASSWORD";

  @Column(name = "second_factor", nullable = false, updatable = false)
  private boolean secondFactor;

  protected UserSession() {}

  /**
   * Opens a session.
   *
   * @param sessionId token id ({@code jti})
   * @param username user
   * @param issuedAt sign-in time
   * @param expiresAt token expiry
   */
  public UserSession(String sessionId, String username, Instant issuedAt, Instant expiresAt) {
    this.sessionId = sessionId;
    this.username = username;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.lastSeenAt = issuedAt;
  }

  /**
   * Opens a session with the way it was opened (V1180).
   *
   * @param sessionId session id
   * @param username user
   * @param issuedAt sign-in time
   * @param expiresAt absolute end of the session
   * @param signInMethod PASSWORD, OIDC or SAML
   * @param secondFactor whether a second factor was checked
   * @param refreshHash SHA-256 of the first refresh token
   */
  public UserSession(
      String sessionId,
      String username,
      Instant issuedAt,
      Instant expiresAt,
      String signInMethod,
      boolean secondFactor,
      String refreshHash) {
    this(sessionId, username, issuedAt, expiresAt);
    this.signInMethod = signInMethod;
    this.secondFactor = secondFactor;
    this.refreshHash = refreshHash;
  }

  /**
   * Replaces the refresh token of the session: the current one becomes the previous one (still
   * accepted during the grace period, for a second tab renewing at the same moment).
   *
   * @param newHash SHA-256 of the new refresh token
   * @param when time of the renewal
   */
  public void rotateRefresh(String newHash, Instant when) {
    this.previousRefreshHash = refreshHash;
    this.refreshHash = newHash;
    this.refreshedAt = when;
  }

  /**
   * Records activity, at most once per interval (the session log is not a request log).
   *
   * @param when time of the request
   * @param intervalSeconds minimum seconds between two updates
   * @return true when the time was recorded
   */
  public boolean touch(Instant when, long intervalSeconds) {
    if (endedAt != null || when.isBefore(lastSeenAt.plusSeconds(intervalSeconds))) {
      return false;
    }
    this.lastSeenAt = when;
    return true;
  }

  /**
   * Ends the session; a session ends once.
   *
   * @param when end time
   * @param reason why it ended
   * @return true when it was open
   */
  public boolean end(Instant when, SessionEndReason reason) {
    if (endedAt != null) {
      return false;
    }
    this.endedAt = when;
    this.endReason = reason;
    return true;
  }

  /**
   * Whether the session is open at a time (not ended, not expired).
   *
   * @param now time
   * @return true when open
   */
  public boolean isOpen(Instant now) {
    return endedAt == null && now.isBefore(expiresAt);
  }

  public Long getId() {
    return id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getUsername() {
    return username;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public SessionEndReason getEndReason() {
    return endReason;
  }

  /**
   * SHA-256 of the current refresh token.
   *
   * @return hash, null for a session without refresh
   */
  public String getRefreshHash() {
    return refreshHash;
  }

  /**
   * SHA-256 of the refresh token the current one replaced.
   *
   * @return hash, null before the first renewal
   */
  public String getPreviousRefreshHash() {
    return previousRefreshHash;
  }

  /**
   * Time of the last renewal.
   *
   * @return time, null before the first renewal
   */
  public Instant getRefreshedAt() {
    return refreshedAt;
  }

  /**
   * How the session was opened.
   *
   * @return PASSWORD, OIDC or SAML
   */
  public String getSignInMethod() {
    return signInMethod;
  }

  /**
   * Whether a second factor was checked at the sign-in.
   *
   * @return true with a second factor
   */
  public boolean isSecondFactor() {
    return secondFactor;
  }
}
