package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A user as seen when preparing an access request: the current values shown next to the requested
 * ones (FR-UA-012, FR-UA-030).
 *
 * @param username user name
 * @param fullName full name
 * @param enabled enabled flag
 * @param roleCodes current roles
 * @param email e-mail
 * @param homeBranchId home branch
 * @param windowsId Windows ID
 * @param businessUnitCode business unit group
 * @param userLevel user level
 * @param locked whether the account is locked
 */
public record UserAccessResponse(
    String username,
    String fullName,
    boolean enabled,
    Set<String> roleCodes,
    String email,
    Long homeBranchId,
    String windowsId,
    String businessUnitCode,
    String userLevel,
    boolean locked) {

  /**
   * Maps a user.
   *
   * @param u user
   * @return response
   */
  public static UserAccessResponse from(AppUser u) {
    return new UserAccessResponse(
        u.getUsername(),
        u.getFullName(),
        u.isEnabled(),
        u.getRoles().stream().map(Role::getCode).collect(Collectors.toCollection(TreeSet::new)),
        u.getEmail(),
        u.getHomeBranchId(),
        u.getWindowsId(),
        u.getBusinessUnitCode(),
        u.getUserLevel(),
        u.isLocked());
  }
}
