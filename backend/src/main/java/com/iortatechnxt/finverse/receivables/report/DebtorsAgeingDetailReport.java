package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.report.ReceivablesReportSupport.AgedItem;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ArItem;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-AR-AGE-DET (Src FAP009) Debtors Aged Analysis - Detailed: every outstanding item of each
 * debtor with its age bucket; unapplied receipts and credits in On Account.
 */
@Component
public class DebtorsAgeingDetailReport implements ReportDefinition {

  private static final String CODE = "FIN-AR-AGE-DET";

  private final ReceivablesQueries queries;

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   */
  public DebtorsAgeingDetailReport(ReceivablesQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "Debtors Aged Analysis - Detailed",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Outstanding debit notes per debtor aged into day slots, with money on account",
        ReceivablesReportSupport.ageingReportParams(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    AgeingSlots slots = ReceivablesReportSupport.slots(p);
    List<ArItem> items = ReceivablesReportSupport.selectedItems(queries, p);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (AgedItem a : ReceivablesReportSupport.age(items, asOf, p)) {
      ArItem i = a.item();
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("party", ReceivablesReportSupport.partyLabel(i));
      row.put("tranCode", i.documentType() + "-" + i.documentNo());
      row.put("documentDate", i.documentDate());
      row.put("dueDate", i.dueDate());
      row.put("reference", i.narration());
      row.put("currency", i.currency());
      row.put("days", a.days());
      row.put("outstanding", a.amount());
      if (a.onAccount()) {
        row.put("onAccount", a.amount());
      } else {
        row.put(ReceivablesReportSupport.bucketKey(a.bucket()), a.amount());
      }
      rows.add(row);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("tranCode", "Tran Code"));
    columns.add(ReportColumn.date("documentDate", "Document Date"));
    columns.add(ReportColumn.date("dueDate", "Due Date"));
    columns.add(ReportColumn.text("reference", "Reference"));
    columns.add(ReportColumn.text("currency", "Ccy"));
    columns.add(new ReportColumn("days", "Days", ColumnType.NUMBER, false));
    columns.add(ReportColumn.amount("outstanding", "O/S Amount"));
    columns.add(ReportColumn.amount("onAccount", "On Account"));
    columns.addAll(ReceivablesReportSupport.bucketColumns(slots));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy("party", "Sub Account")
        .presorted()
        .rows(rows)
        .note(ReceivablesReportSupport.ageingNote(p))
        .build();
  }
}
