package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.RequestAging;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Transaction aging (ADJ-AGING, ADJID.021): endorsement requests of the period with the days from
 * request (submission) to completion, or to today while open, and their aging bucket. Draft layout
 * (OQ42).
 */
@Component
public class AdjustmentAgingReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ADJ-AGING";

  private static final long WEEK = 7;
  private static final long MONTH = 30;

  private final AdjustmentReportSupport support;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   * @param clock clock
   */
  public AdjustmentAgingReport(AdjustmentReportSupport support, Clock clock) {
    this.support = support;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.operations(
        CODE,
        "Adjustment Transaction Aging",
        "Days from request to completion of endorsement requests (ADJID.021; layout to confirm,"
            + " OQ42)",
        AdjustmentReportSupport.period("MONTH_START"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Instant now = clock.instant();
    List<Map<String, Object>> rows =
        support.created(p, r -> true).stream().map(r -> row(r, now)).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.text("invoice", "Invoice No."),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.text("type", "Endorsement Type"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.date("date", "Raised"),
            ReportColumn.date("submitted", "Submitted"),
            ReportColumn.date("completed", "Completed"),
            ReportColumn.count("days", "Aging (days)"),
            ReportColumn.text("bucket", "Bucket"))
        .rows(rows)
        .presorted()
        .build();
  }

  private static Map<String, Object> row(EndorsementRequest r, Instant now) {
    long days = RequestAging.days(r, now);
    Map<String, Object> m = AdjustmentReportSupport.requestRow(r);
    m.put("submitted", AdjustmentReportSupport.day(r.trail().submittedAt()));
    m.put("completed", AdjustmentReportSupport.day(r.trail().completedAt()));
    m.put("days", days);
    m.put("bucket", bucket(days));
    return m;
  }

  private static String bucket(long days) {
    if (days <= 1) {
      return "0-1 day";
    }
    if (days <= WEEK) {
      return "2-7 days";
    }
    return days <= MONTH ? "8-30 days" : "Over 30 days";
  }
}
