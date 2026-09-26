package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries.BookDef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A BIR book of accounts (FRBS 3.2.0, Appendix A VII; list #18-#27): the general journal, purchase
 * journal, sales revenue journal, cash receipts and cash disbursements books list the posted lines
 * of their journals (definition in {@code tax_book_def}, V703); the general ledger lists, per
 * account of one class, the balance brought forward and the lines of the period. Loose-leaf PDF or
 * Excel; the CAS / DAT format is open (AQ07).
 */
public final class BookOfAccountsReport implements ReportDefinition {

  private static final String ACCOUNT_CLASS = "accountClass";
  private static final String ACCOUNT = "account";
  private static final String DEBIT = "debit";
  private static final String CREDIT = "credit";

  private final BirOutputQueries queries;
  private final String book;
  private final String title;

  /**
   * Creates the report.
   *
   * @param queries ledger reads
   * @param book book code in {@code tax_book_def}
   * @param title title
   */
  BookOfAccountsReport(BirOutputQueries queries, String book, String title) {
    this.queries = queries;
    this.book = book;
    this.title = title;
  }

  private boolean ledger() {
    return "SL".equals(book);
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(TaxReportSupport.company());
    params.add(
        ParameterSpec.required(TaxReportSupport.FROM, "From date", ParameterType.DATE)
            .withDefault("MONTH_START"));
    params.add(TaxReportSupport.to());
    if (ledger()) {
      params.add(
          ParameterSpec.select(
              ACCOUNT_CLASS,
              "Account class",
              List.of("ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE", "MEMORANDUM"),
              "ASSET"));
    }
    return new ReportMetadata(
        "TAX-BOOK-" + book,
        "Books of Accounts - " + title,
        ReportCategory.TAX_STATUTORY,
        "BIR book of accounts: " + title + " (FRBS 3.2.0, App. A VII)",
        params,
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    BookDef def = queries.book(book);
    Long companyId = TaxReportSupport.companyId(p);
    LocalDate from = p.date(TaxReportSupport.FROM);
    LocalDate to = p.date(TaxReportSupport.TO);
    TabularReportBuilder builder = TabularReportBuilder.of(p).presorted();
    if (ledger()) {
      List<Map<String, Object>> rows = queries.ledger(companyId, p.text(ACCOUNT_CLASS), from, to);
      rows.forEach(r -> r.put(ACCOUNT, r.get(ACCOUNT) + " " + r.get("account_name")));
      builder
          .columns(
              ReportColumn.text(ACCOUNT, "Account"),
              ReportColumn.date("value_date", "Date"),
              ReportColumn.text("batch_no", "Voucher"),
              ReportColumn.text("narration", "Particulars"),
              ReportColumn.amount(DEBIT, "Debit"),
              ReportColumn.amount(CREDIT, "Credit"))
          .rows(rows)
          .groupBy(ACCOUNT, "Account");
    } else {
      builder
          .columns(
              ReportColumn.date("value_date", "Date"),
              ReportColumn.text("batch_no", "Voucher"),
              ReportColumn.text("journal_type", "Journal"),
              ReportColumn.text("ref", "Reference"),
              ReportColumn.text("narration", "Particulars"),
              ReportColumn.text(ACCOUNT, "Account"),
              ReportColumn.text("account_name", "Account title"),
              ReportColumn.text("party_code", "Party"),
              ReportColumn.amount(DEBIT, "Debit"),
              ReportColumn.amount(CREDIT, "Credit"))
          .rows(queries.journal(companyId, def, from, to));
    }
    return builder
        .note(
            def.description() + "; posted entries in base currency, CAS format to confirm (AQ07).")
        .build();
  }
}
