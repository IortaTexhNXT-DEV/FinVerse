package com.iortatechnxt.brokerverse.disbursement.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * A Disbursement report defined by one constant query (ACCOUNTING_DISBURSEMENT_DESIGN 10, Appendix
 * B): category Disbursement (view {@code DISB_REPORT_VIEW}, export {@code DISB_REPORT_EXPORT},
 * archived), the company and, for period reports, a date range (default: the current month); the
 * rows are grouped by the first column when a group is given. Layouts the BRD does not detail are
 * drafts to confirm with BDOI (AQ16).
 */
public final class SqlReport implements ReportDefinition {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** Period start. */
  static final String FROM = "from";

  /** Period end (or as-of date). */
  static final String TO = "to";

  private final ReportMetadata metadata;
  private final Spec spec;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param spec definition
   * @param jdbc JDBC
   */
  SqlReport(Spec spec, NamedParameterJdbcTemplate jdbc) {
    this.spec = spec;
    this.jdbc = jdbc;
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    if (spec.period()) {
      params.add(
          ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
      params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    }
    this.metadata =
        ReportMetadata.disbursement(spec.code(), spec.title(), spec.description(), params);
  }

  @Override
  public ReportMetadata metadata() {
    return metadata;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, p.optionalDate(FROM).orElse(null));
    args.put(TO, p.optionalDate(TO).orElse(null));
    TabularReportBuilder builder =
        TabularReportBuilder.of(p).columns(spec.columns()).rows(rows(args)).presorted();
    if (spec.groupKey() != null) {
      builder.groupBy(spec.groupKey(), spec.groupLabel());
    }
    if (spec.note() != null) {
      builder.note(spec.note());
    }
    return builder.build();
  }

  private List<Map<String, Object>> rows(Map<String, Object> args) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> row : jdbc.queryForList(spec.sql(), args)) {
      Map<String, Object> out = new LinkedHashMap<>();
      row.forEach((k, v) -> out.put(k, local(v)));
      rows.add(out);
    }
    return rows;
  }

  private static Object local(Object v) {
    if (v instanceof Timestamp t) {
      return t.toInstant().atZone(DisbursementReports.MANILA).toLocalDate();
    }
    return v instanceof Date d ? d.toLocalDate() : v;
  }

  /**
   * The definition of a report.
   *
   * @param code report code
   * @param title title
   * @param description purpose with its BR ID
   * @param period whether it takes a date range
   * @param sql constant query (binds {@code :companyId}, {@code :from}, {@code :to})
   * @param columns columns
   * @param groupKey column to group by, null for none
   * @param groupLabel label of the group
   * @param note note printed under the report, may be null
   */
  record Spec(
      String code,
      String title,
      String description,
      boolean period,
      String sql,
      List<ReportColumn> columns,
      String groupKey,
      String groupLabel,
      String note) {}
}
