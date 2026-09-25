package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Applies an approved access request through the security administration service, so the same rules
 * and audit entries apply as for a change made on the Users or Roles screen. A role-permission
 * change (PMADD05) adds and removes the requested permissions on the role as it is at approval.
 */
@Component
public class AccessChangeApplier {

  private final UserAdminService userAdmin;
  private final RoleRepository roles;
  private final TemporaryPasswords passwords;

  /**
   * Creates the applier.
   *
   * @param userAdmin user administration
   * @param roles roles
   * @param passwords temporary password generator
   */
  public AccessChangeApplier(
      UserAdminService userAdmin, RoleRepository roles, TemporaryPasswords passwords) {
    this.userAdmin = userAdmin;
    this.roles = roles;
    this.passwords = passwords;
  }

  /**
   * Applies the request.
   *
   * @param r approved request
   * @return temporary password of a created user, null otherwise
   */
  public String apply(AccessRequest r) {
    return switch (r.getRequestType()) {
      case CREATE_USER -> create(r);
      case MODIFY_ROLES -> change(r.getUsername(), r.roles(), null);
      case DISABLE_USER -> change(r.getUsername(), null, false);
      case ENABLE_USER -> change(r.getUsername(), null, true);
      case MODIFY_ROLE_PERMISSIONS -> changePermissions(r.permissionChange());
    };
  }

  private String changePermissions(RolePermissionChange change) {
    Role role =
        roles
            .findByCode(change.roleCode())
            .orElseThrow(() -> new ResourceNotFoundException("Role", change.roleCode()));
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    permissions.addAll(role.getPermissions());
    change.added().forEach(p -> permissions.add(Permission.valueOf(p)));
    change.removed().forEach(p -> permissions.remove(Permission.valueOf(p)));
    userAdmin.updateRole(
        role.getId(), new RoleRequest(role.getCode(), role.getName(), permissions));
    return null;
  }

  private String create(AccessRequest r) {
    String password = passwords.generate();
    userAdmin.createUser(
        new UserRequest(
            r.getUsername(),
            r.getFullName(),
            r.getEmail(),
            r.getHomeBranchId(),
            null,
            r.roles(),
            true),
        password);
    return password;
  }

  private String change(String username, Set<String> roles, Boolean enabled) {
    AppUser user = userAdmin.getByUsername(username);
    Set<String> current = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    userAdmin.updateUser(
        user.getId(),
        new UserRequest(
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getHomeBranchId(),
            user.getAuthorizationLimit(),
            roles == null ? current : roles,
            enabled == null ? user.isEnabled() : enabled));
    return null;
  }
}
