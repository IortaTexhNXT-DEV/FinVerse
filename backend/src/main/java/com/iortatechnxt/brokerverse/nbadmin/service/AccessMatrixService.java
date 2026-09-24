package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The agreed User Access Matrix (BRD 3.3.4): roles against permissions as granted today, with the
 * number of enabled users per role. Read-only; exportable to Excel for sign-off.
 */
@Service
@Transactional(readOnly = true)
public class AccessMatrixService {

  private final UserAdminService userAdmin;
  private final DocumentComposer composer;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param userAdmin users and roles
   * @param composer spreadsheet composer
   * @param audit audit trail
   */
  public AccessMatrixService(
      UserAdminService userAdmin, DocumentComposer composer, AuditTrailService audit) {
    this.userAdmin = userAdmin;
    this.composer = composer;
    this.audit = audit;
  }

  /**
   * The matrix.
   *
   * @return roles, permissions and grants
   */
  public AccessMatrix matrix() {
    List<Role> roles = userAdmin.listRoles();
    Map<String, Integer> users = new HashMap<>();
    for (AppUser u : userAdmin.listUsers()) {
      if (u.isEnabled()) {
        u.getRoles().forEach(r -> users.merge(r.getCode(), 1, Integer::sum));
      }
    }
    List<AccessMatrix.RoleColumn> columns =
        roles.stream()
            .map(
                r ->
                    new AccessMatrix.RoleColumn(
                        r.getCode(), r.getName(), users.getOrDefault(r.getCode(), 0)))
            .toList();
    List<AccessMatrix.PermissionRow> rows = new ArrayList<>();
    for (Permission p : Permission.values()) {
      Set<String> granted = new TreeSet<>();
      roles.stream()
          .filter(r -> r.getPermissions().contains(p))
          .forEach(r -> granted.add(r.getCode()));
      rows.add(new AccessMatrix.PermissionRow(p.name(), granted));
    }
    return new AccessMatrix(columns, rows);
  }

  /**
   * The matrix as an Excel workbook (Y where granted); the export is audited.
   *
   * @return XLSX bytes
   */
  @Transactional
  public byte[] exportXlsx() {
    AccessMatrix m = matrix();
    List<String> headers = new ArrayList<>();
    headers.add("Permission");
    m.roles().forEach(r -> headers.add(r.code()));
    List<List<Object>> rows = new ArrayList<>();
    List<Object> users = new ArrayList<>();
    users.add("Enabled users");
    m.roles().forEach(r -> users.add(r.enabledUsers()));
    rows.add(users);
    for (AccessMatrix.PermissionRow row : m.permissions()) {
      List<Object> line = new ArrayList<>();
      line.add(row.permission());
      m.roles().forEach(r -> line.add(row.roles().contains(r.code()) ? "Y" : ""));
      rows.add(line);
    }
    audit.record(
        "AccessMatrix",
        "ALL",
        AuditAction.EXPORT,
        "User access matrix exported: "
            + m.roles().size()
            + " roles, "
            + m.permissions().size()
            + " permissions");
    return composer.xlsx(new SheetSpec("User Access Matrix", headers, rows));
  }
}
