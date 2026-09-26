package com.iortatechnxt.brokerverse.security.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
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
 *
 * <p>BRD-11 adds the directory identity (Windows ID), the business unit group and user level (BRD
 * 1.002.1.1.1), the password dates and the forced change flag (UAM-NFR-36) and the last sign-out
 * (UAM-NFR-35). Deactivated roles grant nothing (BRD 3.002.3).
 */
@Entity
@Table(name = "sec_user")
public class AppUser extends BaseEntity {

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

  @Column(name = "windows_id", length = 50)
  private String windowsId;

  @Column(name = "business_unit_code", length = 40)
  private String businessUnitCode;

  @Column(name = "user_level", length = 40)
  private String userLevel;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt;

  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword;

  @Column(name = "last_logout_at")
  private Instant lastLogoutAt;

  @Column(name = "mobile_no", length = 30)
  private String mobileNo;

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
   * Collects the permissions granted by the active roles; a deactivated role grants nothing (BRD
   * 3.002.3).
   *
   * @return effective permissions
   */
  public Set<Permission> effectivePermissions() {
    Set<Permission> result = EnumSet.noneOf(Permission.class);
    roles.stream().filter(Role::isActive).forEach(r -> result.addAll(r.getPermissions()));
    return result;
  }

  /**
   * Records a password change: the change time and whether the user must change it at the next
   * sign-in (after an administrator reset or on creation; UAM-NFR-36).
   *
   * @param newHash BCrypt hash of the new password
   * @param when change time
   * @param mustChange true when the password was set by someone else
   */
  public void changePassword(String newHash, Instant when, boolean mustChange) {
    this.passwordHash = newHash;
    this.passwordChangedAt = when;
    this.mustChangePassword = mustChange;
  }

  /**
   * Records a sign-out (UAM-NFR-35).
   *
   * @param when sign-out time
   */
  public void recordLogout(Instant when) {
    this.lastLogoutAt = when;
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

  /**
   * Records a failed login; locks the account once the threshold is reached.
   *
   * @param maxFailedAttempts consecutive failures that lock the account
   */
  public void recordFailedLogin(int maxFailedAttempts) {
    recordFailedLogins(failedAttempts + 1, maxFailedAttempts);
  }

  /**
   * Records the consecutive failed logins counted across all instances (shared counter); locks the
   * account once the threshold is reached. The recorded count never decreases here.
   *
   * @param consecutiveFailures failures counted so far, including the current one
   * @param maxFailedAttempts consecutive failures that lock the account
   */
  public void recordFailedLogins(int consecutiveFailures, int maxFailedAttempts) {
    failedAttempts = Math.max(failedAttempts, consecutiveFailures);
    if (failedAttempts >= maxFailedAttempts) {
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

  public String getWindowsId() {
    return windowsId;
  }

  public void setWindowsId(String windowsId) {
    this.windowsId = windowsId;
  }

  public String getBusinessUnitCode() {
    return businessUnitCode;
  }

  public void setBusinessUnitCode(String businessUnitCode) {
    this.businessUnitCode = businessUnitCode;
  }

  public String getUserLevel() {
    return userLevel;
  }

  public void setUserLevel(String userLevel) {
    this.userLevel = userLevel;
  }

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public Instant getLastLogoutAt() {
    return lastLogoutAt;
  }

  public String getMobileNo() {
    return mobileNo;
  }

  public void setMobileNo(String mobileNo) {
    this.mobileNo = mobileNo;
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
