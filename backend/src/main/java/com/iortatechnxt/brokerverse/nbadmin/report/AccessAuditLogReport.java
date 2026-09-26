package com.iortatechnxt.brokerverse.nbadmin.report;

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
public class AccessAuditLogReport implements ReportDefinition {

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
      Map.of("LOGIN", "Log-in", "LOGIN_FAILED", "Failed Log-in", "LOGOUT", "Log-out");

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
        "Access activities with from and to values, done by, approved by and request number"
            + " (BRD 4.003.1)",
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
    List<Map<String, Object>> rows = new ArrayList<>();
    if (!REQUESTS.equals(activity)) {
      rows.addAll(changes(args, activity, roleNames));
    }
    if (UamReportSupport.ALL.equals(activity) || REQUESTS.equals(activity)) {
      rows.addAll(requestEvents(args));
    }
    if (p.flag(SIGN_INS)) {
      rows.addAll(signIns(args));
    }
    List<Map<String, Object>> shown =
        rows.stream()
            .filter(r -> user == null || involves(r, user))
            .sorted(Comparator.comparing(r -> (Instant) r.get(OCCURRED)))
            .map(AccessAuditLogReport::display)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(TIME, "Date"),
            ReportColumn.text(ACTIVITY, "Activity"),
            ReportColumn.text(FROM_COL, "From"),
            ReportColumn.text(TO_COL, "To"),
            ReportColumn.text(DONE_BY, "Done By"),
            ReportColumn.text(APPROVED_BY, "Approved By"),
            ReportColumn.text(REQUEST_NO, "Request No."))
        .rows(shown)
        .presorted()
        .withoutGrandTotal()
        .note("Times in Philippine time. \"Null\" = no value before the change.")
        .build();
  }

  private List<Map<String, Object>> changes(
      Map<String, Object> args, String activity, Map<String, String> roleNames) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> c :
        jdbc.queryForList(
            "select occurred_at, subject_type, subject, activity, attribute, from_value, to_value,"
                + " request_no, done_by, approved_by from sec_access_change_log"
                + " where occurred_at >= :start and occurred_at < :end",
            args)) {
      String code = UamReportSupport.text(c, ACTIVITY);
      boolean profile = PROFILE_ACTIVITIES.contains(code);
      if (skipped(activity, profile)) {
        continue;
      }
      String attribute = UamReportSupport.text(c, "attribute");
      boolean roles = UserAccessHistory.ROLES.equals(attribute);
      String from = UamReportSupport.text(c, "from_value");
      String to = UamReportSupport.text(c, "to_value");
      rows.add(
          entry(
              c.get(OCCURRED),
              UamReportSupport.activity(code, UamReportSupport.text(c, SUBJECT))
                  + (PLAIN_ATTRIBUTES.contains(attribute) ? "" : " (" + attribute + ")"),
              roles ? UamReportSupport.roleNames(from, roleNames) : from,
              roles ? UamReportSupport.roleNames(to, roleNames) : to,
              new Source(
                  UamReportSupport.text(c, "done_by"),
                  UamReportSupport.text(c, "approved_by"),
                  UamReportSupport.text(c, "request_no"),
                  UamReportSupport.text(c, SUBJECT))));
    }
    return rows;
  }

  private List<Map<String, Object>> requestEvents(Map<String, Object> args) {
    return jdbc
        .queryForList(
            "select e.occurred_at, e.action, e.from_status, e.to_status, e.actor, r.request_no,"
                + " r.request_type, coalesce(r.role_code, r.username) as subject"
                + " from nba_access_request_event e join nba_access_request r"
                + " on r.id = e.request_id where e.occurred_at >= :start and e.occurred_at < :end",
            args)
        .stream()
        .map(
            e ->
                entry(
                    e.get(OCCURRED),
                    ACTIONS.getOrDefault(
                            UamReportSupport.text(e, "action"),
                            UamReportSupport.words(UamReportSupport.text(e, "action")))
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
                        UamReportSupport.text(e, SUBJECT))))
        .toList();
  }

  private List<Map<String, Object>> signIns(Map<String, Object> args) {
    return jdbc
        .queryForList(
            "select occurred_at, username, entity_id, action, summary from audit_log"
                + " where action in ('LOGIN', 'LOGIN_FAILED', 'LOGOUT')"
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
                        UamReportSupport.text(a, "entity_id"))))
        .toList();
  }

  private static boolean skipped(String activity, boolean profile) {
    return USER_CHANGES.equals(activity) ? profile : PROFILE_CHANGES.equals(activity) && !profile;
  }

  private static Map<String, Object> entry(
      Object occurred, String activity, String from, String to, Source source) {
    Map<String, Object> m = new HashMap<>();
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

  private static Map<String, Object> display(Map<String, Object> row) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(TIME, UamReportSupport.dateTime((Instant) row.get(OCCURRED)));
    m.put(ACTIVITY, row.get(ACTIVITY));
    m.put(FROM_COL, UamReportSupport.orNull((String) row.get(FROM_COL)));
    m.put(TO_COL, UamReportSupport.orNull((String) row.get(TO_COL)));
    m.put(DONE_BY, row.get(DONE_BY));
    m.put(APPROVED_BY, row.get(APPROVED_BY));
    m.put(REQUEST_NO, row.get(REQUEST_NO));
    return m;
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

  /**
   * Who did an activity and where it came from.
   *
   * @param doneBy actor
   * @param approvedBy approver of the request, may be null
   * @param requestNo request number, may be null
   * @param subject user or group profile concerned
   */
  private record Source(String doneBy, String approvedBy, String requestNo, String subject) {}
}
