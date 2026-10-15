package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
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
 * (PMADD05) adds and removes the requested permissions on the role as it is when applied. Users are
 * maintained in UIDM-ISC, so only group-profile changes are applied here. A permission the request
 * names that the system no longer has (withdrawn after the request was raised) is skipped and the
 * skip is written to the audit trail of the request; the rest of the change applies.
 */
@Component
public class AccessChangeApplier {

  private final UserAdminService userAdmin;
  private final RoleRepository roles;
  private final AuditTrailService audit;

  /**
   * Creates the applier.
   *
   * @param userAdmin user administration
   * @param roles roles
   * @param audit audit trail (permissions skipped)
   */
  public AccessChangeApplier(
      UserAdminService userAdmin, RoleRepository roles, AuditTrailService audit) {
    this.audit = audit;
    this.userAdmin = userAdmin;
    this.roles = roles;
  }

  /**
   * Applies a group-profile change on the authority of its (last) approver.
   *
   * @param r approved request
   * @throws BusinessRuleException ACCESS_USERS_BY_UIDM for a request about a user
   */
  public void apply(AccessRequest r) {
    if (!r.getRequestType().isGroupProfile()) {
      throw new BusinessRuleException(
          AccessRequestPermissions.USERS_BY_UIDM,
          "Users are maintained in UIDM-ISC; request " + r.getRequestNo() + " is not applied");
    }
    ChangeAuthority authority = ChangeAuthority.request(r.getRequestNo(), r.getDecidedBy());
    switch (r.getRequestType()) {
      case CREATE_ROLE -> createRole(r, authority);
      case DEACTIVATE_ROLE -> activation(r, false, authority);
      case REACTIVATE_ROLE -> activation(r, true, authority);
      default -> changePermissions(r, authority);
    }
  }

  private void createRole(AccessRequest r, ChangeAuthority authority) {
    RolePermissionChange change = r.permissionChange();
    RequestedRole data = r.getRole();
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    permissions.addAll(known(r, change.added()));
    userAdmin.createRole(
        new RoleRequest(
            change.roleCode(), data.name(), permissions, data.description(), data.privilegeLevel()),
        authority);
  }

  private void changePermissions(AccessRequest r, ChangeAuthority authority) {
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
  }

  private void activation(AccessRequest r, boolean activate, ChangeAuthority authority) {
    Role role = role(r.getRoleCode());
    if (activate) {
      userAdmin.reactivateRole(role.getId(), authority);
    } else {
      userAdmin.deactivateRole(role.getId(), authority);
    }
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
