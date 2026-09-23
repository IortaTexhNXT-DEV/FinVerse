package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.common.util.AmountInWords;
import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.StatusFilter;
import com.iortatechnxt.finverse.finreport.service.VoucherLine;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-GL-VOUCHER – Voucher (PREMIA FGL001): journal voucher print, one block per voucher with the
 * header, the lines, the total in figures and in words (in the company's base currency, the
 * currency of the LC amounts) and the Entered / Authorised / Approved signature block.
 */
@Component
public class VoucherPrintReport implements ReportDefinition {

  private static final String LINE_NARRATION = "lineNarration";
  private static final String MAIN_NAME = "mainName";
  private static final String DATE_UNKNOWN = "-";

  private final FinReportQueries queries;
  private final FinReportSupport support;
  private final OrganizationService organization;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   * @param organization organization service
   */
  public VoucherPrintReport(
      FinReportQueries queries, FinReportSupport support, OrganizationService organization) {
    this.queries = queries;
    this.support = support;
    this.organization = organization;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.addAll(FinParams.txnRange());
    params.addAll(FinParams.docRange());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.add(FinParams.status(StatusFilter.BOTH));
    params.add(FinParams.user());
    return new ReportMetadata(
        "FIN-GL-VOUCHER",
        "Voucher",
        ReportCategory.GENERAL_LEDGER,
        "Journal voucher print with amount in words and signature block (FGL001)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    String baseCurrency = organization.getCompany(companyId).getBaseCurrency();
    String currencyName = queries.currencyName(baseCurrency);
    List<VoucherLine> lines =
        Vouchers.sort(queries.voucherLines(Vouchers.query(p, h, FinParams.status(p))), true);
    List<ReportRow> rows = new ArrayList<>();
    for (List<VoucherLine> voucher : Vouchers.group(lines, VoucherLine::batchId).values()) {
      VoucherLine head = voucher.get(0);
      rows.add(
          FinRows.label(
              RowKind.GROUP_HEADER,
              0,
              "Voucher "
                  + head.batchNo()
                  + "  Date: "
                  + head.valueDate()
                  + "  Status: "
                  + head.status()
                  + "  Ref: "
                  + FinRows.text(head.reference())));
      rows.add(FinRows.label(RowKind.SECTION, 1, "Narration: " + head.narration()));
      for (VoucherLine l : voucher) {
        Map<String, Object> cells = Vouchers.lineCells(l, h, branches);
        cells.put(MAIN_NAME, h.mainOf(l.accountId()).name());
        if (l.lineNarration() != null) {
          cells.put(LINE_NARRATION, l.lineNarration());
        }
        rows.add(ReportRow.detail(cells));
      }
      BigDecimal total =
          voucher.stream().map(VoucherLine::debitBase).reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal credits =
          voucher.stream().map(VoucherLine::creditBase).reduce(BigDecimal.ZERO, BigDecimal::add);
      rows.add(
          FinRows.row(
              RowKind.SUBTOTAL,
              0,
              "Total Amount",
              FinRows.cells(Vouchers.DEBIT, total, Vouchers.CREDIT, credits)));
      rows.add(
          FinRows.label(
              RowKind.SECTION, 1, "Amount in Words: " + AmountInWords.spell(total, currencyName)));
      rows.add(FinRows.label(RowKind.SECTION, 1, signatures(head)));
    }
    var meta = p.metadata();
    return new ReportResult(
        meta.code(),
        meta.title(),
        p.echo(),
        columns(),
        rows,
        List.of("Amounts in " + baseCurrency + " (" + currencyName + ")."));
  }

  private static String signatures(VoucherLine head) {
    return "Entered by: "
        + head.createdBy()
        + " on "
        + date(head.createdAt())
        + "   Submitted by: "
        + blankLine(head.submittedBy())
        + "   Authorised / Approved by: "
        + blankLine(head.authorizedBy())
        + " on "
        + date(head.authorizedAt());
  }

  private static String blankLine(String user) {
    return user == null ? "__________" : user;
  }

  private static String date(Instant instant) {
    return instant == null
        ? DATE_UNKNOWN
        : instant.atOffset(ZoneOffset.UTC).toLocalDate().toString();
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text(Vouchers.MAIN_AC, "Main A/c"),
        ReportColumn.text(MAIN_NAME, "Main A/c Description"),
        ReportColumn.text(Vouchers.SUB_AC, "Sub A/c"),
        ReportColumn.text(Vouchers.ACCOUNT_NAME, "Sub A/c Description"),
        ReportColumn.text(Vouchers.DIVISION, "Divn"),
        ReportColumn.text(Vouchers.DEPARTMENT, "Dept"),
        ReportColumn.text(Vouchers.ACTIVITY, "Acty"),
        ReportColumn.text(Vouchers.PARTY, "Party"),
        ReportColumn.text(Vouchers.CURRENCY, "Currency"),
        ReportColumn.amountNoTotal(Vouchers.FC_VALUE, "FC Amt"),
        ReportColumn.amount(Vouchers.DEBIT, "Amount DR"),
        ReportColumn.amount(Vouchers.CREDIT, "Amount CR"),
        ReportColumn.text(LINE_NARRATION, "Narration"));
  }
}
