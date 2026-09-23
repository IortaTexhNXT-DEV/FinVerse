package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-AP-AGE-SUM – Creditors Aged Analysis – Summary (Src FAP008): one line per main account, sub
 * account (party) and currency with ageing buckets, On A/c and Net value.
 */
@Component
public class CreditorsAgedSummaryReport implements ReportDefinition {

  private static final String MAIN = "mainAccount";
  private static final String CURRENCY = "currency";

  private final CreditorReportSupport support;

  /**
   * Creates the report.
   *
   * @param support creditor engine
   */
  public CreditorsAgedSummaryReport(CreditorReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-AP-AGE-SUM",
        "Creditors Aged Analysis - Summary",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Creditor ageing by main account and supplier as of a date, with On A/c and Net (FAP008)",
        CreditorReportSupport.parameters(true),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AgeingSlots slots = AgeingSlots.from(p, AgeingSlots.DEFAULT);
    boolean base = CreditorReportSupport.base(p);
    String baseCurrency = support.baseCurrency(p);
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    for (CreditorItem item : support.items(p)) {
      String currency = base ? baseCurrency : item.currency();
      Map<String, Object> row =
          rows.computeIfAbsent(
              item.mainAccount() + "|" + item.partyCode() + "|" + currency,
              k -> newRow(item, currency));
      CreditorReportSupport.accumulate(row, item, slots, p);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("partyCode", "Sub A/c"));
    columns.add(ReportColumn.text("partyName", "Account Name"));
    columns.add(ReportColumn.text(CURRENCY, "Currency"));
    columns.addAll(slots.columns());
    columns.add(ReportColumn.amount(CreditorReportSupport.ON_ACCOUNT, "On A/c"));
    columns.add(ReportColumn.amount(CreditorReportSupport.NET, "Net Value"));
    TabularReportBuilder builder = TabularReportBuilder.of(p).columns(columns);
    if (!base) {
      builder.groupBy(CURRENCY, "Currency").withoutGrandTotal();
    }
    return builder
        .groupBy(MAIN, "Main A/c")
        .rows(new ArrayList<>(rows.values()))
        .note(CreditorReportSupport.note(p, slots))
        .build();
  }

  private static Map<String, Object> newRow(CreditorItem item, String currency) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(MAIN, item.mainAccount());
    row.put("partyCode", item.partyCode());
    row.put("partyName", item.partyName());
    row.put(CURRENCY, currency);
    return row;
  }
}
