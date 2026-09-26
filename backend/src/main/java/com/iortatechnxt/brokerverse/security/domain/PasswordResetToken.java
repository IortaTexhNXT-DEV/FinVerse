package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A "Forgot password?" link (UAM-NFR-37; FR-UA-005 R3): single use and valid for a limited time.
 * Only the SHA-256 of the token is kept, so the link cannot be rebuilt from the database.
 */
@Entity
@Table(name = "sec_password_reset_token")
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected PasswordResetToken() {}

  /**
   * Issues a link.
   *
   * @param tokenHash SHA-256 (hex) of the token sent to the user
   * @param username user
   * @param requestedAt request time
   * @param expiresAt end of validity
   */
  public PasswordResetToken(
      String tokenHash, String username, Instant requestedAt, Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.username = username;
    this.requestedAt = requestedAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Whether the link was used (or withdrawn by a newer one).
   *
   * @return true once used
   */
  public boolean isUsed() {
    return usedAt != null;
  }

  /**
   * Whether the link has expired at a time.
   *
   * @param now time
   * @return true when expired
   */
  public boolean isExpired(Instant now) {
    return !now.isBefore(expiresAt);
  }

  /**
   * Uses the link, once; a newer link withdraws the older ones the same way.
   *
   * @param when time
   */
  public void use(Instant when) {
    if (usedAt == null) {
      usedAt = when;
    }
  }

  public Long getId() {
    return id;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public String getUsername() {
    return username;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }
}
