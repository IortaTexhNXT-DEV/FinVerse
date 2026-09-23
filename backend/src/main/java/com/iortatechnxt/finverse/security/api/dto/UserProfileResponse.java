package com.iortatechnxt.finverse.security.api.dto;

import com.iortatechnxt.finverse.security.domain.AppUser;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.security.domain.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * User profile including effective permissions (drives menu visibility in the UI).
 *
 * @param id id
 * @param username user name
 * @param fullName full name
 * @param email email
 * @param enabled enabled flag
 * @param locked locked flag
 * @param homeBranchId home branch
 * @param authorizationLimit authorization limit
 * @param lastLoginAt last login
 * @param roles role codes
 * @param permissions effective permissions
 */
public record UserProfileResponse(
    Long id,
    String username,
    String fullName,
    String email,
    boolean enabled,
    boolean locked,
    Long homeBranchId,
    BigDecimal authorizationLimit,
    Instant lastLoginAt,
    Set<String> roles,
    Set<Permission> permissions) {

  /**
   * Maps an entity.
   *
   * @param u user
   * @return profile
   */
  public static UserProfileResponse from(AppUser u) {
    return new UserProfileResponse(
        u.getId(),
        u.getUsername(),
        u.getFullName(),
        u.getEmail(),
        u.isEnabled(),
        u.isLocked(),
        u.getHomeBranchId(),
        u.getAuthorizationLimit(),
        u.getLastLoginAt(),
        u.getRoles().stream().map(Role::getCode).collect(Collectors.toCollection(TreeSet::new)),
        u.effectivePermissions());
  }
}
