package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks of a group-profile request (BRD 3.002.1-4; FR-UA-040 to FR-UA-043): a new profile has a
 * free code of capital letters, digits and underscores, a name and at least one known permission; a
 * deactivation needs an active profile other than SYSADMIN; a reactivation an inactive profile.
 * Permission changes of a profile (PMADD05) are checked by {@link RolePermissionChangeValidator}.
 */
@Component
@Transactional(readOnly = true)
public class GroupProfileRequestValidator {

  /** The System Administrator profile, never deactivated (FR-UA-042 R2). */
  public static final String SYSADMIN = "SYSADMIN";

  private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{1,40}");
  private static final String PROFILE = "Group profile ";

  private final RoleRepository roles;
  private final RolePermissionChangeValidator permissionChanges;

  /**
   * Creates the validator.
   *
   * @param roles roles
   * @param permissionChanges role-permission change checks (PMADD05)
   */
  public GroupProfileRequestValidator(
      RoleRepository roles, RolePermissionChangeValidator permissionChanges) {
    this.roles = roles;
    this.permissionChanges = permissionChanges;
  }

  /**
   * Validates and normalises a group-profile request.
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    return switch (c.type()) {
      case CREATE_ROLE -> create(c);
      case DEACTIVATE_ROLE -> activation(c, true);
      case REACTIVATE_ROLE -> activation(c, false);
      default -> permissionChanges.validate(c);
    };
  }

  private AccessRequestContent create(AccessRequestContent c) {
    String code = requireNewCode(c.roleCode());
    RequestedRole role = c.role();
    if (role == null || role.name() == null) {
      throw new BusinessRuleException("ACCESS_ROLE_NAME", "Enter the name of the group profile");
    }
    Set<String> permissions = c.permissionChange().added();
    if (permissions.isEmpty()) {
      throw new BusinessRuleException("ACCESS_NO_PERMISSION", "Select at least one permission");
    }
    RolePermissionChangeValidator.requireKnown(permissions);
    RolePermissionChangeValidator.requireStorable(permissions);
    RequestedRole data =
        new RequestedRole(
            role.name(),
            role.description(),
            role.privilegeLevel() == null ? PrivilegeLevel.STANDARD : role.privilegeLevel());
    return AccessRequestContent.groupProfile(
        AccessRequestType.CREATE_ROLE,
        new RolePermissionChange(code, permissions, Set.of()),
        data,
        c.justification());
  }

  private String requireNewCode(String raw) {
    String code = requireCode(raw);
    if (!CODE.matcher(code).matches()) {
      throw new BusinessRuleException(
          "ACCESS_ROLE_CODE",
          "The profile code has up to 40 capital letters, digits or underscores");
    }
    if (roles.findByCode(code).isPresent()) {
      throw new BusinessRuleException("DUPLICATE", "Role " + code + " already exists");
    }
    return code;
  }

  private AccessRequestContent activation(AccessRequestContent c, boolean deactivate) {
    String code = requireCode(c.roleCode());
    Role role =
        roles
            .findByCode(code)
            .orElseThrow(
                () -> new BusinessRuleException("ACCESS_UNKNOWN_ROLE", "Unknown role(s): " + code));
    if (deactivate) {
      requireDeactivatable(role);
    } else if (role.isActive()) {
      throw new BusinessRuleException(
          "ACCESS_ROLE_ALREADY_ACTIVE", PROFILE + code + " is already active");
    }
    return AccessRequestContent.groupProfile(
        c.type(), new RolePermissionChange(code, Set.of(), Set.of()), null, c.justification());
  }

  private static void requireDeactivatable(Role role) {
    if (SYSADMIN.equals(role.getCode())) {
      throw new BusinessRuleException(
          "ACCESS_ROLE_PROTECTED", PROFILE + SYSADMIN + " cannot be deactivated");
    }
    if (!role.isActive()) {
      throw new BusinessRuleException(
          "ACCESS_ROLE_ALREADY_INACTIVE", PROFILE + role.getCode() + " is already inactive");
    }
  }

  private static String requireCode(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BusinessRuleException("ACCESS_ROLE", "Select the role to change");
    }
    return raw.trim();
  }
}
