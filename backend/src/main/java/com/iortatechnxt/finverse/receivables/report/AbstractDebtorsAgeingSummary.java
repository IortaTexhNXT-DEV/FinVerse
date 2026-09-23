package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.report.ReceivablesReportSupport.AgedItem;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ArItem;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debtors aged analysis summary engine (Src FAP008): one line per debtor (and currency in foreign
 * currency mode) with the aged buckets, On Account and Net Value = buckets + On Account. Grouped by
 * ledger, and optionally by division (branch) first.
 */
public abstract class AbstractDebtorsAgeingSummary implements ReportDefinition {

  private static final String NET = "net";
  private static final String ON_ACCOUNT = "onAccount";
  private static final String DIVISION = "division";
  private static final String LEDGER = "ledger";

  private final ReceivablesQueries queries;
  private final String code;
  private final String title;
  private final boolean byDivision;

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param code report code
   * @param title title
   * @param byDivision group by division (branch) first
   */
  protected AbstractDebtorsAgeingSummary(
      ReceivablesQueries queries, String code, String title, boolean byDivision) {
    this.queries = queries;
    this.code = code;
    this.title = title;
    this.byDivision = byDivision;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.RECEIVABLES_PAYABLES,
        "Net balance per debtor aged into day slots" + (byDivision ? ", by division" : ""),
        ReceivablesReportSupport.ageingReportParams(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AgeingSlots slots = ReceivablesReportSupport.slots(p);
    boolean foreign = ReceivablesReportSupport.foreign(p);
    List<ArItem> items = ReceivablesReportSupport.selectedItems(queries, p);
    Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
    for (AgedItem a : ReceivablesReportSupport.age(items, p.date(GlReportSupport.AS_OF), p)) {
      ArItem i = a.item();
      String currency = foreign ? i.currency() : "BASE";
      String key = (byDivision ? i.branchCode() : "") + "|" + i.partyCode() + "|" + currency;
      Map<String, Object> row =
          byKey.computeIfAbsent(key, k -> newRow(i, foreign ? i.currency() : ""));
      add(row, a.onAccount() ? ON_ACCOUNT : ReceivablesReportSupport.bucketKey(a.bucket()), a);
      add(row, NET, a);
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("party", "Sub A/c"));
    columns.add(ReportColumn.text("name", "Account Name"));
    columns.add(ReportColumn.text("currency", "Ccy"));
    columns.addAll(ReceivablesReportSupport.bucketColumns(slots));
    columns.add(ReportColumn.amount(ON_ACCOUNT, "On A/c"));
    columns.add(ReportColumn.amount(NET, "Net Value"));
    TabularReportBuilder builder = TabularReportBuilder.of(p).columns(columns);
    if (byDivision) {
      builder.groupBy(DIVISION, "Division");
    }
    return builder
        .groupBy(LEDGER, "Main A/c")
        .rows(new ArrayList<>(byKey.values()))
        .note(ReceivablesReportSupport.ageingNote(p))
        .build();
  }

  private static Map<String, Object> newRow(ArItem i, String currency) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(DIVISION, i.branchCode());
    row.put(LEDGER, ReceivablesReportSupport.ledgerOf(i));
    row.put("party", i.partyCode());
    row.put("name", i.partyName());
    row.put("currency", currency);
    return row;
  }

  private static void add(Map<String, Object> row, String key, AgedItem a) {
    row.merge(key, a.amount(), (x, y) -> ((BigDecimal) x).add((BigDecimal) y));
  }
}
