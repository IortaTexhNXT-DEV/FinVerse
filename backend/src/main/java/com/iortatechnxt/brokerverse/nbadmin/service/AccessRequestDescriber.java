package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-line descriptions of access requests for users (work lists, notifications, My Approvals, the
 * audit trail): the group profiles and permissions by their names, never their codes.
 */
@Component
@Transactional(readOnly = true)
public class AccessRequestDescriber {

  private final RoleRepository roles;

  /**
   * Creates the describer.
   *
   * @param roles group profiles (their names)
   */
  public AccessRequestDescriber(RoleRepository roles) {
    this.roles = roles;
  }

  /**
   * One-line description of a request.
   *
   * @param r request
   * @return description
   */
  public String describe(AccessRequest r) {
    AccessRequestType type = r.getRequestType();
    if (type.isGroupProfile()) {
      return describeProfile(r);
    }
    String who = r.getUsername();
    return switch (type) {
      case CREATE_USER ->
          "Create " + (r.getExternal() == null ? "user " : "external user ") + who + profiles(r);
      case MODIFY_ROLES -> "Change the group profiles of " + who + " to " + profileNames(r.roles());
      case MODIFY_USER -> "Modify user " + who + (r.roles().isEmpty() ? "" : profiles(r));
      case DISABLE_USER -> "Deactivate user " + who;
      default -> "Reactivate user " + who;
    };
  }

  /**
   * The names of group profiles, separated by commas (the code when a profile is unknown).
   *
   * @param codes profile codes
   * @return names
   */
  public String profileNames(Collection<String> codes) {
    if (codes.isEmpty()) {
      return "";
    }
    Map<String, String> names =
        roles.findByCodeIn(codes).stream().collect(Collectors.toMap(Role::getCode, Role::getName));
    return codes.stream()
        .sorted()
        .map(c -> names.getOrDefault(c, c))
        .collect(Collectors.joining(", "));
  }

  /**
   * The name of a group profile (the code when unknown).
   *
   * @param code profile code
   * @return name
   */
  public String profileName(String code) {
    return code == null ? "" : roles.findByCode(code).map(Role::getName).orElse(code);
  }

  private String profiles(AccessRequest r) {
    return r.roles().isEmpty() ? "" : " with group profiles " + profileNames(r.roles());
  }

  private String describeProfile(AccessRequest r) {
    RolePermissionChange change = r.permissionChange();
    return switch (r.getRequestType()) {
      case CREATE_ROLE ->
          "Create group profile "
              + newProfileName(r)
              + " with "
              + PermissionNames.names(change.added());
      case DEACTIVATE_ROLE -> "Deactivate group profile " + profileName(r.getRoleCode());
      case REACTIVATE_ROLE -> "Reactivate group profile " + profileName(r.getRoleCode());
      default -> describePermissions(change);
    };
  }

  private static String newProfileName(AccessRequest r) {
    RequestedRole role = r.getRole();
    return role == null || role.name() == null ? r.getRoleCode() : role.name();
  }

  private String describePermissions(RolePermissionChange c) {
    List<String> parts = new ArrayList<>();
    if (!c.added().isEmpty()) {
      parts.add("add " + PermissionNames.names(c.added()));
    }
    if (!c.removed().isEmpty()) {
      parts.add("remove " + PermissionNames.names(c.removed()));
    }
    if (parts.isEmpty()) {
      parts.add("name, description or level");
    }
    return "Change the permissions of group profile "
        + profileName(c.roleCode())
        + ": "
        + String.join("; ", parts);
  }
}
