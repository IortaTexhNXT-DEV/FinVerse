package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.brokerverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService.Brs;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine of the un-reconciled entries reports (Src BR001 / BR002): items of one or all bank
 * accounts not reconciled as of a date, with the days outstanding, grouped by bank.
 */
public abstract class AbstractUnreconciledReport implements ReportDefinition {

  private static final String DETAIL = "detail";

  private final BankReconciliationService reconciliation;
  private final BankAccountDirectory banks;
  private final String code;
  private final String title;

  /**
   * Creates the report.
   *
   * @param reconciliation reconciliation service
   * @param banks bank account directory
   * @param code report code
   * @param title title
   */
  protected AbstractUnreconciledReport(
      BankReconciliationService reconciliation,
      BankAccountDirectory banks,
      String code,
      String title) {
    this.reconciliation = reconciliation;
    this.banks = banks;
    this.code = code;
    this.title = title;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.RECONCILIATION,
        "Un-reconciled items with the days outstanding, per bank account",
        List.of(
            GlReportSupport.companyParam(),
            ParameterSpec.optional(
                BankReconciliationStatementReport.BANK, "Bank Account", ParameterType.ACCOUNT),
            GlReportSupport.asOfParam(),
            ParameterSpec.optional(DETAIL, "Detail Required", ParameterType.BOOLEAN)
                .withDefault("true")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    List<String> codes =
        p.optionalText(BankReconciliationStatementReport.BANK)
            .map(List::of)
            .orElseGet(() -> banks.list(companyId).stream().map(BankAccount::code).toList());
    List<Map<String, Object>> rows = new ArrayList<>();
    for (String bank : codes) {
      Brs brs = reconciliation.statement(companyId, bank, asOf);
      String group = brs.bank().code() + " - " + brs.bank().name();
      for (Item item : items(brs)) {
        rows.add(cells(group, item, asOf));
      }
    }
    ReportResult full =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.date("date", "Doc Date"),
                ReportColumn.text("narration", "Doc Narration"),
                ReportColumn.text("docNo", "Doc No."),
                ReportColumn.text("reference", "Doc Reference"),
                ReportColumn.amount("debit", "Debit"),
                ReportColumn.amount("credit", "Credit"),
                ReportColumn.amount("original", "Original Amount Dr/Cr"),
                ReportColumn.text("currency", "Currency"),
                new ReportColumn("days", "Days", ColumnType.NUMBER, false))
            .groupBy("bank", "Bank")
            .presorted()
            .rows(rows)
            .build();
    if (p.flag(DETAIL)) {
      return full;
    }
    List<ReportRow> summary = full.rows().stream().filter(r -> r.kind() != RowKind.DETAIL).toList();
    return new ReportResult(
        full.code(), full.title(), full.parameterEcho(), full.columns(), summary, full.notes());
  }

  /**
   * The un-reconciled items of one bank account.
   *
   * @param brs reconciliation statement of the bank account
   * @return items
   */
  protected abstract List<Item> items(Brs brs);

  private static Map<String, Object> cells(String group, Item item, LocalDate asOf) {
    Map<String, Object> cells = new LinkedHashMap<>();
    cells.put("bank", group);
    cells.put("date", item.date());
    if (item.narration() != null) {
      cells.put("narration", item.narration());
    }
    cells.put("docNo", item.docNo());
    if (item.reference() != null) {
      cells.put("reference", item.reference());
    }
    cells.put("debit", item.debit());
    cells.put("credit", item.credit());
    cells.put("original", item.debit().subtract(item.credit()));
    cells.put("currency", item.currency());
    cells.put("days", ChronoUnit.DAYS.between(item.date(), asOf));
    return cells;
  }

  /**
   * Un-reconciled item.
   *
   * @param date document / value date
   * @param narration narration or description
   * @param docNo document number
   * @param reference reference
   * @param debit debit amount
   * @param credit credit amount
   * @param currency currency
   */
  protected record Item(
      LocalDate date,
      String narration,
      String docNo,
      String reference,
      BigDecimal debit,
      BigDecimal credit,
      String currency) {}
}
