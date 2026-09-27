package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import java.util.ArrayList;
import java.util.List;

/** One-line descriptions of access requests (work lists, notifications, audit trail). */
final class AccessRequestDescriptions {

  private AccessRequestDescriptions() {}

  /**
   * One-line description of a request.
   *
   * @param r request
   * @return description
   */
  static String describe(AccessRequest r) {
    AccessRequestType type = r.getRequestType();
    if (type.isGroupProfile()) {
      return describeProfile(r);
    }
    String who = r.getUsername();
    return switch (type) {
      case CREATE_USER ->
          "Create " + (r.getExternal() == null ? "user " : "external user ") + who + roles(r);
      case MODIFY_ROLES -> "Change roles of " + who + " to " + r.roles();
      case MODIFY_USER -> "Modify user " + who + (r.roles().isEmpty() ? "" : roles(r));
      case DISABLE_USER -> "Disable user " + who;
      default -> "Enable user " + who;
    };
  }

  private static String roles(AccessRequest r) {
    return r.roles().isEmpty() ? "" : " with roles " + r.roles();
  }

  private static String describeProfile(AccessRequest r) {
    return switch (r.getRequestType()) {
      case CREATE_ROLE ->
          "Create group profile " + r.getRoleCode() + " with " + r.permissionChange().added();
      case DEACTIVATE_ROLE -> "Deactivate group profile " + r.getRoleCode();
      case REACTIVATE_ROLE -> "Reactivate group profile " + r.getRoleCode();
      default -> describePermissions(r.permissionChange());
    };
  }

  private static String describePermissions(RolePermissionChange c) {
    List<String> parts = new ArrayList<>();
    if (!c.added().isEmpty()) {
      parts.add("add " + c.added());
    }
    if (!c.removed().isEmpty()) {
      parts.add("remove " + c.removed());
    }
    if (parts.isEmpty()) {
      parts.add("name, description or level");
    }
    return "Change permissions of role " + c.roleCode() + ": " + String.join("; ", parts);
  }
}
