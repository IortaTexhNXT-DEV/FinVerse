package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rules on the state of an existing user (FR-UA-012 to FR-UA-014): the user exists, a deactivation
 * needs an enabled user and a reactivation a disabled or locked one, and a modification changes at
 * least one attribute or the roles.
 */
final class UserStateRules {

  private static final String USER = "User ";

  private UserStateRules() {}

  static void requireState(AccessRequestType type, String username, AppUser user) {
    if (user == null) {
      throw new BusinessRuleException("ACCESS_UNKNOWN_USER", USER + username + " does not exist");
    }
    if (type == AccessRequestType.DISABLE_USER && !user.isEnabled()) {
      throw new BusinessRuleException(
          "ACCESS_USER_ALREADY_INACTIVE", USER + username + " is already deactivated");
    }
    if (type == AccessRequestType.ENABLE_USER && isActive(user)) {
      throw new BusinessRuleException(
          "ACCESS_USER_ALREADY_ACTIVE", USER + username + " is already active");
    }
  }

  private static boolean isActive(AppUser user) {
    return user.isEnabled() && !user.isLocked();
  }

  static void requireChange(AccessRequestContent c, AppUser user) {
    if (c.type() == AccessRequestType.MODIFY_USER && !changes(c, user)) {
      throw new BusinessRuleException(
          "ACCESS_NOTHING_CHANGED", "The request does not change the user");
    }
  }

  private static boolean changes(AccessRequestContent c, AppUser user) {
    return dataChanges(c, user) || attributesChange(c.userData(), user) || rolesChange(c, user);
  }

  private static boolean rolesChange(AccessRequestContent c, AppUser user) {
    return !c.roleCodes().isEmpty() && !c.roleCodes().equals(roleCodes(user));
  }

  private static boolean dataChanges(AccessRequestContent c, AppUser user) {
    return differs(c.fullName(), user.getFullName())
        || differs(c.email(), user.getEmail())
        || differs(c.homeBranchId(), user.getHomeBranchId());
  }

  private static boolean attributesChange(RequestedUserData d, AppUser user) {
    return differs(d.windowsId(), user.getWindowsId())
        || differs(d.businessUnitCode(), user.getBusinessUnitCode())
        || differs(d.userLevel(), user.getUserLevel());
  }

  private static boolean differs(Object requested, Object current) {
    return requested != null && !Objects.equals(requested, current);
  }

  /**
   * The role codes of a user.
   *
   * @param user user
   * @return codes
   */
  static Set<String> roleCodes(AppUser user) {
    return user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
  }
}
