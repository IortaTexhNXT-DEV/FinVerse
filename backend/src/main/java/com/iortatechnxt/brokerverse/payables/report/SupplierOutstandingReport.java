package com.iortatechnxt.brokerverse.payables.report;

import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * FIN-AP-SUPOS – Supplier Outstanding Summary (Src FR2526): net outstanding per main account and
 * supplier with phone number, On A/c and age buckets (base currency).
 */
@Component
public class SupplierOutstandingReport implements ReportDefinition {

  private static final String MAIN = "mainAccount";

  private final CreditorReportSupport support;
  private final ChartOfAccountsService chart;

  /**
   * Creates the report.
   *
   * @param support creditor engine
   * @param chart chart of accounts
   */
  public SupplierOutstandingReport(CreditorReportSupport support, ChartOfAccountsService chart) {
    this.support = support;
    this.chart = chart;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = CreditorReportSupport.parameters(false);
    return new ReportMetadata(
        "FIN-AP-SUPOS",
        "Supplier Outstanding Summary",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Net outstanding per supplier with On A/c and age buckets (FR2526)",
        params,
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AgeingSlots slots = support.slots(p);
    Map<String, String> names =
        chart.list(p.longValue(GlReportSupport.COMPANY)).stream()
            .collect(Collectors.toMap(GlAccount::getCode, GlAccount::getName, (a, b) -> a));
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    for (CreditorItem item : support.items(p)) {
      Map<String, Object> row =
          rows.computeIfAbsent(
              item.mainAccount() + "|" + item.partyCode(),
              k -> newRow(item, names.getOrDefault(item.mainAccount(), "")));
      CreditorReportSupport.accumulate(row, item, slots, p);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("partyCode", "Supplier Code"));
    columns.add(ReportColumn.text("partyName", "Supplier Name"));
    columns.add(ReportColumn.text("phone", "Phone No."));
    columns.add(ReportColumn.amount(CreditorReportSupport.NET, "Amount"));
    columns.add(ReportColumn.amount(CreditorReportSupport.ON_ACCOUNT, "On A/c"));
    columns.addAll(CreditorReportSupport.bucketColumns(slots));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(MAIN, "Main Account")
        .rows(new ArrayList<>(rows.values()))
        .note(CreditorReportSupport.note(p, slots))
        .build();
  }

  private static Map<String, Object> newRow(CreditorItem item, String accountName) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(MAIN, item.mainAccount() + " " + accountName);
    row.put("partyCode", item.partyCode());
    row.put("partyName", item.partyName());
    row.put("phone", item.phone());
    return row;
  }
}
