package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Applies an approved access request through the security administration service, so the same
 * rules, audit entries and access change log rows (with the request number and approver, BRD
 * 4.003.1) apply as for a change made on the Users or Roles screen. A role-permission change
 * (PMADD05) adds and removes the requested permissions on the role as it is when applied. External
 * (portal) users are provisioned through {@link ExternalUserProvisioner} (decision D7).
 */
@Component
public class AccessChangeApplier {

  private final UserAdminService userAdmin;
  private final RoleRepository roles;
  private final TemporaryPasswords passwords;
  private final ExternalUserApplier externalUsers;

  /**
   * Creates the applier.
   *
   * @param userAdmin user administration
   * @param roles roles
   * @param passwords temporary password generator
   * @param externalUsers external (portal) users
   */
  public AccessChangeApplier(
      UserAdminService userAdmin,
      RoleRepository roles,
      TemporaryPasswords passwords,
      ExternalUserApplier externalUsers) {
    this.userAdmin = userAdmin;
    this.roles = roles;
    this.passwords = passwords;
    this.externalUsers = externalUsers;
  }

  /**
   * Applies the request on the authority of its (last) approver.
   *
   * @param r approved request
   * @return temporary password of a created internal user, null otherwise
   */
  public String apply(AccessRequest r) {
    ChangeAuthority authority = ChangeAuthority.request(r.getRequestNo(), r.getDecidedBy());
    if (r.getUserType() == AccessUserType.EXTERNAL) {
      externalUsers.apply(r);
      return null;
    }
    return switch (r.getRequestType()) {
      case CREATE_USER -> create(r, authority);
      case MODIFY_ROLES, MODIFY_USER, DISABLE_USER, ENABLE_USER -> change(r, authority);
      case CREATE_ROLE -> createRole(r, authority);
      case MODIFY_ROLE_PERMISSIONS -> changePermissions(r, authority);
      case DEACTIVATE_ROLE -> activation(r, false, authority);
      case REACTIVATE_ROLE -> activation(r, true, authority);
    };
  }

  private String create(AccessRequest r, ChangeAuthority authority) {
    String password = passwords.generate();
    RequestedUserData d = r.getUserData();
    userAdmin.createUser(
        new UserRequest(
            r.getUsername(),
            r.getFullName(),
            r.getEmail(),
            r.getHomeBranchId(),
            null,
            r.roles(),
            true,
            d.windowsId(),
            d.businessUnitCode(),
            d.userLevel()),
        password,
        authority);
    return password;
  }

  private String change(AccessRequest r, ChangeAuthority authority) {
    AppUser user = userAdmin.getByUsername(r.getUsername());
    AccessRequestType type = r.getRequestType();
    Set<String> requestedRoles = r.roles();
    Set<String> newRoles =
        requestedRoles.isEmpty() ? UserRequestValidator.roleCodes(user) : requestedRoles;
    boolean enabled =
        switch (type) {
          case DISABLE_USER -> false;
          case ENABLE_USER -> true;
          default -> user.isEnabled();
        };
    RequestedUserData d = r.getUserData();
    userAdmin.updateUser(
        user.getId(),
        new UserRequest(
            user.getUsername(),
            valueOr(r.getFullName(), user.getFullName()),
            valueOr(r.getEmail(), user.getEmail()),
            r.getHomeBranchId() == null ? user.getHomeBranchId() : r.getHomeBranchId(),
            user.getAuthorizationLimit(),
            newRoles,
            enabled,
            d.windowsId(),
            d.businessUnitCode(),
            d.userLevel()),
        authority);
    if (type == AccessRequestType.ENABLE_USER && (d.unlock() || user.isLocked())) {
      userAdmin.unlock(user.getId());
    }
    return null;
  }

  private String createRole(AccessRequest r, ChangeAuthority authority) {
    RolePermissionChange change = r.permissionChange();
    RequestedRole data = r.getRole();
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    change.added().forEach(p -> permissions.add(Permission.valueOf(p)));
    userAdmin.createRole(
        new RoleRequest(
            change.roleCode(), data.name(), permissions, data.description(), data.privilegeLevel()),
        authority);
    return null;
  }

  private String changePermissions(AccessRequest r, ChangeAuthority authority) {
    RolePermissionChange change = r.permissionChange();
    Role role = role(change.roleCode());
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    permissions.addAll(role.getPermissions());
    change.added().forEach(p -> permissions.add(Permission.valueOf(p)));
    change.removed().forEach(p -> permissions.remove(Permission.valueOf(p)));
    RequestedRole data = r.getRole();
    userAdmin.updateRole(
        role.getId(),
        new RoleRequest(
            role.getCode(),
            data == null ? role.getName() : valueOr(data.name(), role.getName()),
            permissions,
            data == null ? null : data.description(),
            data == null ? null : data.privilegeLevel()),
        authority);
    return null;
  }

  private String activation(AccessRequest r, boolean activate, ChangeAuthority authority) {
    Role role = role(r.getRoleCode());
    if (activate) {
      userAdmin.reactivateRole(role.getId(), authority);
    } else {
      userAdmin.deactivateRole(role.getId(), authority);
    }
    return null;
  }

  private Role role(String code) {
    return roles.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Role", code));
  }

  private static String valueOr(String value, String fallback) {
    return value == null ? fallback : value;
  }
}
