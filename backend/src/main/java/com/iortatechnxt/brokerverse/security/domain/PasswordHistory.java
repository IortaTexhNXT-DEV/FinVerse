package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A previous password hash of a user, kept for the reuse rule (UAM-NFR-36, {@code
 * PASSWORD_HISTORY_COUNT}).
 */
@Entity
@Table(name = "sec_password_history")
public class PasswordHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "password_hash", nullable = false, updatable = false, length = 100)
  private String passwordHash;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected PasswordHistory() {}

  /**
   * Keeps a password hash.
   *
   * @param username user
   * @param passwordHash BCrypt hash
   * @param changedAt time the password was set
   */
  public PasswordHistory(String username, String passwordHash, Instant changedAt) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.changedAt = changedAt;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
