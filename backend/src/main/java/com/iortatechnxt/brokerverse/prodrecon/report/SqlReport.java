package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;

/**
 * A production reconciliation report made of one SQL statement over the reconciliation tables,
 * grouped by one of its columns (the insurer, or the disposition).
 */
final class SqlReport implements ReportDefinition {

  private final ReconReportSupport support;
  private final ReportMetadata metadata;
  private final String sql;
  private final Group group;
  private final List<ReportColumn> columns;

  /**
   * Creates the report.
   *
   * @param support report SQL
   * @param metadata metadata
   * @param sql constant SQL with the parameters of {@link ReconReportSupport#args}
   * @param group grouping column
   * @param columns other columns
   */
  SqlReport(
      ReconReportSupport support,
      ReportMetadata metadata,
      String sql,
      Group group,
      List<ReportColumn> columns) {
    this.support = support;
    this.metadata = metadata;
    this.sql = sql;
    this.group = group;
    this.columns = List.copyOf(columns);
  }

  @Override
  public ReportMetadata metadata() {
    return metadata;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(group.key(), group.label())
        .rows(support.rows(sql, ReconReportSupport.args(p)))
        .presorted()
        .build();
  }

  /**
   * The grouping of a report.
   *
   * @param key column key
   * @param label group label
   */
  record Group(String key, String label) {

    /** Grouped by insurer. */
    static final Group INSURER = new Group("insurer", "Insurance Company");
  }
}
