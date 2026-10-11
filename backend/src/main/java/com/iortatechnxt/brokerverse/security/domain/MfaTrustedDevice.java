package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A device remembered for the second factor until it expires (parameter {@code
 * MFA_REMEMBER_DEVICE_DAYS}, off as delivered); only the SHA-256 of its token is kept.
 */
@Entity
@Table(name = "sec_mfa_trusted_device")
public class MfaTrustedDevice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  protected MfaTrustedDevice() {}

  /**
   * Remembers a device.
   *
   * @param username user
   * @param tokenHash SHA-256 of the device token
   * @param createdAt time
   * @param expiresAt end of the trust
   */
  public MfaTrustedDevice(String username, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.username = username;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public String getUsername() {
    return username;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
