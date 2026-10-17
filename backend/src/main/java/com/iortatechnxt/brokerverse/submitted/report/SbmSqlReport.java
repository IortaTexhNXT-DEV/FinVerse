package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;

/**
 * A Submitted Policies report read by one SQL statement: its metadata, SQL and columns; the rows
 * are scoped and labelled by {@link SbmReportSupport}.
 */
public abstract class SbmSqlReport implements ReportDefinition {

  private final SbmReportSupport support;
  private final Spec spec;

  /**
   * The definition of a report.
   *
   * @param code code
   * @param title title
   * @param description purpose
   * @param dateLabel what the date range filters
   * @param sql SQL
   * @param columns columns
   */
  public record Spec(
      String code,
      String title,
      String description,
      String dateLabel,
      String sql,
      List<ReportColumn> columns) {

    /** Defensive copy. */
    public Spec {
      columns = List.copyOf(columns);
    }
  }

  /**
   * Creates the report.
   *
   * @param support scope and labels
   * @param spec definition
   */
  protected SbmSqlReport(SbmReportSupport support, Spec spec) {
    this.support = support;
    this.spec = spec;
  }

  @Override
  public ReportMetadata metadata() {
    return SbmReportSupport.metadata(
        spec.code(), spec.title(), spec.description(), spec.dateLabel());
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(spec.columns())
        .rows(support.rows(spec.sql(), p))
        .presorted()
        .build();
  }
}
