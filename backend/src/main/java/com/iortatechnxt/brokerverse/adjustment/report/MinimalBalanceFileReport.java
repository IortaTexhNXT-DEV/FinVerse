package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItemRepository;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Minimal balance write-off summary (ADJ-MINBAL-FILE, ADJID.026): the balances written off or
 * credited in the period by file, with their journal. Draft layout (OQ42).
 */
@Component
public class MinimalBalanceFileReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ADJ-MINBAL-FILE";

  private final MinBalanceItemRepository items;

  /**
   * Creates the report.
   *
   * @param items write-offs
   */
  public MinimalBalanceFileReport(MinBalanceItemRepository items) {
    this.items = items;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.operations(
        CODE,
        "Minimal Balance Write-off Summary",
        "Balances of 10.00 to 100.00 written off or credited by the minimal balance file"
            + " (ADJID.026; layout to confirm, OQ42)",
        AdjustmentReportSupport.period("MONTH_START"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        items
            .createdBetween(
                p.longValue(AdjustmentReportSupport.COMPANY),
                AdjustmentReportSupport.start(p),
                AdjustmentReportSupport.end(p))
            .stream()
            .map(MinimalBalanceFileReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("file", "File / Request"),
            ReportColumn.date("date", "Date"),
            ReportColumn.text("invoice", "Invoice No."),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("client", "Client"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amount("balance", "Balance"),
            ReportColumn.text("action", "Action"),
            ReportColumn.text("journal", "Journal"),
            ReportColumn.text("by", "Processed By"))
        .groupBy("file", "File")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> row(MinBalanceItem i) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("file", i.getFileRef());
    m.put("date", AdjustmentReportSupport.day(i.getCreatedAt()));
    m.put("invoice", i.getInvoiceNo());
    m.put("arn", i.getArn());
    m.put("client", i.getClientCode());
    m.put("currency", i.getCurrency());
    m.put("balance", i.getBalance());
    m.put("action", i.getAction().name());
    m.put("journal", i.getJournalBatchNo());
    m.put("by", i.getCreatedBy());
    return m;
  }
}
