package com.iortatechnxt.brokerverse.collections.escalation.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared parameters and SQL access of the Collections reports of wave C1-B (COLLECTIONS_DESIGN
 * section 11: escalations, broken promises, installments due). Every report is a Collections report
 * ({@code CLX_REPORT_VIEW}, export {@code CLX_EXPORT}, archived) for the company of the header.
 * Layouts are drafts until BDOI confirms them (CQ22).
 */
@Component
@Transactional(readOnly = true)
public class PlanReportSql {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** Period start. */
  static final String FROM = "from";

  /** Period end. */
  static final String TO = "to";

  /** Note on draft layouts. */
  static final String DRAFT_NOTE = "Draft layout - fields to confirm with BDOI (CQ22).";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc named-parameter JDBC
   */
  public PlanReportSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Metadata of a report over a period.
   *
   * @param code code
   * @param title title
   * @param description description with its BR ID
   * @param fromDefault default of the period start (e.g. MONTH_START)
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, String fromDefault, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault(fromDefault));
    params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    params.addAll(List.of(extra));
    return ReportMetadata.collections(code, title, description, params);
  }

  /**
   * The bind values of the common parameters.
   *
   * @param p parameters
   * @return company and dates
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, p.date(FROM));
    args.put(TO, p.date(TO));
    return args;
  }

  /**
   * Runs a constant query; SQL dates and timestamps become local dates (Philippine time).
   *
   * @param sql constant SQL
   * @param args bind values
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    return jdbc.queryForList(sql, args).stream().map(PlanReportSql::local).toList();
  }

  private static Map<String, Object> local(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach((key, value) -> out.put(key, localValue(value)));
    return out;
  }

  private static Object localValue(Object value) {
    return switch (value) {
      case Timestamp t -> t.toInstant().atZone(MANILA).toLocalDate();
      case Date d -> d.toLocalDate();
      case null, default -> value;
    };
  }
}
