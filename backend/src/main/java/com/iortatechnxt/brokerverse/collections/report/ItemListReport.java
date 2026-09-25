package com.iortatechnxt.brokerverse.collections.report;

import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;

/**
 * A Collections report listing collection items with the item columns (p.59-61): the subclasses
 * give the selection (condition and order) and whether a period applies.
 */
public abstract class ItemListReport implements ReportDefinition {

  private final ClxReportSql sql;
  private final ReportMetadata metadata;
  private final String query;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   * @param metadata metadata
   * @param condition extra SQL condition (starting with {@code and}) and order
   */
  protected ItemListReport(ClxReportSql sql, ReportMetadata metadata, String condition) {
    this.sql = sql;
    this.metadata = metadata;
    this.query =
        ClxReportSql.ITEM_SELECT + ClxReportSql.ITEM_FROM + ClxReportSql.ITEM_FILTERS + condition;
  }

  @Override
  public ReportMetadata metadata() {
    return metadata;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(ClxReportSql.itemColumns())
        .rows(sql.rows(query, ClxReportSql.args(p)))
        .presorted()
        .note(ClxReportSql.DRAFT_NOTE)
        .build();
  }
}
