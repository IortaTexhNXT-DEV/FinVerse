package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Checks a user access request before it is submitted: the user must not exist for a creation and
 * must exist for any other request, roles must exist, and a creation needs a full name.
 * Role-permission changes (PMADD05) are checked by {@link RolePermissionChangeValidator}.
 */
@Component
public class AccessRequestValidator {

  private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._-]{3,50}");

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final RolePermissionChangeValidator permissionChanges;

  /**
   * Creates the validator.
   *
   * @param users users
   * @param roles roles
   * @param permissionChanges role-permission change checks (PMADD05)
   */
  public AccessRequestValidator(
      AppUserRepository users,
      RoleRepository roles,
      RolePermissionChangeValidator permissionChanges) {
    this.users = users;
    this.roles = roles;
    this.permissionChanges = permissionChanges;
  }

  /**
   * Validates and normalises a request.
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    if (c.type() == AccessRequestType.MODIFY_ROLE_PERMISSIONS) {
      return permissionChanges.validate(c);
    }
    String username = c.username() == null ? "" : c.username().trim();
    if (!USERNAME.matcher(username).matches()) {
      throw new BusinessRuleException(
          "ACCESS_USERNAME",
          "The user name has 3 to 50 letters, digits, dots, dashes or underscores");
    }
    requireUserState(c, username);
    Set<String> roleCodes = requireRoles(c);
    return new AccessRequestContent(
        c.type(),
        username,
        trimmed(c.fullName()),
        trimmed(c.email()),
        roleCodes,
        c.homeBranchId(),
        c.justification().trim());
  }

  private void requireUserState(AccessRequestContent c, String username) {
    boolean exists = users.existsByUsernameIgnoreCase(username);
    if (c.type() != AccessRequestType.CREATE_USER) {
      if (!exists) {
        throw new BusinessRuleException(
            "ACCESS_UNKNOWN_USER", "User " + username + " does not exist");
      }
      return;
    }
    requireNew(username, exists);
    if (c.fullName() == null || c.fullName().isBlank()) {
      throw new BusinessRuleException("ACCESS_FULL_NAME", "Enter the full name of the new user");
    }
  }

  private static void requireNew(String username, boolean exists) {
    if (exists) {
      throw new BusinessRuleException("ACCESS_USER_EXISTS", "User " + username + " already exists");
    }
  }

  private Set<String> requireRoles(AccessRequestContent c) {
    boolean needsRoles =
        c.type() == AccessRequestType.CREATE_USER || c.type() == AccessRequestType.MODIFY_ROLES;
    if (!needsRoles) {
      return Set.of();
    }
    if (c.roleCodes().isEmpty()) {
      throw new BusinessRuleException("ACCESS_ROLES", "Select at least one role");
    }
    Set<String> known =
        roles.findByCodeIn(c.roleCodes()).stream().map(Role::getCode).collect(Collectors.toSet());
    Set<String> unknown = new TreeSet<>(c.roleCodes());
    unknown.removeAll(known);
    if (!unknown.isEmpty()) {
      throw new BusinessRuleException("ACCESS_UNKNOWN_ROLE", "Unknown role(s): " + unknown);
    }
    return new TreeSet<>(c.roleCodes());
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
