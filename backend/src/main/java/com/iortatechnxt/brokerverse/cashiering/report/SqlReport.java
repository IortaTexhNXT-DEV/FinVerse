package com.iortatechnxt.brokerverse.cashiering.report;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * A Cashiering report (CSHID.017/018/023) defined by constant SQL over the cashiering tables and
 * the Operations ledger: company and period parameters, columns, optional grouping and footnote.
 * Viewed with {@code OPS_REPORT_VIEW}, exported with {@code OPS_REPORT_EXPORT} and archived ({@code
 * ReportMetadata.operations}). Layouts not given in Annex II carry the footnote "Draft layout"
 * (OQ42).
 */
public class SqlReport implements ReportDefinition {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** Period start. */
  static final String FROM = "from";

  /** Period end. */
  static final String TO = "to";

  /** Footnote of the draft layouts. */
  static final String DRAFT =
      "Draft layout: fields to be confirmed by BDOI (OQ42). Report of the Cashiering module.";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

  private final Spec spec;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates a report.
   *
   * @param spec code, title, SQL, columns, grouping and note
   * @param jdbc JDBC
   */
  public SqlReport(Spec spec, NamedParameterJdbcTemplate jdbc) {
    this.spec = spec;
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
    params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    return ReportMetadata.operations(spec.code(), spec.title(), spec.description(), params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("company", p.longValue(COMPANY))
            .addValue(FROM, p.optionalDate(FROM).orElse(EARLIEST))
            .addValue(TO, p.optionalDate(TO).orElse(LATEST));
    List<Map<String, Object>> rows =
        jdbc.queryForList(spec.sql(), args).stream().map(SqlReport::normalise).toList();
    TabularReportBuilder builder = TabularReportBuilder.of(p).columns(spec.columns());
    if (spec.groupKey() != null) {
      builder.groupBy(spec.groupKey(), spec.groupLabel());
    }
    if (spec.note() != null) {
      builder.note(spec.note());
    }
    return builder.rows(rows).presorted().build();
  }

  private static Map<String, Object> normalise(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach(
        (k, v) ->
            out.put(
                k,
                switch (v) {
                  case Date d -> d.toLocalDate();
                  case Timestamp t -> t.toInstant().atZone(MANILA).toLocalDate();
                  case null, default -> v;
                }));
    return out;
  }

  /**
   * What a report shows.
   *
   * @param code report code
   * @param title title
   * @param description one-line purpose citing the BR ID
   * @param sql constant SQL with :company, :from and :to
   * @param columns columns
   * @param groupKey grouping column, null for none
   * @param groupLabel grouping label
   * @param note footnote, null for none
   */
  public record Spec(
      String code,
      String title,
      String description,
      String sql,
      List<ReportColumn> columns,
      String groupKey,
      String groupLabel,
      String note) {

    /** Defensive copy. */
    public Spec {
      columns = List.copyOf(columns);
    }
  }
}
