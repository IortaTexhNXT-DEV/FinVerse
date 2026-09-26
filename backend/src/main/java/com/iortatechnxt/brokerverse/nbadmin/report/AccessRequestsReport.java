package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.sql.Date;
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
 * {@code UAM-REQUESTS} Access Requests (BRD 1.008; FR-UA-018): the access requests created in a
 * period by status, type, requester and approver, with their age in days (to the decision, or to
 * today while open). Grouped by status.
 */
@Component
public class AccessRequestsReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "UAM-REQUESTS";

  private static final String STATUS = "status";
  private static final String TYPE = "type";
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
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>(UamReportSupport.periodParams());
    params.add(
        ParameterSpec.select(
            STATUS, "Status", options(AccessRequestStatus.values()), UamReportSupport.ALL));
    params.add(
        ParameterSpec.select(
            TYPE, "Request Type", options(AccessRequestType.values()), UamReportSupport.ALL));
    return UamReportSupport.metadata(
        CODE,
        "Access Requests",
        "Access requests by status, type, requester and approver, with their age (BRD 1.008)",
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
    LocalDate today = LocalDate.now(clock.withZone(UamReportSupport.MANILA));
    List<Map<String, Object>> rows =
        jdbc
            .queryForList(
                "select request_no, request_type, status, coalesce(role_code, username) as subject,"
                    + " coalesce(submitted_by, created_by) as requester,"
                    + " coalesce(decided_by, assigned_approver) as approver, created_at,"
                    + " decided_at, effective_from from nba_access_request"
                    + " where created_at >= :start and created_at < :end"
                    + " and (:status = 'ALL' or status = :status)"
                    + " and (:type = 'ALL' or request_type = :type)"
                    + " order by status, created_at, request_no",
                args)
            .stream()
            .map(r -> row(r, today))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.text(TYPE, "Request Type"),
            ReportColumn.text("subject", "User / Group Profile"),
            ReportColumn.text("requester", "Requester"),
            ReportColumn.text("approver", "Approver"),
            ReportColumn.date("createdAt", "Date Created"),
            ReportColumn.date("decidedAt", "Date Decided"),
            ReportColumn.date("effectiveFrom", "Effective Date"),
            new ReportColumn("age", "Age (days)", ColumnType.NUMBER, false),
            ReportColumn.count("requests", "Requests"))
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
    m.put("requestNo", UamReportSupport.text(r, "request_no"));
    m.put(TYPE, UamReportSupport.words(UamReportSupport.text(r, "request_type")));
    m.put("subject", UamReportSupport.text(r, "subject"));
    m.put("requester", UamReportSupport.text(r, "requester"));
    m.put("approver", UamReportSupport.text(r, "approver"));
    m.put("createdAt", createdOn);
    m.put("decidedAt", UamReportSupport.date(decided));
    Object effective = r.get("effective_from");
    m.put("effectiveFrom", effective instanceof Date d ? d.toLocalDate() : null);
    m.put("age", Math.max(0, ChronoUnit.DAYS.between(createdOn, until)));
    m.put("requests", 1);
    return m;
  }
}
