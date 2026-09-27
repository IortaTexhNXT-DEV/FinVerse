package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.StatusFilter;
import com.iortatechnxt.brokerverse.finreport.service.VoucherLine;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-GL-DAYBOOK – Journal and Other Day Books (PREMIA FGL008): posted vouchers only, one book per
 * transaction code (Journal, Receipts, Payments, Premium...) or all codes as one book when "Combine
 * Type" is set. Each book ends with the number of vouchers and entries and its totals.
 */
@Component
public class DayBookReport implements ReportDefinition {

  private static final String DOC_DATE = "docDate";
  private static final String TC = "tc";
  private static final String DOC_NO = "docNo";
  private static final String REFERENCE = "reference";
  private static final String NARRATION = "narration";
  private static final String ALL_BOOKS = "ALL";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public DayBookReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.addAll(FinParams.txnRange());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.docRange());
    params.add(FinParams.flag(FinParams.COMBINE, "Combine Type"));
    params.add(FinParams.user());
    return new ReportMetadata(
        "FIN-GL-DAYBOOK",
        "Journal and Other Day Books",
        ReportCategory.GENERAL_LEDGER,
        "Posted vouchers as journal, cash and other day books by transaction code (FGL008)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<VoucherLine> lines =
        Vouchers.sort(queries.voucherLines(Vouchers.query(p, h, StatusFilter.POSTED)), false);
    boolean combine = p.flag(FinParams.COMBINE);
    List<ReportRow> rows = new ArrayList<>();
    Vouchers.group(lines, l -> combine ? ALL_BOOKS : l.transactionCode())
        .forEach(
            (book, bookLines) -> {
              String caption =
                  combine ? "All transaction codes" : FinReportSupport.transactionCodeLabel(book);
              rows.add(FinRows.label(RowKind.GROUP_HEADER, 0, "Book : " + caption));
              bookLines.forEach(l -> rows.add(ReportRow.detail(cells(l, h, branches))));
              rows.add(Vouchers.summary(RowKind.SUBTOTAL, 0, "Book Total " + book, bookLines));
            });
    if (!lines.isEmpty()) {
      rows.add(Vouchers.summary(RowKind.TOTAL, 0, "TOTAL", lines));
    }
    var meta = p.metadata();
    return new ReportResult(
        meta.code(),
        meta.title(),
        p.echo(),
        columns(),
        rows,
        List.of("Posted vouchers only. Debits equal credits per voucher and per book."));
  }

  private static Map<String, Object> cells(
      VoucherLine l, AccountHierarchy h, Map<Long, String> branches) {
    Map<String, Object> cells = Vouchers.lineCells(l, h, branches);
    cells.putAll(
        FinRows.cells(
            DOC_DATE,
            l.valueDate(),
            TC,
            l.transactionCode(),
            DOC_NO,
            l.batchNo(),
            REFERENCE,
            l.lineReference() == null ? l.reference() : l.lineReference(),
            NARRATION,
            l.lineNarration() == null ? l.narration() : l.lineNarration()));
    return cells;
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.date(DOC_DATE, "Document Date"),
        ReportColumn.text(TC, "TC"),
        ReportColumn.text(DOC_NO, "Document Number"),
        ReportColumn.text(Vouchers.MAIN_AC, "Main A/c"),
        ReportColumn.text(Vouchers.ACCOUNT_NAME, "Account Name"),
        ReportColumn.text(Vouchers.SUB_AC, "Sub A/c"),
        ReportColumn.text(Vouchers.DIVISION, "Division"),
        ReportColumn.text(Vouchers.DEPARTMENT, "Department"),
        ReportColumn.amount(Vouchers.DEBIT, "Debits"),
        ReportColumn.amount(Vouchers.CREDIT, "Credits"),
        ReportColumn.text(REFERENCE, "Document Reference"),
        ReportColumn.text(NARRATION, "Narration"),
        ReportColumn.text(Vouchers.ACTIVITY, "Activity"));
  }
}
