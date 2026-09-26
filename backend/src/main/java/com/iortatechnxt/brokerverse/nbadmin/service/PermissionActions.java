package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The action classes of the permissions (PMADD05, table {@code sec_permission_action} seeded in
 * V755): each permission of the broking areas is VIEW, CREATE, AMEND or APPROVE within a functional
 * area, and may have two rows (CREATE and AMEND). Rows come back in area order (the order in which
 * the areas were seeded), then by action class and permission.
 */
@Component
public class PermissionActions {

  private static final String SQL =
      """
      select permission, area, action from sec_permission_action
      where (cast(? as varchar) is null or area = ?)
      order by min(id) over (partition by area),
               case action when 'VIEW' then 1 when 'CREATE' then 2 when 'AMEND' then 3 else 4 end,
               permission
      """;

  private final JdbcTemplate jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc JDBC access
   */
  public PermissionActions(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The classified permissions.
   *
   * @param area functional area, null for all
   * @return action classes in display order
   */
  public List<PermissionAction> list(String area) {
    return jdbc
        .query(
            SQL,
            (rs, i) ->
                new PermissionAction(
                    rs.getString("permission"), rs.getString("area"), rs.getString("action")),
            area,
            area)
        .stream()
        .filter(a -> Permission.isOffered(a.permission()))
        .toList();
  }

  /**
   * One action class of a permission.
   *
   * @param permission permission code
   * @param area functional area (e.g. PRODUCT_MAINTENANCE, PACKAGE_REQUEST, CLIENTS)
   * @param action VIEW, CREATE, AMEND or APPROVE
   */
  public record PermissionAction(String permission, String area, String action) {}
}
