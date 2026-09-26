package com.iortatechnxt.brokerverse.frbs.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * A report of the BDOI report pack defined by one constant query (FRBS 3.2.0, Appendix A IV-VI;
 * design section 10): category BDOI Report Pack (view {@code FRBS_REPORT_VIEW}, export {@code
 * FRBS_REPORT_EXPORT} in Excel, PDF, ODS or CSV, archived). The query binds {@code :companyId},
 * {@code :from} and {@code :to}, and the derived dates {@code :monthStart}, {@code :yearStart},
 * {@code :prevYearStart} and {@code :prevTo} (the same date a year earlier). Layouts BDOI has not
 * detailed are drafts to confirm (AQ05).
 */
public final class FrbsSqlReport implements ReportDefinition {

  static final String COMPANY = "companyId";
  static final String FROM = "from";
  static final String TO = "to";
  static final String YEAR = "year";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final ReportMetadata metadata;
  private final Spec spec;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param spec definition
   * @param jdbc JDBC
   */
  FrbsSqlReport(Spec spec, NamedParameterJdbcTemplate jdbc) {
    this(spec, jdbc, false);
  }

  private FrbsSqlReport(Spec spec, NamedParameterJdbcTemplate jdbc, boolean document) {
    this.spec = spec;
    this.jdbc = jdbc;
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    switch (spec.dates()) {
      case PERIOD -> {
        params.add(
            ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
        params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
      }
      case AS_OF ->
          params.add(ParameterSpec.required(TO, "As of", ParameterType.DATE).withDefault("TODAY"));
      default ->
          params.add(
              ParameterSpec.required(YEAR, "Year", ParameterType.NUMBER)
                  .withDefault(String.valueOf(LocalDate.now(MANILA).getYear())));
    }
    ReportMetadata m = ReportMetadata.frbs(spec.code(), spec.title(), spec.description(), params);
    this.metadata = document ? m.asDocument() : m;
  }

  /**
   * The same report as a board document, also exported to Word (A1-FRBS {@code word_requested},
   * client requirement 16).
   *
   * @return report
   */
  FrbsSqlReport asDocument() {
    return new FrbsSqlReport(spec, jdbc, true);
  }

  @Override
  public ReportMetadata metadata() {
    return metadata;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate to;
    LocalDate from;
    if (spec.dates() == Dates.YEAR) {
      int year = Integer.parseInt(p.text(YEAR));
      from = Year.of(year).atDay(1);
      to = from.plusYears(1).minusDays(1);
    } else {
      to = p.date(TO);
      from = p.optionalDate(FROM).orElse(to.withDayOfMonth(1));
    }
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, Date.valueOf(from));
    args.put(TO, Date.valueOf(to));
    args.put("monthStart", Date.valueOf(to.withDayOfMonth(1)));
    args.put("yearStart", Date.valueOf(to.withDayOfYear(1)));
    args.put("prevYearStart", Date.valueOf(to.minusYears(1).withDayOfYear(1)));
    args.put("prevTo", Date.valueOf(to.minusYears(1)));
    TabularReportBuilder builder =
        TabularReportBuilder.of(p).columns(spec.columns()).rows(rows(args)).presorted();
    if (spec.groupKey() != null) {
      builder.groupBy(spec.groupKey(), spec.groupLabel());
    }
    spec.notes().forEach(builder::note);
    return builder.build();
  }

  private List<Map<String, Object>> rows(Map<String, Object> args) {
    return jdbc.query(
        spec.sql(),
        args,
        (rs, i) -> {
          ResultSetMetaData meta = rs.getMetaData();
          Map<String, Object> out = new LinkedHashMap<>();
          for (int c = 1; c <= meta.getColumnCount(); c++) {
            out.put(meta.getColumnLabel(c), toLocal(rs.getObject(c)));
          }
          return out;
        });
  }

  private static Object toLocal(Object value) {
    return switch (value) {
      case Timestamp t -> t.toInstant().atZone(MANILA).toLocalDate();
      case Date d -> d.toLocalDate();
      case null, default -> value;
    };
  }

  /** The dates a report takes. */
  enum Dates {
    /** From and to. */
    PERIOD,
    /** One as-of date (month, year to date and previous year are derived). */
    AS_OF,
    /** A calendar year. */
    YEAR
  }

  /**
   * The definition of a report.
   *
   * @param code report code
   * @param title title
   * @param description purpose with its BR ID
   * @param dates the dates it takes
   * @param sql constant query
   * @param columns columns
   * @param groupKey column to group by, null for none
   * @param groupLabel label of the group
   * @param notes notes printed under the report
   */
  record Spec(
      String code,
      String title,
      String description,
      Dates dates,
      String sql,
      List<ReportColumn> columns,
      String groupKey,
      String groupLabel,
      List<String> notes) {}
}
