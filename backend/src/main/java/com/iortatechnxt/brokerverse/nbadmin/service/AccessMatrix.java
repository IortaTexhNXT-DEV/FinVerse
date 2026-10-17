package com.iortatechnxt.brokerverse.nbadmin.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User access matrix: roles as columns, permissions as rows, each permission with its functional
 * area and action classes (PMADD05).
 *
 * @param roles role columns
 * @param permissions permission rows
 */
public record AccessMatrix(List<RoleColumn> roles, List<PermissionRow> permissions) {

  /** Defensive copies. */
  public AccessMatrix {
    roles = List.copyOf(roles);
    permissions = List.copyOf(permissions);
  }

  /**
   * A role.
   *
   * @param code role code
   * @param name role name
   * @param enabledUsers enabled users holding the role
   */
  public record RoleColumn(String code, String name, int enabledUsers) {}

  /**
   * A permission and the roles granting it.
   *
   * @param permission permission code
   * @param roles role codes granting it
   * @param area functional area of the permission, null when not classified
   * @param actions action classes (VIEW, CREATE, AMEND, APPROVE), empty when not classified
   */
  public record PermissionRow(
      String permission, Set<String> roles, String area, List<String> actions) {

    /** Defensive copies. */
    public PermissionRow {
      roles = Set.copyOf(roles);
      actions = List.copyOf(actions);
    }
  }

  /**
   * The matrix by action (PMADD05): roles as columns, one row per functional area and action class,
   * each cell listing the permissions behind it that the role holds.
   *
   * @param roles role columns
   * @param rows area x action rows
   */
  public record ByAction(List<RoleColumn> roles, List<ActionRow> rows) {

    /** Defensive copies. */
    public ByAction {
      roles = List.copyOf(roles);
      rows = List.copyOf(rows);
    }
  }

  /**
   * One area and action class.
   *
   * @param area functional area
   * @param action VIEW, CREATE, AMEND or APPROVE
   * @param permissions permissions classified in this cell row
   * @param grants role code -> the permissions of this row the role holds (roles holding none are
   *     left out)
   */
  public record ActionRow(
      String area, String action, List<String> permissions, Map<String, List<String>> grants) {

    /** Defensive copies. */
    public ActionRow {
      permissions = List.copyOf(permissions);
      grants = Map.copyOf(grants);
    }
  }
}
