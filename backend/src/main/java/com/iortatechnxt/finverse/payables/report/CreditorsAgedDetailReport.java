package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ColumnType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.subledger.service.AgeingSlots;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-AP-AGE-DET – Creditors Aged Analysis – Detailed (Src FAP009): every open document of each
 * creditor with due date, days pending, O/S amount split into On Account or an age bucket, with Sub
 * Account totals and a report total.
 */
@Component
public class CreditorsAgedDetailReport implements ReportDefinition {

  private static final String SUB_ACCOUNT = "subAccount";

  private final CreditorReportSupport support;

  /**
   * Creates the report.
   *
   * @param support creditor engine
   */
  public CreditorsAgedDetailReport(CreditorReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-AP-AGE-DET",
        "Creditors Aged Analysis - Detailed",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Open creditor documents with due dates, days pending and ageing (FAP009)",
        CreditorReportSupport.parameters(true),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AgeingSlots slots = support.slots(p);
    boolean base = CreditorReportSupport.base(p);
    String baseCurrency = support.baseCurrency(p);
    boolean byDue = CreditorReportSupport.byDueDate(p);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (CreditorItem item : support.items(p)) {
      String currency = base ? baseCurrency : item.currency();
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(
          SUB_ACCOUNT,
          item.mainAccount()
              + " / "
              + item.partyCode()
              + " "
              + item.partyName()
              + " ("
              + currency
              + ")");
      row.put("document", item.documentNo());
      row.put("documentDate", item.documentDate());
      row.put("dueDate", item.dueDate());
      row.put("reference", item.narration());
      row.put("outstanding", item.signed(base));
      CreditorReportSupport.accumulate(row, item, slots, p);
      row.remove(CreditorReportSupport.NET);
      row.put("days", item.age(p.date(GlReportSupport.AS_OF), byDue));
      rows.add(row);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("document", "Document"));
    columns.add(ReportColumn.date("documentDate", "Doc Date"));
    columns.add(ReportColumn.date("dueDate", "Due Date"));
    columns.add(ReportColumn.text("reference", "Reference"));
    columns.add(ReportColumn.amount("outstanding", "O/S Amount"));
    columns.add(ReportColumn.amount(CreditorReportSupport.ON_ACCOUNT, "On Account"));
    columns.addAll(CreditorReportSupport.bucketColumns(slots));
    columns.add(new ReportColumn("days", "Days Pending", ColumnType.NUMBER, false));
    TabularReportBuilder builder =
        TabularReportBuilder.of(p).columns(columns).groupBy(SUB_ACCOUNT, "Sub Account").presorted();
    if (!base) {
      builder.withoutGrandTotal();
    }
    return builder.rows(rows).note(CreditorReportSupport.note(p, slots)).build();
  }
}
