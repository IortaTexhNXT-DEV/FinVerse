package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * A Renewal report read with one SQL statement over the renewal accounts {@code c} (FRRN.044.01):
 * the expiry range, the criteria and the user's data scope apply; it is previewed, printed and
 * exported to Excel, PDF and CSV like every report, its runs audited, and its file named {@code
 * <Report Name>_<MMDDYYYY>}.
 */
public class SqlRenewalReport implements ReportDefinition {

  private static final DateTimeFormatter NAME_DATE =
      DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT);

  private final Spec spec;
  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param spec code, title, statement and columns
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public SqlRenewalReport(Spec spec, NbReportJdbc jdbc, RenewalReportSupport support) {
    this.spec = spec;
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(spec.code(), spec.title(), spec.description());
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc
            .rows(spec.select() + RenewalReportSupport.FILTERS + spec.tail(), support.args(p).map())
            .stream()
            .map(RenewalReportSupport::labels)
            .toList();
    return TabularReportBuilder.of(p).columns(spec.columns()).rows(rows).presorted().build();
  }

  @Override
  public String exportName(LocalDate extractionDate) {
    return spec.title() + "_" + NAME_DATE.format(extractionDate);
  }

  /**
   * A report.
   *
   * @param code report code
   * @param title report name
   * @param description purpose
   * @param select statement up to the filters on {@code c} (ending with a where clause)
   * @param tail grouping and order after the filters
   * @param columns columns
   */
  public record Spec(
      String code,
      String title,
      String description,
      String select,
      String tail,
      List<ReportColumn> columns) {

    /** Defensive copy. */
    public Spec {
      columns = List.copyOf(columns);
    }
  }
}
