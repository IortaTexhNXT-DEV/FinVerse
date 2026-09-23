package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.finverse.receivables.domain.BrsFigures;
import com.iortatechnxt.finverse.receivables.service.BankBookQueries.BookEntry;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService.Brs;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * FIN-BRS-STMT (Src BR003) Bank Reconciliation Statement: book balance, the four groups of
 * reconciling items, the balance per bank derived from the book, the balance per imported statement
 * and the unexplained difference.
 */
@Component
public class BankReconciliationStatementReport implements ReportDefinition {

  /** Bank account parameter. */
  static final String BANK = "bankAccount";

  private static final String AMOUNT = "amount";

  private final BankReconciliationService reconciliation;

  /**
   * Creates the report.
   *
   * @param reconciliation reconciliation service
   */
  public BankReconciliationStatementReport(BankReconciliationService reconciliation) {
    this.reconciliation = reconciliation;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-BRS-STMT",
        "Bank Reconciliation Statement",
        ReportCategory.RECONCILIATION,
        "Book balance reconciled to the bank statement balance with the reconciling items",
        List.of(
            GlReportSupport.companyParam(),
            ParameterSpec.required(BANK, "Bank Account", ParameterType.ACCOUNT),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Brs brs =
        reconciliation.statement(
            p.longValue(GlReportSupport.COMPANY), p.text(BANK), p.date(GlReportSupport.AS_OF));
    BrsFigures f = brs.figures();
    List<ReportRow> rows = new ArrayList<>();
    rows.add(
        new ReportRow(
            RowKind.SECTION,
            0,
            "Bank "
                + brs.bank().code()
                + " - "
                + brs.bank().name()
                + " ("
                + brs.bank().currency()
                + ")",
            Map.of()));
    rows.add(total("Balance as per Book", f.bookBalance()));
    book(
        rows,
        "1. Book Debit Entries not accounted by Bank (less)",
        brs.bookDebits(),
        BookEntry::debit);
    book(
        rows,
        "2. Book Credit Entries not accounted by Bank (add)",
        brs.bookCredits(),
        BookEntry::credit);
    bank(
        rows,
        "3. Bank Debit Entries not accounted in Book (less)",
        brs.bankDebits(),
        BankStatementLine::getDebit);
    bank(
        rows,
        "4. Bank Credit Entries not accounted in Book (add)",
        brs.bankCredits(),
        BankStatementLine::getCredit);
    rows.add(total("Balance as per Bank (computed)", f.computedBankBalance()));
    rows.add(total("Balance as per Bank Statement", f.statementBalance()));
    rows.add(total("Unexplained Difference", f.difference()));
    List<ReportColumn> columns =
        List.of(
            ReportColumn.date("date", "Doc. Date"),
            ReportColumn.text("tranCode", "Tran. Code"),
            ReportColumn.text("docNo", "Doc. No."),
            ReportColumn.text("description", "Description"),
            ReportColumn.text("reference", "Chq. No. / Reference"),
            ReportColumn.amountNoTotal(AMOUNT, "Amount"));
    return new ReportResult(
        "FIN-BRS-STMT",
        metadata().title(),
        p.echo(),
        columns,
        rows,
        List.of(
            "Bank balance = Book balance - (1) + (2) - (3) + (4); amounts in the bank account"
                + " currency; the difference must be zero to finalize the reconciliation"));
  }

  private static void book(
      List<ReportRow> rows,
      String title,
      List<BookEntry> entries,
      Function<BookEntry, BigDecimal> amount) {
    rows.add(new ReportRow(RowKind.SECTION, 1, title, Map.of()));
    BigDecimal total = BigDecimal.ZERO;
    for (BookEntry e : entries) {
      Map<String, Object> cells = new LinkedHashMap<>();
      cells.put("date", e.valueDate());
      cells.put("tranCode", e.journalType());
      cells.put("docNo", e.batchNo());
      putText(cells, "description", e.narration());
      putText(cells, "reference", e.reference());
      cells.put(AMOUNT, amount.apply(e));
      rows.add(ReportRow.detail(cells));
      total = total.add(amount.apply(e));
    }
    rows.add(new ReportRow(RowKind.SUBTOTAL, 1, "Sub Total", Map.of(AMOUNT, total)));
  }

  private static void bank(
      List<ReportRow> rows,
      String title,
      List<BankStatementLine> lines,
      Function<BankStatementLine, BigDecimal> amount) {
    rows.add(new ReportRow(RowKind.SECTION, 1, title, Map.of()));
    BigDecimal total = BigDecimal.ZERO;
    for (BankStatementLine l : lines) {
      Map<String, Object> cells = new LinkedHashMap<>();
      cells.put("date", l.getValueDate());
      cells.put("tranCode", "BANK");
      cells.put("docNo", "Line " + l.getLineNo());
      putText(cells, "description", l.getDescription());
      putText(cells, "reference", l.getReference());
      cells.put(AMOUNT, amount.apply(l));
      rows.add(ReportRow.detail(cells));
      total = total.add(amount.apply(l));
    }
    rows.add(new ReportRow(RowKind.SUBTOTAL, 1, "Sub Total", Map.of(AMOUNT, total)));
  }

  private static ReportRow total(String label, BigDecimal value) {
    return new ReportRow(RowKind.TOTAL, 0, label, Map.of(AMOUNT, value));
  }

  private static void putText(Map<String, Object> cells, String key, String value) {
    if (value != null) {
      cells.put(key, value);
    }
  }
}
