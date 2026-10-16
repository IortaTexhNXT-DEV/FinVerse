package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.NamedExport;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code UAM-REQUESTS} Group Profile Changes (BRD 1.008; FR-UA-018): the maker-checker changes of
 * the group profiles raised in a period (new group profile, change of definition or permissions,
 * deactivation, reactivation) by status, with the maker, the checker and their age in days (to the
 * decision, or to today while open). Grouped by status. Users and their membership are maintained
 * in UIDM-ISC, so requests about users (kept as history from before) are not listed.
 */
@Component
public class AccessRequestsReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "UAM-REQUESTS";

  private static final String STATUS = "status";
  private static final String TYPE = "type";
  private static final List<AccessRequestType> CHANGES =
      Arrays.stream(AccessRequestType.values()).filter(AccessRequestType::isGroupProfile).toList();
  private static final Map<String, String> CHANGE_NAMES =
      Map.of(
          "CREATE_ROLE", "New group profile",
          "MODIFY_ROLE_PERMISSIONS", "Change of definition or permissions",
          "DEACTIVATE_ROLE", "Deactivation",
          "REACTIVATE_ROLE", "Reactivation");
  private static final Set<String> OPEN =
      Set.of("DRAFT", "PENDING", "PENDING_SECOND", "RETURNED", "SCHEDULED", "FOR_IMPLEMENTATION");

  private final NamedParameterJdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC
   * @param clock clock
   */
  public AccessRequestsReport(NamedParameterJdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public String exportName() {
    return "Group Profile Changes Report";
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>(UamReportSupport.periodParams());
    params.add(
        ParameterSpec.select(
            STATUS, "Status", options(AccessRequestStatus.values()), UamReportSupport.ALL));
    params.add(
        ParameterSpec.select(
            TYPE,
            "Change",
            options(CHANGES.toArray(AccessRequestType[]::new)),
            UamReportSupport.ALL));
    return UamReportSupport.metadata(
        CODE,
        "Group Profile Changes",
        "Changes of the group profiles by status, with the maker, the checker and their age",
        params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate[] period = UamReportSupport.period(p);
    Map<String, Object> args = new HashMap<>();
    args.put("start", Timestamp.from(UamReportSupport.startOf(period[0])));
    args.put("end", Timestamp.from(UamReportSupport.endOf(period[1])));
    args.put(STATUS, p.text(STATUS));
    args.put(TYPE, p.text(TYPE));
    args.put("changes", CHANGES.stream().map(Enum::name).toList());
    LocalDate today = BusinessClock.today(clock);
    List<Map<String, Object>> rows =
        jdbc
            .queryForList(
                "select r.request_no, r.request_type, r.status, r.role_code,"
                    + " coalesce(g.name, r.role_code) as profile_name,"
                    + " r.permissions_added, r.permissions_removed,"
                    + " coalesce(r.submitted_by, r.created_by) as maker, r.decided_by,"
                    + " r.created_at, r.decided_at from nba_access_request r"
                    + " left join sec_role g on g.code = r.role_code"
                    + " where r.request_type in (:changes)"
                    + " and r.created_at >= :start and r.created_at < :end"
                    + " and (:status = 'ALL' or r.status = :status)"
                    + " and (:type = 'ALL' or r.request_type = :type)"
                    + " order by r.status, r.created_at, r.request_no",
                args)
            .stream()
            .map(r -> row(r, today))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("changeNo", "Change No."),
            ReportColumn.text(TYPE, "Change"),
            ReportColumn.text("profileCode", "Group Profile Code"),
            ReportColumn.text("profileName", "Group Profile"),
            ReportColumn.text("permissions", "Permissions Added / Removed"),
            ReportColumn.text("maker", "Maker"),
            ReportColumn.text("checker", "Checker"),
            ReportColumn.date("createdAt", "Date Raised"),
            ReportColumn.date("decidedAt", "Date Decided"),
            new ReportColumn("age", "Age (days)", ColumnType.NUMBER, false),
            ReportColumn.count("changes", "Changes"))
        .groupBy(STATUS, "Status")
        .rows(rows)
        .presorted()
        .build();
  }

  private static List<String> options(Enum<?>... values) {
    return Stream.concat(Stream.of(UamReportSupport.ALL), Arrays.stream(values).map(Enum::name))
        .toList();
  }

  private static Map<String, Object> row(Map<String, Object> r, LocalDate today) {
    String status = UamReportSupport.text(r, STATUS);
    Instant created = UamReportSupport.instant(r.get("created_at"));
    Instant decided = UamReportSupport.instant(r.get("decided_at"));
    LocalDate createdOn = UamReportSupport.date(created);
    LocalDate until =
        OPEN.contains(status) || decided == null ? today : UamReportSupport.date(decided);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(STATUS, UamReportSupport.words(status));
    m.put("changeNo", UamReportSupport.text(r, "request_no"));
    String type = UamReportSupport.text(r, "request_type");
    m.put(TYPE, CHANGE_NAMES.getOrDefault(type, UamReportSupport.words(type)));
    m.put("profileCode", UamReportSupport.text(r, "role_code"));
    m.put("profileName", UamReportSupport.text(r, "profile_name"));
    m.put("permissions", permissions(r));
    m.put("maker", UamReportSupport.text(r, "maker"));
    m.put("checker", UamReportSupport.text(r, "decided_by"));
    m.put("createdAt", createdOn);
    m.put("decidedAt", UamReportSupport.date(decided));
    m.put("age", Math.max(0, ChronoUnit.DAYS.between(createdOn, until)));
    m.put("changes", 1);
    return m;
  }

  /** The count of permissions added and removed ("3 added, 1 removed"); empty when none. */
  private static String permissions(Map<String, Object> r) {
    int added = count(UamReportSupport.text(r, "permissions_added"));
    int removed = count(UamReportSupport.text(r, "permissions_removed"));
    List<String> parts = new ArrayList<>();
    if (added > 0) {
      parts.add(added + " added");
    }
    if (removed > 0) {
      parts.add(removed + " removed");
    }
    return String.join(", ", parts);
  }

  private static int count(String codes) {
    return codes == null || codes.isBlank() ? 0 : codes.split(",").length;
  }
}
