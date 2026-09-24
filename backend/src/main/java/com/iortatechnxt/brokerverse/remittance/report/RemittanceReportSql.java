package com.iortatechnxt.brokerverse.remittance.report;

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
 * Shared parameters and SQL access of the Remittance reports (RMTID.039, Annex III): every report
 * is an Operations report (view OPS_REPORT_VIEW, export OPS_REPORT_EXPORT, archived), runs for the
 * company of the header and, where it covers a period, a date range (default: the current month).
 * Layouts the Annex does not detail are drafts to confirm (OQ42).
 */
@Component
@Transactional(readOnly = true)
public class RemittanceReportSql {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** Period start. */
  static final String FROM = "from";

  /** Period end. */
  static final String TO = "to";

  /** Insurer filter. */
  static final String INSURER = "insurer";

  /** "Any" option of the select filters. */
  static final String ALL = "ALL";

  /** Note on draft layouts. */
  static final String DRAFT_NOTE = "Draft layout - fields to confirm with BDOI (OQ42).";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc named-parameter JDBC
   */
  public RemittanceReportSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Metadata of a remittance report.
   *
   * @param code code
   * @param title title
   * @param description description with its BR ID
   * @param period whether the report takes a date range
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, boolean period, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    if (period) {
      params.add(
          ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
      params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    }
    params.add(ParameterSpec.optional(INSURER, "Insurer code", ParameterType.TEXT));
    params.addAll(List.of(extra));
    return ReportMetadata.operations(code, title, description, params);
  }

  /**
   * The bind values of the common parameters.
   *
   * @param p parameters
   * @return company, dates (null when absent) and insurer (null when blank)
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, p.optionalDate(FROM).orElse(null));
    args.put(TO, p.optionalDate(TO).orElse(null));
    args.put(
        INSURER, p.optionalText(INSURER).map(String::strip).filter(s -> !s.isEmpty()).orElse(null));
    return args;
  }

  /**
   * A select filter's value, null for "all".
   *
   * @param p parameters
   * @param name parameter
   * @return value
   */
  static String selected(ReportParameters p, String name) {
    return p.optionalText(name).filter(v -> !ALL.equals(v)).orElse(null);
  }

  /**
   * Runs a constant query; SQL dates and timestamps become local dates (Philippine time).
   *
   * @param sql constant SQL
   * @param args bind values
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> row : jdbc.queryForList(sql, args)) {
      Map<String, Object> out = new LinkedHashMap<>();
      for (Map.Entry<String, Object> e : row.entrySet()) {
        Object v = e.getValue();
        if (v instanceof Timestamp t) {
          v = t.toInstant().atZone(MANILA).toLocalDate();
        } else if (v instanceof Date d) {
          v = d.toLocalDate();
        }
        out.put(e.getKey(), v);
      }
      rows.add(out);
    }
    return rows;
  }
}
