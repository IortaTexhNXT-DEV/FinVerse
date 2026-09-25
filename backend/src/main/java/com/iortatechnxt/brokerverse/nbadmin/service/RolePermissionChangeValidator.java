package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Checks a role-permission change request (PMADD05 AC3, PRODUCT_MAINTENANCE_DESIGN section 6.3):
 * the role exists, every permission is known, and the change grants or withdraws at least one
 * permission. Permissions the role already holds are dropped from "added" and permissions it does
 * not hold from "removed", so the stored request shows the effective change.
 */
@Component
public class RolePermissionChangeValidator {

  /** Longest comma-separated permission list a request stores. */
  private static final int MAX_LIST_LENGTH = 2000;

  private static final Set<String> KNOWN =
      Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private final RoleRepository roles;

  /**
   * Creates the validator.
   *
   * @param roles roles
   */
  public RolePermissionChangeValidator(RoleRepository roles) {
    this.roles = roles;
  }

  /**
   * Validates and normalises a MODIFY_ROLE_PERMISSIONS request.
   *
   * @param c request content
   * @return normalised content (effective added / removed permissions)
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    RolePermissionChange change = c.permissionChange();
    if (change == null || change.roleCode() == null || change.roleCode().isBlank()) {
      throw new BusinessRuleException("ACCESS_ROLE", "Select the role to change");
    }
    String code = change.roleCode().trim();
    Role role =
        roles
            .findByCode(code)
            .orElseThrow(
                () -> new BusinessRuleException("ACCESS_UNKNOWN_ROLE", "Unknown role(s): " + code));
    requireKnown(change.added());
    requireKnown(change.removed());
    Set<String> overlap = new TreeSet<>(change.added());
    overlap.retainAll(change.removed());
    if (!overlap.isEmpty()) {
      throw new BusinessRuleException(
          "ACCESS_PERMISSION_CONFLICT", "Permission(s) both added and removed: " + overlap);
    }
    Set<String> current =
        role.getPermissions().stream().map(Enum::name).collect(Collectors.toSet());
    Set<String> added = new TreeSet<>(change.added());
    added.removeAll(current);
    Set<String> removed = new TreeSet<>(change.removed());
    removed.retainAll(current);
    RolePermissionChange effective = new RolePermissionChange(role.getCode(), added, removed);
    RequestedRole data = roleDataChange(role, c.role());
    if (effective.isEmpty() && data == null) {
      throw new BusinessRuleException(
          "ACCESS_NO_PERMISSION_CHANGE",
          "The request does not change the permissions of role " + role.getCode());
    }
    requireStorable(added);
    requireStorable(removed);
    return AccessRequestContent.groupProfile(
        AccessRequestType.MODIFY_ROLE_PERMISSIONS, effective, data, c.justification());
  }

  /**
   * The name, description and level a change sets, when any differs from the role (BRD 3.002.2).
   *
   * @param role the role today
   * @param requested requested data, null for none
   * @return requested data, or null when it changes nothing
   */
  private static RequestedRole roleDataChange(Role role, RequestedRole requested) {
    if (requested == null) {
      return null;
    }
    boolean changes =
        differs(requested.name(), role.getName())
            || differs(requested.description(), role.getDescription())
            || differs(requested.privilegeLevel(), role.getPrivilegeLevel());
    return changes ? requested : null;
  }

  private static boolean differs(Object requested, Object current) {
    return requested != null && !Objects.equals(requested, current);
  }

  /**
   * Checks that every permission is known.
   *
   * @param permissions permission codes
   */
  static void requireKnown(Set<String> permissions) {
    Set<String> unknown = new TreeSet<>(permissions);
    unknown.removeAll(KNOWN);
    if (!unknown.isEmpty()) {
      throw new BusinessRuleException(
          "ACCESS_UNKNOWN_PERMISSION", "Unknown permission(s): " + unknown);
    }
  }

  /**
   * Checks that a permission list fits the request.
   *
   * @param permissions permission codes
   */
  static void requireStorable(Set<String> permissions) {
    if (String.join(",", permissions).length() > MAX_LIST_LENGTH) {
      throw new BusinessRuleException(
          "ACCESS_PERMISSION_LIST_TOO_LONG", "Split the change into several requests");
    }
  }
}
