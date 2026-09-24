package com.iortatechnxt.brokerverse.nbadmin.service;

import java.util.List;
import java.util.Set;

/**
 * User access matrix: roles as columns, permissions as rows.
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
   */
  public record PermissionRow(String permission, Set<String> roles) {

    /** Defensive copy. */
    public PermissionRow {
      roles = Set.copyOf(roles);
    }
  }
}
