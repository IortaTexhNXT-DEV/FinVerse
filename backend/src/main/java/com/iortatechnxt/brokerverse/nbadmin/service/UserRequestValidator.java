package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full checks of a request about an internal user on submission and again at approval (BRD
 * 1.002-1.005; FR-UA-011 to FR-UA-014): the user ID format for a new user (USER_ID_PATTERN), the
 * user exists or not, is enabled or not, the group profiles exist and are active, the Windows ID is
 * free, a modification changes something, and nobody requests a change of their own roles.
 */
@Component
@Transactional(readOnly = true)
public class UserRequestValidator {

  /** Letters, digits, dots, dashes and underscores (the user name rule of the platform). */
  static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._-]{3,50}");

  private static final String USER = "User ";

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final AccessSettings settings;
  private final CurrentUser currentUser;

  /**
   * Creates the validator.
   *
   * @param users users
   * @param roles roles
   * @param settings parameters (user ID format)
   * @param currentUser current user (the requester)
   */
  public UserRequestValidator(
      AppUserRepository users,
      RoleRepository roles,
      AccessSettings settings,
      CurrentUser currentUser) {
    this.users = users;
    this.roles = roles;
    this.settings = settings;
    this.currentUser = currentUser;
  }

  /**
   * Validates and normalises a request about an internal user.
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    String username = requireUsername(c.username());
    AppUser user = users.findByUsernameIgnoreCase(username).orElse(null);
    if (c.type() == AccessRequestType.CREATE_USER) {
      requireNewUser(c, username, user);
    } else {
      requireState(c.type(), username, user);
      username = user.getUsername();
    }
    Set<String> roleCodes = requireRoles(c);
    requireFreeWindowsId(c.userData(), user);
    AccessRequestContent clean =
        new AccessRequestContent(
            c.type(),
            username,
            trimmed(c.fullName()),
            trimmed(c.email()),
            roleCodes,
            c.homeBranchId(),
            c.justification(),
            null,
            c.userData(),
            null,
            null,
            c.effectiveFrom());
    if (user != null) {
      requireNotOwnRoles(clean, user);
      requireChange(clean, user);
    }
    return clean;
  }

  /**
   * The user name, trimmed, in the platform format.
   *
   * @param raw user name as entered
   * @return trimmed user name
   */
  static String requireUsername(String raw) {
    String username = raw == null ? "" : raw.trim();
    if (!USERNAME.matcher(username).matches()) {
      throw new BusinessRuleException(
          "ACCESS_USERNAME",
          "The user name has 3 to 50 letters, digits, dots, dashes or underscores");
    }
    return username;
  }

  private void requireNewUser(AccessRequestContent c, String username, AppUser existing) {
    if (existing != null) {
      throw new BusinessRuleException("ACCESS_USER_EXISTS", USER + username + " already exists");
    }
    String pattern = settings.userIdPattern();
    if (!pattern.isBlank() && !matches(pattern, username)) {
      throw new BusinessRuleException(
          "ACCESS_USER_ID_FORMAT", "The user ID must follow the format " + pattern);
    }
    if (c.fullName() == null || c.fullName().isBlank()) {
      throw new BusinessRuleException("ACCESS_FULL_NAME", "Enter the full name of the new user");
    }
  }

  private static boolean matches(String pattern, String username) {
    try {
      return Pattern.compile(pattern).matcher(username).matches();
    } catch (PatternSyntaxException e) {
      return true;
    }
  }

  private static void requireState(AccessRequestType type, String username, AppUser user) {
    if (user == null) {
      throw new BusinessRuleException("ACCESS_UNKNOWN_USER", USER + username + " does not exist");
    }
    if (type == AccessRequestType.DISABLE_USER && !user.isEnabled()) {
      throw new BusinessRuleException(
          "ACCESS_USER_ALREADY_INACTIVE", USER + username + " is already deactivated");
    }
    if (type == AccessRequestType.ENABLE_USER && user.isEnabled() && !user.isLocked()) {
      throw new BusinessRuleException(
          "ACCESS_USER_ALREADY_ACTIVE", USER + username + " is already active");
    }
  }

  private Set<String> requireRoles(AccessRequestContent c) {
    if (!c.type().carriesUserRoles()) {
      return Set.of();
    }
    boolean required = c.type() != AccessRequestType.MODIFY_USER;
    if (c.roleCodes().isEmpty()) {
      if (required) {
        throw new BusinessRuleException("ACCESS_ROLES", "Select at least one role");
      }
      return Set.of();
    }
    List<Role> found = roles.findByCodeIn(c.roleCodes());
    Set<String> unknown = new TreeSet<>(c.roleCodes());
    found.forEach(r -> unknown.remove(r.getCode()));
    if (!unknown.isEmpty()) {
      throw new BusinessRuleException("ACCESS_UNKNOWN_ROLE", "Unknown role(s): " + unknown);
    }
    found.stream()
        .filter(r -> !r.isActive())
        .findFirst()
        .ifPresent(
            r -> {
              throw new BusinessRuleException(
                  "ACCESS_ROLE_INACTIVE", "Group profile " + r.getCode() + " is not active");
            });
    return new TreeSet<>(c.roleCodes());
  }

  private void requireFreeWindowsId(RequestedUserData data, AppUser user) {
    String windowsId = data.windowsId();
    if (windowsId == null) {
      return;
    }
    boolean used =
        user == null
            ? users.existsByWindowsIdIgnoreCase(windowsId)
            : users.existsByWindowsIdIgnoreCaseAndIdNot(windowsId, user.getId());
    if (used) {
      throw new BusinessRuleException(
          "ACCESS_WINDOWS_ID_IN_USE", "Windows ID " + windowsId + " belongs to another user");
    }
  }

  private void requireNotOwnRoles(AccessRequestContent c, AppUser user) {
    if (c.type().carriesUserRoles()
        && !c.roleCodes().isEmpty()
        && CurrentUser.sameUser(currentUser.username(), user.getUsername())
        && !c.roleCodes().equals(roleCodes(user))) {
      throw new BusinessRuleException("SELF_ROLE_CHANGE", "You cannot change your own roles");
    }
  }

  private static void requireChange(AccessRequestContent c, AppUser user) {
    if (c.type() == AccessRequestType.MODIFY_USER && !changes(c, user)) {
      throw new BusinessRuleException(
          "ACCESS_NOTHING_CHANGED", "The request does not change the user");
    }
  }

  private static boolean changes(AccessRequestContent c, AppUser user) {
    return dataChanges(c, user)
        || attributesChange(c.userData(), user)
        || (!c.roleCodes().isEmpty() && !c.roleCodes().equals(roleCodes(user)));
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

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
