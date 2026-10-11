package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixWorkbook.Grid;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixWorkbook.Header;
import com.iortatechnxt.brokerverse.nbadmin.service.PermissionActions.PermissionAction;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import java.time.Clock;
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

  private static final String TITLE = "User Access Matrix";

  private final UserAdminService userAdmin;
  private final PermissionActions actions;
  private final AuditTrailService audit;
  private final ProductModules modules;
  private final Printing printing;

  /**
   * Who and what the header of the exported workbook names.
   *
   * @param companies companies (the operating company in the header)
   * @param currentUser the user exporting
   * @param users display names
   * @param clock clock (run date)
   */
  public record Printing(
      CompanyRepository companies, CurrentUser currentUser, UserDirectory users, Clock clock) {}

  /**
   * Creates the service.
   *
   * @param userAdmin users and roles
   * @param actions permission action classes
   * @param audit audit trail
   * @param modules product module switches (permissions of switched-off modules are not shown)
   * @param companies companies (the operating company in the header of the export)
   * @param currentUser the user exporting
   * @param users display names
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public AccessMatrixService(
      UserAdminService userAdmin,
      PermissionActions actions,
      AuditTrailService audit,
      ProductModules modules,
      CompanyRepository companies,
      CurrentUser currentUser,
      UserDirectory users,
      Clock clock) {
    this.modules = modules;
    this.userAdmin = userAdmin;
    this.actions = actions;
    this.audit = audit;
    this.printing = new Printing(companies, currentUser, users, clock);
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
    for (Permission p : Permission.values()) {
      if (!modules.isPermissionActive(p.name())) {
        continue;
      }
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
   * The matrix as an Excel workbook in the report layout: a sheet by permission (Y where granted,
   * with the area and action classes, the enabled users of each profile in the first row) and a
   * sheet by action (the permissions each role holds per area and action). The export is audited.
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
    return AccessMatrixWorkbook.write(List.of(permissionSheet(m), actionSheet(byAction)));
  }

  private Header header(String legend) {
    String company =
        printing
            .companies()
            .findFirstByOrderByIdAsc()
            .map(Company::getName)
            .orElse(BrandAssets.SYSTEM_NAME);
    String user = printing.currentUser().username();
    String name = printing.users().displayName(user);
    return new Header(
        company,
        TITLE,
        "Run By: "
            + (name == null ? user : name)
            + "   Run Date: "
            + DisplayFormat.dateTime(printing.clock().instant()),
        legend);
  }

  private Grid permissionSheet(AccessMatrix m) {
    List<String> profiles = m.roles().stream().map(AccessMatrix.RoleColumn::code).toList();
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
    String legend =
        m.permissions().size()
            + " permissions x "
            + profiles.size()
            + " group profiles. Y: the group profile holds the permission. First row: the enabled"
            + " users of each group profile.";
    return new Grid(
        TITLE,
        header(legend),
        List.of("Permission", "Area", "Action class"),
        profiles,
        rows,
        false);
  }

  private Grid actionSheet(AccessMatrix.ByAction m) {
    List<String> profiles = m.roles().stream().map(AccessMatrix.RoleColumn::code).toList();
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
    String legend =
        m.rows().size()
            + " area and action class rows x "
            + profiles.size()
            + " group profiles. Each cell lists the permissions of the row the group profile holds.";
    return new Grid(
        "By Action",
        header(legend),
        List.of("Area", "Action class", "Permissions"),
        profiles,
        rows,
        true);
  }
}
