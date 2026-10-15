package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
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
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Applies an approved access request through the security administration service, so the same
 * rules, audit entries and access change log rows (with the request number and approver, BRD
 * 4.003.1) apply as for a change made on the Users or Roles screen. A role-permission change
 * (PMADD05) adds and removes the requested permissions on the role as it is when applied. External
 * (portal) users are provisioned through {@link ExternalUserProvisioner} (decision D7). A requested
 * data scope (V1240) is applied with the user data, on the same authority. A permission the request
 * names that the system no longer has (withdrawn after the request was raised) is skipped and the
 * skip is written to the audit trail of the request; the rest of the change applies.
 */
@Component
public class AccessChangeApplier {

  private final UserAdminService userAdmin;
  private final RoleRepository roles;
  private final TemporaryPasswords passwords;
  private final ExternalUserApplier externalUsers;
  private final DataScopeService dataScopes;
  private final AuditTrailService audit;

  /**
   * Creates the applier.
   *
   * @param userAdmin user administration
   * @param roles roles
   * @param passwords temporary password generator
   * @param externalUsers external (portal) users
   * @param dataScopes data scope administration
   * @param audit audit trail (permissions skipped)
   */
  public AccessChangeApplier(
      UserAdminService userAdmin,
      RoleRepository roles,
      TemporaryPasswords passwords,
      ExternalUserApplier externalUsers,
      DataScopeService dataScopes,
      AuditTrailService audit) {
    this.audit = audit;
    this.userAdmin = userAdmin;
    this.roles = roles;
    this.passwords = passwords;
    this.externalUsers = externalUsers;
    this.dataScopes = dataScopes;
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
    if (r.getRequestType().isGroupProfile()) {
      return applyGroupProfile(r, authority);
    }
    return r.getRequestType() == AccessRequestType.CREATE_USER
        ? create(r, authority)
        : change(r, authority);
  }

  private String applyGroupProfile(AccessRequest r, ChangeAuthority authority) {
    return switch (r.getRequestType()) {
      case CREATE_ROLE -> createRole(r, authority);
      case DEACTIVATE_ROLE -> activation(r, false, authority);
      case REACTIVATE_ROLE -> activation(r, true, authority);
      default -> changePermissions(r, authority);
    };
  }

  private String create(AccessRequest r, ChangeAuthority authority) {
    String password = passwords.generate();
    RequestedUserData d = r.getUserData();
    AppUser created =
        userAdmin.createUser(
            new UserRequest(
                r.getUsername(),
                r.getFullName(),
                r.getEmail(),
                r.getHomeBranchId(),
                d.authorizationLimit(),
                r.roles(),
                true,
                d.windowsId(),
                d.businessUnitCode(),
                d.userLevel()),
            password,
            authority);
    d.requestedScope().ifPresent(scope -> dataScopes.replace(created, scope, authority));
    return password;
  }

  private String change(AccessRequest r, ChangeAuthority authority) {
    AppUser user = userAdmin.getByUsername(r.getUsername());
    AccessRequestType type = r.getRequestType();
    Set<String> requestedRoles = r.roles();
    Set<String> newRoles =
        requestedRoles.isEmpty() ? UserStateRules.roleCodes(user) : requestedRoles;
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
            d.authorizationLimit() == null ? user.getAuthorizationLimit() : d.authorizationLimit(),
            newRoles,
            enabled,
            d.windowsId(),
            d.businessUnitCode(),
            d.userLevel()),
        authority);
    d.requestedScope().ifPresent(scope -> dataScopes.replace(user, scope, authority));
    if (type == AccessRequestType.ENABLE_USER && (d.unlock() || user.isLocked())) {
      userAdmin.unlock(user.getId());
    }
    return null;
  }

  private String createRole(AccessRequest r, ChangeAuthority authority) {
    RolePermissionChange change = r.permissionChange();
    RequestedRole data = r.getRole();
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    permissions.addAll(known(r, change.added()));
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
    permissions.addAll(known(r, change.added()));
    permissions.removeAll(known(r, change.removed()));
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

  /**
   * The permissions of a request that the system still has; the others are skipped with a note in
   * the audit trail of the request.
   */
  private Set<Permission> known(AccessRequest r, Set<String> codes) {
    Set<Permission> found = EnumSet.noneOf(Permission.class);
    Set<String> withdrawn = new TreeSet<>();
    for (String code : codes) {
      Arrays.stream(Permission.values())
          .filter(p -> p.name().equals(code))
          .findFirst()
          .ifPresentOrElse(found::add, () -> withdrawn.add(code));
    }
    if (!withdrawn.isEmpty()) {
      audit.record(
          AccessRequestService.ENTITY,
          r.getRequestNo(),
          AuditAction.UPDATE,
          "Permissions no longer in the system skipped: " + String.join(", ", withdrawn));
    }
    return found;
  }

  private Role role(String code) {
    return roles.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Role", code));
  }

  private static String valueOr(String value, String fallback) {
    return value == null ? fallback : value;
  }
}
