package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared parameters and SQL of the compliance reports (SNSRP-901, 903; spec section 7, FRS 6.1):
 * the company, the period ({@code fromDate} / {@code toDate}, checked by the report framework), the
 * case filters (marketing unit, unit head, disposition, case status, case type) and constant SQL
 * whose timestamps are shown in Philippine time. The columns are the project's proposal until BDOI
 * confirms the layouts (SQ17).
 */
@Component
@Transactional(readOnly = true)
public class ScrReportSql {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Period start. */
  public static final String FROM = "fromDate";

  /** Period end. */
  public static final String TO = "toDate";

  /** Marketing unit filter. */
  public static final String UNIT = "marketingUnit";

  /** Unit head filter. */
  public static final String HEAD = "unitHead";

  /** Disposition filter. */
  public static final String DISPOSITION = "disposition";

  /** Case status (stage) filter. */
  public static final String STATUS = "caseStatus";

  /** Note printed under every compliance report. */
  static final String LAYOUT_NOTE =
      "Columns proposed by the project until BDOI confirms them (SQ17).";

  /** The optional case filters on {@code scr_case k}. */
  static final String CASE_FILTERS =
      " and (cast(:marketingUnit as varchar) is null or k.marketing_unit = :marketingUnit)"
          + " and (cast(:unitHead as varchar) is null or lower(k.unit_head) = lower(:unitHead))"
          + " and (cast(:disposition as varchar) is null or k.disposition = :disposition)"
          + " and (cast(:caseStatus as varchar) is null or k.stage = :caseStatus)";

  /** Philippine calendar day of a timestamp column (the period filter). */
  static final String DAY = " at time zone 'Asia/Manila' as date)";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc named-parameter JDBC
   */
  public ScrReportSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The company parameter.
   *
   * @return spec
   */
  static ParameterSpec company() {
    return ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY);
  }

  /**
   * The period parameters (month to date by default).
   *
   * @return from and to
   */
  static List<ParameterSpec> period() {
    return List.of(
        ParameterSpec.required(FROM, "Date From", ParameterType.DATE).withDefault("MONTH_START"),
        ParameterSpec.required(TO, "Date To", ParameterType.DATE).withDefault("TODAY"));
  }

  /**
   * The case filters of SNSRP-901.
   *
   * @return marketing unit, unit head, disposition and case status
   */
  static List<ParameterSpec> caseFilters() {
    return List.of(
        ParameterSpec.optional(UNIT, "Marketing Unit", ParameterType.TEXT),
        ParameterSpec.optional(HEAD, "Unit Head", ParameterType.TEXT),
        ParameterSpec.optional(DISPOSITION, "Disposition", ParameterType.TEXT),
        ParameterSpec.optional(STATUS, "Case Status", ParameterType.TEXT));
  }

  /**
   * The bind values of the common parameters present in a report.
   *
   * @param p parameters
   * @return company, period and case filters (null when absent)
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.optionalLong(COMPANY).orElse(null));
    args.put(FROM, p.optionalDate(FROM).orElse(null));
    args.put(TO, p.optionalDate(TO).orElse(null));
    for (String name : List.of(UNIT, HEAD, DISPOSITION, STATUS)) {
      args.put(name, text(p, name));
    }
    return args;
  }

  /**
   * An optional text parameter, stripped; null when blank.
   *
   * @param p parameters
   * @param name parameter
   * @return value
   */
  static String text(ReportParameters p, String name) {
    return p.optionalText(name).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
  }

  /**
   * A whole-number column.
   *
   * @param key key
   * @param label label
   * @return column
   */
  static ReportColumn number(String key, String label) {
    return new ReportColumn(key, label, ColumnType.NUMBER, false);
  }

  /**
   * Runs a constant query; timestamps become Philippine date-times, SQL dates local dates.
   *
   * @param sql constant SQL
   * @param args bind values
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    return jdbc.queryForList(sql, args).stream().map(ScrReportSql::local).toList();
  }

  private static Map<String, Object> local(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach((column, value) -> out.put(column, localValue(value)));
    return out;
  }

  private static Object localValue(Object value) {
    if (value instanceof Timestamp stamp) {
      return STAMP.format(stamp.toInstant().atZone(MANILA));
    }
    return value instanceof Date day ? day.toLocalDate() : value;
  }
}
