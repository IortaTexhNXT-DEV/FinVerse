package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A single-use recovery code of the second factor (V1181); only its SHA-256 is kept. */
@Entity
@Table(name = "sec_mfa_recovery_code")
public class MfaRecoveryCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "code_hash", nullable = false, updatable = false, length = 64)
  private String codeHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected MfaRecoveryCode() {}

  /**
   * A new code.
   *
   * @param username user
   * @param codeHash SHA-256 of the code
   * @param createdAt creation time
   */
  public MfaRecoveryCode(String username, String codeHash, Instant createdAt) {
    this.username = username;
    this.codeHash = codeHash;
    this.createdAt = createdAt;
  }

  /**
   * Uses the code.
   *
   * @param when time of use
   */
  public void use(Instant when) {
    this.usedAt = when;
  }

  public String getUsername() {
    return username;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public Instant getUsedAt() {
    return usedAt;
  }
}
