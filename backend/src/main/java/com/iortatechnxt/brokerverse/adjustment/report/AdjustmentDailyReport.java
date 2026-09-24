package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Adjustment and Daily Endorsement Report (ADJ-DAILY, ADJID.016): every endorsement request raised
 * or posted in the period (by default today), filterable by endorsement type and user; run daily by
 * the {@code ADJ_DAILY_REPORT} job and archived. Draft layout (OQ42).
 */
@Component
public class AdjustmentDailyReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ADJ-DAILY";

  private final AdjustmentReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   */
  public AdjustmentDailyReport(AdjustmentReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.operations(
        CODE,
        "Adjustment and Daily Endorsement Report",
        "Endorsement requests raised or posted in the period, by type and user (ADJID.016;"
            + " layout to confirm, OQ42)",
        AdjustmentReportSupport.periodTypeUser("TODAY"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Predicate<EndorsementRequest> filter = AdjustmentReportSupport.typeAndUser(p);
    Map<Long, EndorsementRequest> rows = new LinkedHashMap<>();
    support.created(p, filter).forEach(r -> rows.put(r.getId(), r));
    support.posted(p).stream().filter(filter).forEach(r -> rows.putIfAbsent(r.getId(), r));
    List<Map<String, Object>> data =
        rows.values().stream().map(AdjustmentDailyReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.date("date", "Raised"),
            ReportColumn.text("class", "Class"),
            ReportColumn.text("type", "Endorsement Type"),
            ReportColumn.text("requestType", "Request Type"),
            ReportColumn.text("invoice", "Invoice No."),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.amount("premium", "Premium Change"),
            ReportColumn.amount("commission", "Commission Change"),
            ReportColumn.text("requestedBy", "Requested By"),
            ReportColumn.text("postedBy", "Posted By"),
            ReportColumn.date("posted", "Posted"))
        .rows(data)
        .presorted()
        .build();
  }

  private static Map<String, Object> row(EndorsementRequest r) {
    Map<String, Object> m = AdjustmentReportSupport.requestRow(r);
    m.put("requestedBy", r.getCreatedBy());
    m.put("postedBy", DocText.text(r.trail().postedBy()));
    m.put("posted", AdjustmentReportSupport.day(r.trail().postedAt()));
    return m;
  }
}
