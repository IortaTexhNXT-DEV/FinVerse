package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.report.core.NamedExport;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code UAM-AUDIT-LOG} User Access Audit Log (BRD 4.003.1, sample D; UAM-NFR-35, 41; FR-UA-063):
 * for a period, every access activity with its from and to values, who did it, the approver and the
 * request number - the access change log (users and group profiles), the request history (created,
 * submitted, returned, approved, rejected, cancelled, implemented) and, on request, the log-ins,
 * failed log-ins and log-outs of the audit trail. Sorted by date and time.
 */
@Component
public class AccessAuditLogReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "UAM-AUDIT-LOG";

  /** Activity filter: changes to users. */
  static final String USER_CHANGES = "USER_CHANGES";

  /** Activity filter: changes to group profiles. */
  static final String PROFILE_CHANGES = "GROUP_PROFILE_CHANGES";

  /** Activity filter: request history. */
  static final String REQUESTS = "REQUESTS";

  private static final String USER = "user";
  private static final String ACTIVITY = "activity";
  private static final String SIGN_INS = "includeSignIns";
  private static final String TIME = "time";
  private static final String FROM_COL = "fromValue";
  private static final String TO_COL = "toValue";
  private static final String DONE_BY = "doneBy";
  private static final String APPROVED_BY = "approvedBy";
  private static final String REQUEST_NO = "requestNo";
  private static final String SUBJECT = "subject";
  private static final String MODULE = "module";
  private static final String USER_ID = "userId";
  private static final String ROLE = "role";
  private static final String ACTION = "action";
  private static final String UPDATE = "Update";
  private static final String IP = "ipAddress";
  private static final String ROLE_NAMES = "role_names";
  private static final String IP_ADDRESS = "ip_address";
  private static final String OCCURRED = "occurred_at";
  private static final String START = "start";
  private static final String END = "end";
  private static final Set<String> PROFILE_ACTIVITIES =
      Set.of("CREATE_ROLE", "ROLE_PERMISSIONS", "DEACTIVATE_ROLE", "REACTIVATE_ROLE");
  private static final Set<String> PLAIN_ATTRIBUTES =
      Set.of(UserAccessHistory.ROLES, "enabled", "active", "password", "locked");

  private static final Map<String, String> ACTIONS =
      Map.ofEntries(
          Map.entry("SAVE", "Saved"),
          Map.entry("EDIT", "Edited"),
          Map.entry("SUBMIT", "Submitted"),
          Map.entry("RESUBMIT", "Resubmitted"),
          Map.entry("RETURN", "Returned"),
          Map.entry("CANCEL", "Cancelled"),
          Map.entry("APPROVE", "Approved"),
          Map.entry("SECOND_APPROVE", "Second Approval of"),
          Map.entry("REJECT", "Rejected"),
          Map.entry("SCHEDULE", "Scheduled"),
          Map.entry("APPLY", "Applied"),
          Map.entry("APPLY_FAILED", "Failed to Apply"),
          Map.entry("FOR_IMPLEMENTATION", "For Implementation:"),
          Map.entry("IMPLEMENT", "Implemented"));

  private static final Map<String, String> TYPES =
      Map.ofEntries(
          Map.entry("CREATE_USER", "Enroll New User"),
          Map.entry("MODIFY_USER", "Modify User"),
          Map.entry("MODIFY_ROLES", "Modify User Group Profile"),
          Map.entry("DISABLE_USER", "Deactivate User"),
          Map.entry("ENABLE_USER", "Reactivate User"),
          Map.entry("MODIFY_ROLE_PERMISSIONS", "Modify Group Profile Access"),
          Map.entry("CREATE_ROLE", "Create Group Profile"),
          Map.entry("DEACTIVATE_ROLE", "Deactivate Group Profile"),
          Map.entry("REACTIVATE_ROLE", "Reactivate Group Profile"));

  private static final Map<String, String> SIGN_IN_ACTIONS =
      Map.of(
          "LOGIN", "Log-in",
          "LOGIN_FAILED", "Failed Log-in",
          "LOGOUT", "Log-out",
          "INACTIVITY", "Inactivity of",
          "TIMEOUT", "Time-out of");

  /** BDOI's action words (FRUM.008.01) of the access changes, request events and sign-ins. */
  private static final Map<String, String> ACTION_WORDS =
      Map.ofEntries(
          Map.entry("CREATE_USER", "Create"),
          Map.entry("MODIFY_USER", UPDATE),
          Map.entry("ROLES_CHANGED", UPDATE),
          Map.entry("DISABLE_USER", "Deactivate"),
          Map.entry("ENABLE_USER", "Reactivate"),
          Map.entry("UNLOCK", "Unlock"),
          Map.entry("PASSWORD_RESET", "Reset Password"),
          Map.entry("CREATE_ROLE", "Create"),
          Map.entry("ROLE_PERMISSIONS", UPDATE),
          Map.entry("DEACTIVATE_ROLE", "Deactivate"),
          Map.entry("REACTIVATE_ROLE", "Reactivate"),
          Map.entry("DATA_SCOPE_CHANGED", UPDATE),
          Map.entry("SAVE", "Create"),
          Map.entry("EDIT", UPDATE),
          Map.entry("SUBMIT", "Submit"),
          Map.entry("RESUBMIT", "Submit"),
          Map.entry("RETURN", "Return"),
          Map.entry("CANCEL", "Cancel"),
          Map.entry("APPROVE", "Approve"),
          Map.entry("SECOND_APPROVE", "Approve"),
          Map.entry("REJECT", "Reject"),
          Map.entry("SCHEDULE", "Schedule"),
          Map.entry("APPLY", "Apply"),
          Map.entry("APPLY_FAILED", "Apply"),
          Map.entry("FOR_IMPLEMENTATION", "Submit"),
          Map.entry("IMPLEMENT", "Implement"),
          Map.entry("LOGIN", "Login"),
          Map.entry("LOGIN_FAILED", "Failed Login"),
          Map.entry("LOGOUT", "Logout"),
          Map.entry("INACTIVITY", "Inactivity"),
          Map.entry("TIMEOUT", "Timeout"));

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC
   */
  public AccessAuditLogReport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String exportName() {
    return "Audit Logs";
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>(UamReportSupport.periodParams());
    params.add(ParameterSpec.optional(USER, "User", ParameterType.TEXT));
    params.add(
        ParameterSpec.select(
            ACTIVITY,
            "Activity",
            List.of(UamReportSupport.ALL, USER_CHANGES, PROFILE_CHANGES, REQUESTS),
            UamReportSupport.ALL));
    params.add(
        ParameterSpec.optional(SIGN_INS, "Include Log-ins and Log-outs", ParameterType.BOOLEAN)
            .withDefault("false"));
    return UamReportSupport.metadata(
        CODE,
        "User Access Audit Log",
        "Access activities with from and to values, done by, approved by and request number",
        params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate[] period = UamReportSupport.period(p);
    Map<String, Object> args = new HashMap<>();
    args.put(START, Timestamp.from(UamReportSupport.startOf(period[0])));
    args.put(END, Timestamp.from(UamReportSupport.endOf(period[1])));
    String activity = p.text(ACTIVITY);
    String user = p.optionalText(USER).map(String::trim).orElse(null);
    Map<String, String> roleNames = roleNames();
    Map<String, String> branchNames = branchNames();
    List<Map<String, Object>> rows = new ArrayList<>();
    if (!REQUESTS.equals(activity)) {
      rows.addAll(changes(args, activity, roleNames, branchNames));
    }
    if (UamReportSupport.ALL.equals(activity) || REQUESTS.equals(activity)) {
      rows.addAll(requestEvents(args));
    }
    if (p.flag(SIGN_INS)) {
      rows.addAll(signIns(args));
    }
    Map<String, String> windowsIds = windowsIds();
    List<Map<String, Object>> shown =
        rows.stream()
            .filter(r -> user == null || involves(r, user))
            .sorted(Comparator.comparing(r -> (Instant) r.get(OCCURRED)))
            .map(r -> display(r, windowsIds))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(TIME, "Timestamp"),
            ReportColumn.text(MODULE, "Module"),
            ReportColumn.text(USER_ID, "User Id"),
            ReportColumn.text(DONE_BY, "Performed By"),
            ReportColumn.text(ROLE, "User Group Profile"),
            ReportColumn.text(ACTION, "Action"),
            ReportColumn.text(FROM_COL, "Old Value"),
            ReportColumn.text(TO_COL, "New Value"),
            ReportColumn.text(ACTIVITY, "Activity"),
            ReportColumn.text(IP, "IP Address"),
            ReportColumn.text(APPROVED_BY, "Approved By"),
            ReportColumn.text(REQUEST_NO, "Request No."))
        .rows(shown)
        .presorted()
        .withoutGrandTotal()
        .note("Times in Philippine time. \"Null\" = no value before the change.")
        .build();
  }

  private List<Map<String, Object>> changes(
      Map<String, Object> args,
      String activity,
      Map<String, String> roleNames,
      Map<String, String> branchNames) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> c :
        jdbc.queryForList(
            "select occurred_at, subject_type, subject, activity, attribute, from_value, to_value,"
                + " request_no, done_by, approved_by, role_names, ip_address"
                + " from sec_access_change_log"
                + " where occurred_at >= :start and occurred_at < :end",
            args)) {
      String code = UamReportSupport.text(c, ACTIVITY);
      boolean profile = PROFILE_ACTIVITIES.contains(code);
      if (skipped(activity, profile)) {
        continue;
      }
      String attribute = UamReportSupport.text(c, "attribute");
      String from = UamReportSupport.text(c, "from_value");
      String to = UamReportSupport.text(c, "to_value");
      rows.add(
          entry(
              c.get(OCCURRED),
              UamReportSupport.activity(code, UamReportSupport.text(c, SUBJECT))
                  + (PLAIN_ATTRIBUTES.contains(attribute)
                      ? ""
                      : " (" + UamReportSupport.attributeLabel(attribute) + ")"),
              UamReportSupport.shownValue(attribute, from, roleNames, branchNames),
              UamReportSupport.shownValue(attribute, to, roleNames, branchNames),
              new Source(
                  UamReportSupport.text(c, "done_by"),
                  UamReportSupport.text(c, "approved_by"),
                  UamReportSupport.text(c, "request_no"),
                  UamReportSupport.text(c, SUBJECT)),
              new Origin(
                  ACTION_WORDS.get(code),
                  UamReportSupport.text(c, ROLE_NAMES),
                  UamReportSupport.text(c, IP_ADDRESS))));
    }
    return rows;
  }

  private List<Map<String, Object>> requestEvents(Map<String, Object> args) {
    return jdbc
        .queryForList(
            "select e.occurred_at, e.action, e.from_status, e.to_status, e.actor, r.request_no,"
                + " r.request_type, coalesce(r.role_code, r.username) as subject,"
                + " e.role_names, e.ip_address"
                + " from nba_access_request_event e join nba_access_request r"
                + " on r.id = e.request_id where e.occurred_at >= :start and e.occurred_at < :end",
            args)
        .stream()
        .map(
            e ->
                entry(
                    e.get(OCCURRED),
                    ACTIONS.getOrDefault(
                            UamReportSupport.text(e, ACTION),
                            UamReportSupport.words(UamReportSupport.text(e, ACTION)))
                        + " Request to "
                        + TYPES.getOrDefault(
                            UamReportSupport.text(e, "request_type"),
                            UamReportSupport.words(UamReportSupport.text(e, "request_type")))
                        + " "
                        + UamReportSupport.text(e, SUBJECT),
                    UamReportSupport.words(UamReportSupport.text(e, "from_status")),
                    UamReportSupport.words(UamReportSupport.text(e, "to_status")),
                    new Source(
                        UamReportSupport.text(e, "actor"),
                        null,
                        UamReportSupport.text(e, "request_no"),
                        UamReportSupport.text(e, SUBJECT)),
                    new Origin(
                        ACTION_WORDS.get(UamReportSupport.text(e, ACTION)),
                        UamReportSupport.text(e, ROLE_NAMES),
                        UamReportSupport.text(e, IP_ADDRESS))))
        .toList();
  }

  private List<Map<String, Object>> signIns(Map<String, Object> args) {
    return jdbc
        .queryForList(
            "select occurred_at, username, entity_id, action, summary, role_names, ip_address"
                + " from audit_log"
                + " where action in ('LOGIN', 'LOGIN_FAILED', 'LOGOUT', 'INACTIVITY', 'TIMEOUT')"
                + " and occurred_at >= :start and occurred_at < :end",
            args)
        .stream()
        .map(
            a ->
                entry(
                    a.get(OCCURRED),
                    SIGN_IN_ACTIONS.get(UamReportSupport.text(a, "action"))
                        + " "
                        + UamReportSupport.text(a, "entity_id"),
                    "-",
                    UamReportSupport.text(a, "summary"),
                    new Source(
                        UamReportSupport.text(a, "username"),
                        null,
                        null,
                        UamReportSupport.text(a, "entity_id")),
                    new Origin(
                        ACTION_WORDS.get(UamReportSupport.text(a, ACTION)),
                        UamReportSupport.text(a, ROLE_NAMES),
                        UamReportSupport.text(a, IP_ADDRESS))))
        .toList();
  }

  private static boolean skipped(String activity, boolean profile) {
    return USER_CHANGES.equals(activity) ? profile : PROFILE_CHANGES.equals(activity) && !profile;
  }

  private static Map<String, Object> entry(
      Object occurred, String activity, String from, String to, Source source, Origin origin) {
    Map<String, Object> m = new HashMap<>();
    m.put(ACTION, origin.action());
    m.put(ROLE, origin.roles());
    m.put(IP, origin.address());
    m.put(OCCURRED, UamReportSupport.instant(occurred));
    m.put(ACTIVITY, activity);
    m.put(FROM_COL, from);
    m.put(TO_COL, to);
    m.put(DONE_BY, source.doneBy());
    m.put(APPROVED_BY, source.approvedBy());
    m.put(REQUEST_NO, source.requestNo());
    m.put(SUBJECT, source.subject());
    return m;
  }

  private static boolean involves(Map<String, Object> row, String user) {
    return UamReportSupport.same(user, (String) row.get(SUBJECT))
        || UamReportSupport.same(user, (String) row.get(DONE_BY))
        || UamReportSupport.same(user, (String) row.get(APPROVED_BY));
  }

  private static Map<String, Object> display(
      Map<String, Object> row, Map<String, String> windowsIds) {
    Map<String, Object> m = new LinkedHashMap<>();
    String doneBy = (String) row.get(DONE_BY);
    m.put(TIME, UamReportSupport.dateTime((Instant) row.get(OCCURRED)));
    m.put(MODULE, "User Access Maintenance");
    m.put(USER_ID, doneBy == null ? null : windowsIds.get(doneBy.toLowerCase(Locale.ROOT)));
    m.put(DONE_BY, doneBy);
    m.put(ROLE, row.get(ROLE));
    m.put(ACTION, row.get(ACTION));
    m.put(FROM_COL, UamReportSupport.orNull((String) row.get(FROM_COL)));
    m.put(TO_COL, UamReportSupport.orNull((String) row.get(TO_COL)));
    m.put(ACTIVITY, row.get(ACTIVITY));
    m.put(IP, row.get(IP));
    m.put(APPROVED_BY, row.get(APPROVED_BY));
    m.put(REQUEST_NO, row.get(REQUEST_NO));
    return m;
  }

  private Map<String, String> windowsIds() {
    Map<String, String> ids = new HashMap<>();
    jdbc.query(
        "select lower(username), windows_id from sec_user where windows_id is not null",
        rs -> {
          ids.put(rs.getString(1), rs.getString(2));
        });
    return ids;
  }

  private Map<String, String> roleNames() {
    Map<String, String> names = new HashMap<>();
    jdbc.query(
        "select code, name from sec_role",
        rs -> {
          names.put(rs.getString(1), rs.getString(2));
        });
    return names;
  }

  /** The branches by id, as the home branch of a user is logged by its id. */
  private Map<String, String> branchNames() {
    Map<String, String> names = new HashMap<>();
    jdbc.query(
        "select id, code, name from org_branch",
        rs -> {
          names.put(rs.getString("id"), rs.getString("code") + " - " + rs.getString("name"));
        });
    return names;
  }

  /**
   * Who did an activity and where it came from.
   *
   * @param doneBy actor
   * @param approvedBy approver of the request, may be null
   * @param requestNo request number, may be null
   * @param subject user or group profile concerned
   */
  private record Source(String doneBy, String approvedBy, String requestNo, String subject) {}

  /**
   * BDOI's action word of an activity, and the roles and source address of its actor.
   *
   * @param action action word
   * @param roles group profiles of the actor at the time, may be null
   * @param address source (IP) address, may be null
   */
  private record Origin(String action, String roles, String address) {}
}
