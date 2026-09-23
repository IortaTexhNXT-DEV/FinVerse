package com.iortatechnxt.finverse.security.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Application user.
 *
 * <p>Carries the security controls required by the GL: account lockout after repeated failed
 * logins, a home branch, and an authorization limit capping the value of batches the user may
 * authorize.
 */
@Entity
@Table(name = "sec_user")
public class AppUser extends BaseEntity {

  /** Consecutive failed logins that lock the account. */
  public static final int MAX_FAILED_ATTEMPTS = 5;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(length = 120)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private boolean locked;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "home_branch_id")
  private Long homeBranchId;

  @Column(name = "authorization_limit", precision = 19, scale = 2)
  private BigDecimal authorizationLimit;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "sec_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private final Set<Role> roles = new HashSet<>();

  protected AppUser() {}

  /**
   * Creates a user.
   *
   * @param username login name
   * @param fullName display name
   * @param passwordHash BCrypt hash
   */
  public AppUser(String username, String fullName, String passwordHash) {
    this.username = username;
    this.fullName = fullName;
    this.passwordHash = passwordHash;
  }

  /**
   * Collects the permissions granted by all roles.
   *
   * @return effective permissions
   */
  public Set<Permission> effectivePermissions() {
    Set<Permission> result = EnumSet.noneOf(Permission.class);
    roles.forEach(r -> result.addAll(r.getPermissions()));
    return result;
  }

  /**
   * Records a successful login and resets the failure counter.
   *
   * @param when login time
   */
  public void recordSuccessfulLogin(Instant when) {
    failedAttempts = 0;
    lastLoginAt = when;
  }

  /** Records a failed login; locks the account once the threshold is reached. */
  public void recordFailedLogin() {
    failedAttempts++;
    if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
      locked = true;
    }
  }

  /** Unlocks the account (administrator action). */
  public void unlock() {
    locked = false;
    failedAttempts = 0;
  }

  public String getUsername() {
    return username;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isLocked() {
    return locked;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Long getHomeBranchId() {
    return homeBranchId;
  }

  public void setHomeBranchId(Long homeBranchId) {
    this.homeBranchId = homeBranchId;
  }

  public BigDecimal getAuthorizationLimit() {
    return authorizationLimit;
  }

  public void setAuthorizationLimit(BigDecimal authorizationLimit) {
    this.authorizationLimit = authorizationLimit;
  }

  /**
   * Returns an immutable view of the assigned roles.
   *
   * @return roles
   */
  public Set<Role> getRoles() {
    return Set.copyOf(roles);
  }

  /**
   * Replaces the assigned roles.
   *
   * @param newRoles roles to assign
   */
  public void replaceRoles(Set<Role> newRoles) {
    roles.clear();
    roles.addAll(newRoles);
  }
}
