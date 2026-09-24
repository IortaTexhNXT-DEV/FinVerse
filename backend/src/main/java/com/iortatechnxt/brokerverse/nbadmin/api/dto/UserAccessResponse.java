package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A user as seen when preparing an access request.
 *
 * @param username user name
 * @param fullName full name
 * @param enabled enabled flag
 * @param roleCodes current roles
 */
public record UserAccessResponse(
    String username, String fullName, boolean enabled, Set<String> roleCodes) {

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
        u.getRoles().stream().map(Role::getCode).collect(Collectors.toCollection(TreeSet::new)));
  }
}
