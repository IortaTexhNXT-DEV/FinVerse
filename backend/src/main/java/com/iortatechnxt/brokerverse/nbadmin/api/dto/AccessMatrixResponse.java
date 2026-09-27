package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import java.util.List;
import java.util.TreeSet;

/**
 * User access matrix: roles as columns, permissions as rows with their action classes (PMADD05).
 *
 * @param roles roles with their enabled users
 * @param permissions permissions with the roles granting them
 */
public record AccessMatrixResponse(List<Role> roles, List<Row> permissions) {

  /**
   * Maps the matrix.
   *
   * @param m matrix
   * @return response
   */
  public static AccessMatrixResponse from(AccessMatrix m) {
    return new AccessMatrixResponse(
        m.roles().stream().map(r -> new Role(r.code(), r.name(), r.enabledUsers())).toList(),
        m.permissions().stream()
            .map(
                p ->
                    new Row(
                        p.permission(),
                        List.copyOf(new TreeSet<>(p.roles())),
                        p.area(),
                        p.actions()))
            .toList());
  }

  /**
   * A role column.
   *
   * @param code code
   * @param name name
   * @param enabledUsers enabled users holding it
   */
  public record Role(String code, String name, int enabledUsers) {}

  /**
   * A permission row.
   *
   * @param permission permission code
   * @param roles role codes granting it
   * @param area functional area (PMADD05), null when not classified
   * @param actions action classes VIEW / CREATE / AMEND / APPROVE, empty when not classified
   */
  public record Row(String permission, List<String> roles, String area, List<String> actions) {}
}
