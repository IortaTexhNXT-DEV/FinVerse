package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * UA-02 Role matrix: one row per permission of a group profile. The profile is added or its name,
 * description and privilege level updated; KEEP and ADD give the permission to the profile, REMOVE
 * takes it away. The upload is approved by an access approver.
 */
@Component
public class RoleMatrixUpload extends ConfigUploadHandler {

  static final String PROFILE = "Group profile code";
  static final String NAME = "Name";
  static final String DESCRIPTION = "Description";
  static final String LEVEL = "Privilege level";
  static final String PERMISSION = "Permission";
  static final String ACTION_CLASS = "Action class";
  static final String CHANGE = "Keep / Add / Remove";
  static final String UNIT_HEAD = "Approved by unit head";

  private static final List<String> LEVELS = List.of("LOW", "STANDARD", "HIGH", "ADMIN");
  private static final List<String> CHANGES = List.of("KEEP", "ADD", "REMOVE");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_]{1,40}");
  private static final String REMOVE = "REMOVE";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public RoleMatrixUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_ROLE_MATRIX";
  }

  @Override
  public String templateId() {
    return "UA-02";
  }

  @Override
  public String title() {
    return "Role-to-permission matrix";
  }

  @Override
  public String screen() {
    return "System Administration > Roles";
  }

  @Override
  public String permission() {
    return "ROLE_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "ACCESS_APPROVE";
  }

  @Override
  public String filledBy() {
    return "Business Administration (User Access); unit heads";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
                PROFILE, "Code of the profile; an existing code changes the profile", "MKT_AO")
            .format("Up to 40 capital letters, digits or _"),
        BulkColumn.required(NAME, "Name shown on screens", "Marketing Account Officer"),
        BulkColumn.optional(DESCRIPTION, "What the profile is for", "Marketing account officers"),
        BulkColumn.required(LEVEL, "Low, Standard, High or Admin", "Standard")
            .values("Low", "Standard", "High", "Admin"),
        BulkColumn.required(PERMISSION, "One row per permission of the profile", "QUOTE_MAINTAIN")
            .allowed("Permission code of the User Access Matrix"),
        BulkColumn.optional(
            ACTION_CLASS, "As shown by the matrix, for the review", "Create, Amend"),
        BulkColumn.required(CHANGE, "Change against the matrix as delivered", "KEEP")
            .codes(CHANGES.toArray(String[]::new)),
        BulkColumn.required(UNIT_HEAD, "Unit head who confirms the row", "Carla Reyes"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PROFILE) + "|" + row.text(PERMISSION);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (!CODE_FORMAT.matcher(row.text(PROFILE)).matches()) {
      errors.add(error(PROFILE, "use up to 40 capital letters, digits or _"));
    }
    if (!LEVELS.contains(level(row))) {
      errors.add(error(LEVEL, "use Low, Standard, High or Admin"));
    }
    if (Arrays.stream(Permission.values()).noneMatch(p -> p.name().equals(row.text(PERMISSION)))) {
      errors.add(
          error(
              PERMISSION, row.text(PERMISSION) + " is not a permission of the User Access Matrix"));
    }
    oneOf(errors, CHANGE, row.text(CHANGE), CHANGES);
    if ("SYSADMIN".equals(row.text(PROFILE)) && REMOVE.equals(row.text(CHANGE))) {
      errors.add(
          error(
              CHANGE,
              "the permissions of the System Administrator profile are not removed by upload"));
    }
    return errors;
  }

  private static String level(BulkRow row) {
    return row.text(LEVEL).trim().toUpperCase(Locale.ROOT);
  }

  private static Map<String, Object> key(BulkRow row) {
    return columns("code", row.text(PROFILE));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action("sec_role", key(row));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    long roleId =
        db.upsert(
            "sec_role",
            key(row),
            columns(
                "name", row.text(NAME),
                "description", row.text(DESCRIPTION),
                "privilege_level", level(row)),
            context,
            "Group profile");
    if (REMOVE.equals(row.text(CHANGE))) {
      int removed =
          db.execute(
              "delete from sec_role_permission where role_id = ? and permission = ?",
              roleId,
              row.text(PERMISSION));
      return row.text(PROFILE)
          + (removed > 0 ? " without " : " already without ")
          + row.text(PERMISSION);
    }
    int added =
        db.execute(
            "insert into sec_role_permission (role_id, permission) select ?, ? where not exists"
                + " (select 1 from sec_role_permission where role_id = ? and permission = ?)",
            roleId,
            row.text(PERMISSION),
            roleId,
            row.text(PERMISSION));
    return row.text(PROFILE) + (added > 0 ? " with " : " already with ") + row.text(PERMISSION);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select r.code, r.name, r.description, initcap(r.privilege_level) as level, p.permission,"
                + " coalesce(u.full_name, r.updated_by, r.created_by) as confirmed_by from sec_role r"
                + " join sec_role_permission p on p.role_id = r.id"
                + " left join sec_user u on u.username = coalesce(r.updated_by, r.created_by)"
                + " where r.active order by r.code, p.permission")
        .stream()
        .map(
            r ->
                exportRow(
                    PROFILE, r.get("code"),
                    NAME, r.get("name"),
                    DESCRIPTION, r.get("description"),
                    LEVEL, r.get("level"),
                    PERMISSION, r.get("permission"),
                    CHANGE, "KEEP",
                    UNIT_HEAD, r.get("confirmed_by")))
        .toList();
  }
}
