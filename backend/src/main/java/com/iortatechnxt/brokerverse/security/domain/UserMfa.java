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
 * The authenticator app of a user (second factor, V1181): the TOTP secret encrypted at rest,
 * PENDING until the first code confirms the enrolment, then ACTIVE. A user has at most one of each:
 * a new enrolment stays PENDING beside the ACTIVE one until it is confirmed. The step of the last
 * code accepted is kept so that no code is accepted twice.
 */
@Entity
@Table(name = "sec_user_mfa")
public class UserMfa {

  /** Waiting for the first code. */
  public static final String PENDING = "PENDING";

  /** Confirmed; asked for at sign-in. */
  public static final String ACTIVE = "ACTIVE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(name = "secret_cipher", nullable = false, length = 200)
  private String secretCipher;

  @Column(nullable = false, length = 10)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "activated_at")
  private Instant activatedAt;

  @Column(name = "last_used_step")
  private Long lastUsedStep;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected UserMfa() {}

  /**
   * Starts an enrolment.
   *
   * @param username user
   * @param secretCipher encrypted secret
   * @param createdAt start of the enrolment
   */
  public UserMfa(String username, String secretCipher, Instant createdAt) {
    this.username = username;
    this.secretCipher = secretCipher;
    this.status = PENDING;
    this.createdAt = createdAt;
  }

  /**
   * Confirms the enrolment with the step of its first code.
   *
   * @param step step of the code
   * @param when confirmation time
   */
  public void activate(long step, Instant when) {
    this.status = ACTIVE;
    this.activatedAt = when;
    use(step, when);
  }

  /**
   * Records the step of an accepted code.
   *
   * @param step step
   * @param when time
   */
  public void use(long step, Instant when) {
    this.lastUsedStep = step;
    this.lastUsedAt = when;
  }

  /**
   * Replaces the stored secret (re-encryption with a new key).
   *
   * @param cipher encrypted secret
   */
  public void reencrypt(String cipher) {
    this.secretCipher = cipher;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getSecretCipher() {
    return secretCipher;
  }

  public String getStatus() {
    return status;
  }

  public boolean isActive() {
    return ACTIVE.equals(status);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  /**
   * Step of the last accepted code.
   *
   * @return step, {@link Long#MIN_VALUE} when none
   */
  public long getLastUsedStep() {
    return lastUsedStep == null ? Long.MIN_VALUE : lastUsedStep;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }
}
