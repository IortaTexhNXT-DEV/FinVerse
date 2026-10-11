package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.StatusFilter;
import com.iortatechnxt.brokerverse.finreport.service.VoucherLine;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * One engine, two layouts: FIN-GL-TXNLIST (List of Transactions Detailed, FR2183) and
 * FIN-GL-PROCLIST (List of Processed / Unprocessed Transactions, FGL002). Vouchers are grouped by
 * transaction code with a Transaction-wise Summary (vouchers, entries, debits, credits) per code
 * and a Report-wise Summary at the end.
 */
@Component
public class VoucherListingEngine {

  private static final String STATUS_FLAG = "statusFlag";
  private static final String DESCRIPTION = "description";
  private static final String DOC_REFERENCE = "docReference";
  private static final String ORDER_VALUE = "orderValue";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the engine.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public VoucherListingEngine(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  /**
   * Parameters shared by both layouts.
   *
   * @return specs
   */
  public static List<ParameterSpec> parameters() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.addAll(FinParams.txnRange());
    params.addAll(FinParams.docRange());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.mainRange());
    params.addAll(FinParams.subRange());
    params.add(FinParams.orderBy());
    params.add(FinParams.user());
    params.add(FinParams.status(StatusFilter.BOTH));
    params.add(FinParams.flag(FinParams.COMBINE, "Combine Transactions"));
    return params;
  }

  /**
   * Generates the listing.
   *
   * @param p parameters
   * @param processedLayout true for the FGL002 layout, false for FR2183
   * @return result
   */
  public ReportResult generate(ReportParameters p, boolean processedLayout) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    boolean byNumber = FinParams.BY_NUMBER.equals(p.text(FinParams.ORDER_BY));
    List<VoucherLine> lines =
        Vouchers.sort(queries.voucherLines(Vouchers.query(p, h, FinParams.status(p))), byNumber);
    if (p.flag(FinParams.COMBINE)) {
      lines = Vouchers.combine(lines);
    }
    List<ReportRow> rows = new ArrayList<>();
    Vouchers.group(lines, VoucherLine::transactionCode)
        .forEach(
            (tc, tcLines) -> {
              rows.add(
                  FinRows.label(
                      RowKind.GROUP_HEADER,
                      0,
                      "Transaction Code : " + FinReportSupport.transactionCodeLabel(tc)));
              Vouchers.group(tcLines, VoucherLine::batchId)
                  .values()
                  .forEach(v -> addVoucher(rows, v, h, branches, processedLayout, byNumber));
              rows.add(
                  Vouchers.summary(RowKind.SUBTOTAL, 0, "Transaction-wise Summary " + tc, tcLines));
            });
    if (!lines.isEmpty()) {
      rows.add(Vouchers.summary(RowKind.TOTAL, 0, "Report-wise Summary", lines));
    }
    var meta = p.metadata();
    return new ReportResult(
        meta.code(),
        meta.title(),
        p.echo(),
        columns(processedLayout),
        rows,
        List.of(
            "Transaction code = voucher number prefix. Status P = posted, U = unposted"
                + " (draft or pending approval). Division = branch, Department = cost centre,"
                + " Activity = line of business."));
  }

  private static void addVoucher(
      List<ReportRow> rows,
      List<VoucherLine> voucher,
      AccountHierarchy h,
      Map<Long, String> branches,
      boolean processedLayout,
      boolean byNumber) {
    VoucherLine first = voucher.get(0);
    rows.add(FinRows.label(RowKind.GROUP_HEADER, 1, Vouchers.header(first)));
    for (VoucherLine l : voucher) {
      Map<String, Object> cells = Vouchers.lineCells(l, h, branches);
      cells.put(STATUS_FLAG, StatusFilter.flag(l.status()));
      if (processedLayout) {
        cells.put(ORDER_VALUE, byNumber ? l.batchNo() : l.valueDate().toString());
      } else {
        cells.putAll(
            FinRows.cells(DESCRIPTION, l.lineNarration(), DOC_REFERENCE, l.lineReference()));
      }
      rows.add(ReportRow.detail(cells));
    }
  }

  private static List<ReportColumn> columns(boolean processedLayout) {
    List<ReportColumn> cols = new ArrayList<>();
    cols.add(ReportColumn.text(Vouchers.MAIN_AC, "Main A/c"));
    cols.add(ReportColumn.text(Vouchers.SUB_AC, "Sub A/c"));
    cols.add(ReportColumn.text(Vouchers.ACCOUNT_NAME, "Account Name"));
    cols.add(ReportColumn.text(Vouchers.DIVISION, "Divn"));
    cols.add(ReportColumn.text(Vouchers.DEPARTMENT, "Dept"));
    cols.add(ReportColumn.text(Vouchers.ACTIVITY, "Acty"));
    cols.add(ReportColumn.text(Vouchers.PARTY, "Party"));
    cols.add(ReportColumn.text(Vouchers.CURRENCY, "Curr"));
    cols.add(ReportColumn.amountNoTotal(Vouchers.FC_VALUE, "FC Value"));
    cols.add(ReportColumn.amount(Vouchers.DEBIT, "LC Debit"));
    cols.add(ReportColumn.amount(Vouchers.CREDIT, "LC Credit"));
    cols.add(ReportColumn.text(STATUS_FLAG, "Status"));
    if (processedLayout) {
      cols.add(ReportColumn.text(ORDER_VALUE, "Order By Value"));
    } else {
      cols.add(ReportColumn.text(DESCRIPTION, "Description"));
      cols.add(ReportColumn.text(DOC_REFERENCE, "Doc Reference"));
    }
    return cols;
  }
}
