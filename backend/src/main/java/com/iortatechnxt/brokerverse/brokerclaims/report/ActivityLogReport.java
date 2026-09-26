package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.brokerclaims.report.ClaimActivitySource.ActivityQuery;
import com.iortatechnxt.brokerverse.brokerclaims.report.ClaimActivitySource.ClaimActivity;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Claims Activity Log (BRCLM.041/042, NFR 15.08; FR-CM-066): status changes, field changes and
 * diary entries of the claims, and the activities of the {@link ClaimActivitySource} contributors
 * (insurer updates and location reference changes, CL1-A), with user and time, oldest first. The
 * report reads the history tables only; nothing is edited from it.
 */
@Component
public class ActivityLogReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "BCL-ACTIVITY-LOG";

  private static final String FROM = "periodFrom";
  private static final String TO = "periodTo";
  private static final String USER = "user";
  private static final String CLAIM = "claimNo";
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final String CLAIM_SCOPE =
      " where c.company_id = :companyId"
          + " and (cast(:claimNo as varchar) is null or c.claim_no = :claimNo)";

  private static final String SQL =
      "select h.changed_at as at, h.changed_by as who, c.claim_no, 'Status' as activity,"
          + " coalesce(fs.label, h.from_status, '-') || ' (' || coalesce(h.from_phase, '-')"
          + " || ') -> ' || coalesce(ts.label, h.to_status) || ' (' || h.to_phase || ')' as detail,"
          + " h.remark from bcl_status_history h join bcl_claim c on c.id = h.claim_id"
          + " left join lov_value fs on fs.type_code = 'BCL_CLAIM_STATUS' and fs.code = h.from_status"
          + " left join lov_value ts on ts.type_code = 'BCL_CLAIM_STATUS' and ts.code = h.to_status"
          + CLAIM_SCOPE
          + " and h.changed_at >= :fromTs and h.changed_at < :toTs"
          + " union all"
          + " select e.changed_at, e.changed_by, c.claim_no, 'Field: ' || e.field,"
          + " coalesce(e.old_value, '-') || ' -> ' || coalesce(e.new_value, '-'), e.reason"
          + " from bcl_claim_event e join bcl_claim c on c.id = e.claim_id"
          + CLAIM_SCOPE
          + " and e.changed_at >= :fromTs and e.changed_at < :toTs"
          + " union all"
          + " select d.created_at, d.created_by, c.claim_no, 'Diary: ' || d.entry_type,"
          + " d.text, case when d.due_date is null then null else 'Due ' || d.due_date end"
          + " from bcl_diary_entry d join bcl_claim c on c.id = d.claim_id"
          + CLAIM_SCOPE
          + " and d.created_at >= :fromTs and d.created_at < :toTs"
          + " union all"
          + " select d.done_at, d.done_by, c.claim_no, 'Diary done', d.text, d.done_remark"
          + " from bcl_diary_entry d join bcl_claim c on c.id = d.claim_id"
          + CLAIM_SCOPE
          + " and d.done_at >= :fromTs and d.done_at < :toTs";

  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectProvider<ClaimActivitySource> sources;

  /**
   * Creates the report.
   *
   * @param jdbc named-parameter JDBC
   * @param sources contributors (CL1-A: insurer updates, location references)
   */
  public ActivityLogReport(
      NamedParameterJdbcTemplate jdbc, ObjectProvider<ClaimActivitySource> sources) {
    this.jdbc = jdbc;
    this.sources = sources;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(BclReportSql.COMPANY, "Company", ParameterType.COMPANY));
    params.add(
        ParameterSpec.required(FROM, "Period from", ParameterType.DATE).withDefault("MONTH_START"));
    params.add(ParameterSpec.required(TO, "Period to", ParameterType.DATE).withDefault("TODAY"));
    params.add(ParameterSpec.optional(USER, "User", ParameterType.TEXT));
    params.add(ParameterSpec.optional(CLAIM, "Claim number", ParameterType.TEXT));
    return ReportMetadata.claimsHandling(
        CODE,
        "Claims Activity Log",
        "Status and field changes, insurer updates, location references and diary, with user and"
            + " time (BRCLM.041/042)",
        params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = BclReportSql.args(p);
    Instant from = start((LocalDate) args.get(FROM));
    Instant to = start(((LocalDate) args.get(TO)).plusDays(1));
    args.put("fromTs", Timestamp.from(from));
    args.put("toTs", Timestamp.from(to));
    String user = (String) args.get(USER);
    List<ClaimActivity> activities = new ArrayList<>();
    jdbc.query(
        SQL,
        args,
        rs -> {
          activities.add(
              new ClaimActivity(
                  rs.getTimestamp("at").toInstant(),
                  rs.getString("who"),
                  rs.getString("claim_no"),
                  rs.getString("activity"),
                  rs.getString("detail"),
                  rs.getString("remark")));
        });
    ActivityQuery query =
        new ActivityQuery(
            (Long) args.get(BclReportSql.COMPANY), from, to, user, (String) args.get(CLAIM));
    sources.orderedStream().forEach(s -> activities.addAll(s.activities(query)));
    List<Map<String, Object>> rows =
        activities.stream()
            .filter(a -> user == null || CurrentUser.sameUser(user, a.user()))
            .sorted(Comparator.comparing(ClaimActivity::at))
            .map(ActivityLogReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("at", "Date and Time"),
            ReportColumn.text("user", "User"),
            ReportColumn.text("claim_no", "Claim Number"),
            ReportColumn.text("activity", "Activity"),
            ReportColumn.text("detail", "Change"),
            ReportColumn.text("remark", "Remark / Reason"))
        .rows(rows)
        .presorted()
        .build();
  }

  private static Map<String, Object> row(ClaimActivity a) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("at", a.at().atZone(ClaimAgeing.MANILA).format(TIME));
    row.put("user", a.user());
    row.put("claim_no", a.claimNo());
    row.put("activity", a.activity());
    row.put("detail", a.detail());
    row.put("remark", a.remark());
    return row;
  }

  private static Instant start(LocalDate day) {
    return day.atStartOfDay(ClaimAgeing.MANILA).toInstant();
  }
}
