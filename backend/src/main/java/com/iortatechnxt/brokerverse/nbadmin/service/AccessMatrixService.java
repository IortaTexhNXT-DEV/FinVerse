package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.nbadmin.service.PermissionActions.PermissionAction;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The agreed User Access Matrix (BRD 3.3.4): roles against permissions as granted today, with the
 * number of enabled users per role. The role-to-action view (PMADD05) groups the permissions by
 * functional area and action class (VIEW / CREATE / AMEND / APPROVE, {@link PermissionActions}).
 * Read-only; exportable to Excel (both views) for sign-off.
 */
@Service
@Transactional(readOnly = true)
public class AccessMatrixService {

  private final UserAdminService userAdmin;
  private final PermissionActions actions;
  private final DocumentComposer composer;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param userAdmin users and roles
   * @param actions permission action classes
   * @param composer spreadsheet composer
   * @param audit audit trail
   */
  public AccessMatrixService(
      UserAdminService userAdmin,
      PermissionActions actions,
      DocumentComposer composer,
      AuditTrailService audit) {
    this.userAdmin = userAdmin;
    this.actions = actions;
    this.composer = composer;
    this.audit = audit;
  }

  /**
   * The matrix by permission, each permission with its area and action classes.
   *
   * @return roles, permissions and grants
   */
  public AccessMatrix matrix() {
    List<Role> roles = userAdmin.listRoles();
    Map<String, List<PermissionAction>> classes =
        actions.list(null).stream()
            .collect(Collectors.groupingBy(PermissionAction::permission, Collectors.toList()));
    List<AccessMatrix.PermissionRow> rows = new ArrayList<>();
    for (Permission p : Permission.offered()) {
      Set<String> granted = new TreeSet<>();
      roles.stream()
          .filter(r -> r.getPermissions().contains(p))
          .forEach(r -> granted.add(r.getCode()));
      List<PermissionAction> own = classes.getOrDefault(p.name(), List.of());
      rows.add(
          new AccessMatrix.PermissionRow(
              p.name(),
              granted,
              own.isEmpty() ? null : own.get(0).area(),
              own.stream().map(PermissionAction::action).toList()));
    }
    return new AccessMatrix(columns(roles), rows);
  }

  /**
   * The matrix by action (PMADD05): one row per area and action class with, for each role, the
   * permissions behind the cell that the role holds.
   *
   * @param area functional area, null for all
   * @return roles and area x action rows
   */
  public AccessMatrix.ByAction byAction(String area) {
    List<Role> roles = userAdmin.listRoles();
    Map<AreaAction, List<String>> rows = new LinkedHashMap<>();
    for (PermissionAction a : actions.list(area)) {
      rows.computeIfAbsent(new AreaAction(a.area(), a.action()), k -> new ArrayList<>())
          .add(a.permission());
    }
    List<AccessMatrix.ActionRow> result = new ArrayList<>();
    rows.forEach(
        (key, permissions) ->
            result.add(
                new AccessMatrix.ActionRow(
                    key.area(), key.action(), permissions, grants(roles, permissions))));
    return new AccessMatrix.ByAction(columns(roles), result);
  }

  private record AreaAction(String area, String action) {}

  private static Map<String, List<String>> grants(List<Role> roles, List<String> permissions) {
    Map<String, List<String>> grants = new TreeMap<>();
    for (Role r : roles) {
      Set<String> held = r.getPermissions().stream().map(Enum::name).collect(Collectors.toSet());
      List<String> cell = permissions.stream().filter(held::contains).toList();
      if (!cell.isEmpty()) {
        grants.put(r.getCode(), cell);
      }
    }
    return grants;
  }

  private List<AccessMatrix.RoleColumn> columns(List<Role> roles) {
    Map<String, Integer> users = new HashMap<>();
    for (AppUser u : userAdmin.listUsers()) {
      if (u.isEnabled()) {
        u.getRoles().forEach(r -> users.merge(r.getCode(), 1, Integer::sum));
      }
    }
    return roles.stream()
        .map(
            r ->
                new AccessMatrix.RoleColumn(
                    r.getCode(), r.getName(), users.getOrDefault(r.getCode(), 0)))
        .toList();
  }

  /**
   * The matrix as an Excel workbook: a sheet by permission (Y where granted, with the area and
   * action classes) and a sheet by action (the permissions each role holds per area and action).
   * The export is audited.
   *
   * @return XLSX bytes
   */
  @Transactional
  public byte[] exportXlsx() {
    AccessMatrix m = matrix();
    AccessMatrix.ByAction byAction = byAction(null);
    audit.record(
        "AccessMatrix",
        "ALL",
        AuditAction.EXPORT,
        "User access matrix exported: "
            + m.roles().size()
            + " roles, "
            + m.permissions().size()
            + " permissions, "
            + byAction.rows().size()
            + " area / action rows");
    return composer.xlsx(List.of(permissionSheet(m), actionSheet(byAction)));
  }

  private static SheetSpec permissionSheet(AccessMatrix m) {
    List<String> headers = new ArrayList<>(List.of("Permission", "Area", "Action class"));
    m.roles().forEach(r -> headers.add(r.code()));
    List<List<Object>> rows = new ArrayList<>();
    List<Object> users = new ArrayList<>(List.of("Enabled users", "", ""));
    m.roles().forEach(r -> users.add(r.enabledUsers()));
    rows.add(users);
    for (AccessMatrix.PermissionRow row : m.permissions()) {
      List<Object> line = new ArrayList<>();
      line.add(row.permission());
      line.add(row.area() == null ? "" : row.area());
      line.add(String.join(", ", row.actions()));
      m.roles().forEach(r -> line.add(row.roles().contains(r.code()) ? "Y" : ""));
      rows.add(line);
    }
    return new SheetSpec("User Access Matrix", headers, rows);
  }

  private static SheetSpec actionSheet(AccessMatrix.ByAction m) {
    List<String> headers = new ArrayList<>(List.of("Area", "Action class", "Permissions"));
    m.roles().forEach(r -> headers.add(r.code()));
    List<List<Object>> rows = new ArrayList<>();
    for (AccessMatrix.ActionRow row : m.rows()) {
      List<Object> line = new ArrayList<>();
      line.add(row.area());
      line.add(row.action());
      line.add(String.join(", ", row.permissions()));
      m.roles()
          .forEach(
              r -> line.add(String.join(", ", row.grants().getOrDefault(r.code(), List.of()))));
      rows.add(line);
    }
    return new SheetSpec("By Action", headers, rows);
  }
}
