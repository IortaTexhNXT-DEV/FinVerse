package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code UAM-GROUP-PROFILE} User Group Profile Report (BRD 3.003.2, sample B; FR-UA-061): for each
 * group profile, the modules (permission areas) it can access and every task (permission and action
 * class) of those modules marked With Access or No Access, with the profile's created and last
 * modified dates and actors. A module filter lists that module even where the profile has no
 * access. Inactive profiles are listed only when the Active filter asks for them.
 */
@Component
public class GroupProfileReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "UAM-GROUP-PROFILE";

  private static final String AREA = "area";
  private static final String ACTIVE = "active";
  private static final String ACTIVE_ONLY = "ACTIVE";
  private static final String INACTIVE_ONLY = "INACTIVE";
  private static final String OTHER = "OTHER";
  private static final String PROFILE = "profile";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC
   */
  public GroupProfileReport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return UamReportSupport.metadata(
        CODE,
        "User Group Profile Report",
        "Modules and tasks each group profile can access, with its created and modified dates"
            + " (BRD 3.003.2)",
        List.of(
            UamReportSupport.groupProfileParam(),
            ParameterSpec.optional(AREA, "Module (area code)", ParameterType.TEXT),
            ParameterSpec.select(
                ACTIVE,
                "Active",
                List.of(ACTIVE_ONLY, INACTIVE_ONLY, UamReportSupport.ALL),
                ACTIVE_ONLY)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String profile = p.optionalText(UamReportSupport.GROUP_PROFILE).orElse(null);
    String area = p.optionalText(AREA).orElse(null);
    String active = p.text(ACTIVE);
    Map<String, String> areas = areas();
    Map<Long, Set<String>> granted = grants();
    Map<String, String> actions = actions();
    Map<String, RoleChange> lastChange = lastChanges();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> role :
        jdbc.queryForList(
            "select id, code, name, active, created_at, created_by, updated_at, updated_by"
                + " from sec_role order by name",
            Map.of())) {
      String code = UamReportSupport.text(role, "code");
      boolean isActive = Boolean.TRUE.equals(role.get(ACTIVE));
      if (UamReportSupport.matches(profile, code) && wanted(active, isActive)) {
        Set<String> perms = granted.getOrDefault(((Number) role.get("id")).longValue(), Set.of());
        RoleChange change = lastChange.get(code);
        tasks(perms, areas, area)
            .forEach(pm -> rows.add(row(role, isActive, pm, perms, areas, actions, change)));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("module", "Module Name"),
            ReportColumn.text("task", "Task Name"),
            ReportColumn.text("access", "Access"),
            ReportColumn.date("createdAt", "Date Created"),
            ReportColumn.text("createdBy", "Created by"),
            ReportColumn.date("modifiedAt", "Date Modified"),
            ReportColumn.text("modifiedBy", "Modified by"))
        .groupBy(PROFILE, "Group Profile")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Module = permission area; task = permission and action class. Only the modules in"
                + " which the profile has at least one task are listed, unless a module is chosen.")
        .build();
  }

  private static boolean wanted(String filter, boolean active) {
    return switch (filter) {
      case ACTIVE_ONLY -> active;
      case INACTIVE_ONLY -> !active;
      default -> true;
    };
  }

  /** Permissions of the modules shown for one profile, ordered by module and permission. */
  private static List<String> tasks(Set<String> perms, Map<String, String> areas, String area) {
    Set<String> modules = new HashSet<>();
    if (area == null || area.isBlank()) {
      perms.forEach(pm -> modules.add(areas.getOrDefault(pm, OTHER)));
    } else {
      modules.add(area.trim().toUpperCase(Locale.ROOT));
    }
    return Permission.offered().stream()
        .map(Permission::name)
        .filter(pm -> modules.contains(areas.getOrDefault(pm, OTHER)))
        .sorted(
            Comparator.comparing((String pm) -> areas.getOrDefault(pm, OTHER))
                .thenComparing(pm -> pm))
        .toList();
  }

  @SuppressWarnings("java:S107") // one row of the report joins the role, the task and the log
  private static Map<String, Object> row(
      Map<String, Object> role,
      boolean active,
      String permission,
      Set<String> perms,
      Map<String, String> areas,
      Map<String, String> actions,
      RoleChange change) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(
        PROFILE,
        UamReportSupport.text(role, "name")
            + " ("
            + UamReportSupport.text(role, "code")
            + (active ? ")" : ", inactive)"));
    m.put("module", UamReportSupport.words(areas.getOrDefault(permission, OTHER)));
    String action = actions.get(permission);
    m.put("task", action == null ? permission : permission + " (" + action + ")");
    m.put("access", perms.contains(permission) ? "With Access" : "No Access");
    m.put("createdAt", UamReportSupport.date(UamReportSupport.instant(role.get("created_at"))));
    m.put("createdBy", UamReportSupport.text(role, "created_by"));
    Instant modifiedAt =
        change == null ? UamReportSupport.instant(role.get("updated_at")) : change.at();
    m.put("modifiedAt", UamReportSupport.date(modifiedAt));
    m.put(
        "modifiedBy", change == null ? UamReportSupport.text(role, "updated_by") : change.actor());
    return m;
  }

  private Map<String, String> areas() {
    Map<String, String> areas = new HashMap<>();
    jdbc.query(
        "select permission, area from sec_permission_action",
        rs -> {
          areas.putIfAbsent(rs.getString(1), rs.getString(2));
        });
    return areas;
  }

  private Map<String, String> actions() {
    Map<String, List<String>> actions = new HashMap<>();
    jdbc.query(
        "select permission, action from sec_permission_action order by action",
        rs -> {
          actions.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
        });
    Map<String, String> joined = new HashMap<>();
    actions.forEach((k, v) -> joined.put(k, String.join(" / ", v)));
    return joined;
  }

  private Map<Long, Set<String>> grants() {
    Map<Long, Set<String>> grants = new HashMap<>();
    jdbc.query(
        "select role_id, permission from sec_role_permission",
        rs -> {
          grants.computeIfAbsent(rs.getLong(1), k -> new HashSet<>()).add(rs.getString(2));
        });
    return grants;
  }

  /** The last change of each role in the change log: time and actor (approver and request). */
  private Map<String, RoleChange> lastChanges() {
    Map<String, RoleChange> last = new HashMap<>();
    jdbc.query(
        "select subject, occurred_at, activity, request_no, done_by, approved_by"
            + " from sec_access_change_log where subject_type = 'ROLE' order by occurred_at, id",
        rs -> {
          String approver = rs.getString("approved_by");
          String actor =
              approver == null
                  ? rs.getString("done_by")
                  : approver + " (" + rs.getString("request_no") + ")";
          last.put(
              rs.getString("subject"),
              new RoleChange(
                  rs.getTimestamp("occurred_at").toInstant(),
                  UamReportSupport.activityName(rs.getString("activity")) + " / " + actor));
        });
    return last;
  }

  /**
   * The last change of a role.
   *
   * @param at time
   * @param actor activity and actor text
   */
  private record RoleChange(Instant at, String actor) {}
}
