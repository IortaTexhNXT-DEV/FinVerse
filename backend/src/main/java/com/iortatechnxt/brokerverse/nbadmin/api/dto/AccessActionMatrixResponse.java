package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import java.util.List;
import java.util.Map;

/**
 * Role-to-action matrix (PMADD05): roles as columns, one row per functional area and action class.
 *
 * @param roles roles with their enabled users
 * @param rows area x action rows
 */
public record AccessActionMatrixResponse(List<AccessMatrixResponse.Role> roles, List<Row> rows) {

  /**
   * Maps the matrix.
   *
   * @param m matrix by action
   * @return response
   */
  public static AccessActionMatrixResponse from(AccessMatrix.ByAction m) {
    return new AccessActionMatrixResponse(
        m.roles().stream()
            .map(r -> new AccessMatrixResponse.Role(r.code(), r.name(), r.enabledUsers()))
            .toList(),
        m.rows().stream()
            .map(r -> new Row(r.area(), r.action(), r.permissions(), r.grants()))
            .toList());
  }

  /**
   * One area and action class.
   *
   * @param area functional area
   * @param action VIEW, CREATE, AMEND or APPROVE
   * @param permissions permissions classified in the row
   * @param grants role code -> permissions of the row the role holds (roles holding none are
   *     omitted)
   */
  public record Row(
      String area, String action, List<String> permissions, Map<String, List<String>> grants) {}
}
