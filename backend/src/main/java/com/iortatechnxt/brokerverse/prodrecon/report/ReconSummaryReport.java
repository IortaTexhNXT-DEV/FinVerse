package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Production Reconciliation Summary Report (PRC-SUMMARY, Annex IV #1, PRCID.035): per insurer, the
 * items and amounts matched, matched with discrepancies and unmatched, with the stages of the
 * insurer's cycles as remarks.
 */
@Component
public class ReconSummaryReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PRC-SUMMARY";

  private static final String SQL =
      "with x as (select c.insurer_code as insurer, i.status, c.stage, "
          + ReconReportSupport.AMOUNT
          + " as amount"
          + ReconReportSupport.ITEMS_OF_PERIOD
          + ") select insurer,"
          + " count(*) filter (where status = 'MATCHED') as matched_items,"
          + " coalesce(sum(amount) filter (where status = 'MATCHED'), 0) as matched_amount,"
          + " count(*) filter (where status = 'MATCHED_WITH_DISCREPANCY') as discrepancy_items,"
          + " coalesce(sum(amount) filter (where status = 'MATCHED_WITH_DISCREPANCY'), 0)"
          + " as discrepancy_amount,"
          + " count(*) filter (where status not like 'MATCHED%') as unmatched_items,"
          + " coalesce(sum(amount) filter (where status not like 'MATCHED%'), 0)"
          + " as unmatched_amount,"
          + " string_agg(distinct stage, ', ') as remarks"
          + " from x group by insurer order by insurer";

  private final ReconReportSupport support;

  /**
   * Creates the report.
   *
   * @param support report SQL
   */
  public ReconSummaryReport(ReconReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return ReconReportSupport.metadata(
        CODE,
        "Production Reconciliation Summary",
        "Matched, discrepant and unmatched items and amounts per insurer (PRCID.035)");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("insurer", "Insurance Company"),
            ReportColumn.count("matched_items", "Matched Items"),
            ReportColumn.amount("matched_amount", "Matched Amount"),
            ReportColumn.count("discrepancy_items", "With Discrepancies Items"),
            ReportColumn.amount("discrepancy_amount", "With Discrepancies Amount"),
            ReportColumn.count("unmatched_items", "Unmatched Items"),
            ReportColumn.amount("unmatched_amount", "Unmatched Amount"),
            ReportColumn.text("remarks", "Remarks"))
        .rows(support.rows(SQL, ReconReportSupport.args(p)))
        .presorted()
        .build();
  }
}
