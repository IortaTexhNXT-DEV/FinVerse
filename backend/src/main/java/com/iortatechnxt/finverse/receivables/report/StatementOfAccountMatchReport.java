package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.PartyStatementService;
import com.iortatechnxt.finverse.receivables.service.PartyStatementService.PartyStatement;
import com.iortatechnxt.finverse.receivables.service.PartyStatementService.StatementLine;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
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
import org.springframework.stereotype.Component;

/**
 * FIN-ARAP-SOA-MATCH (Src FR2545) Statement of Account with Matched / Un-Matched Details for
 * debtors and creditors: documents of the period split into knocked-off and open ones.
 */
@Component
public class StatementOfAccountMatchReport implements ReportDefinition {

  private static final String CODE = "FIN-ARAP-SOA-MATCH";
  private static final String BALANCE = "balance";
  private static final String NET_BALANCE = "Net Balance";

  private final PartyStatementService statements;

  /**
   * Creates the report.
   *
   * @param statements party statement service
   */
  public StatementOfAccountMatchReport(PartyStatementService statements) {
    this.statements = statements;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    params.add(GlReportSupport.fromParam().withDefault("YEAR_START"));
    params.add(GlReportSupport.toParam());
    params.addAll(ReceivablesReportSupport.partyParams());
    params.add(
        ParameterSpec.select(
            ReceivablesReportSupport.CURRENCY_BASIS,
            "Currency",
            List.of(ReceivablesReportSupport.FOREIGN, ReceivablesReportSupport.BASE),
            ReceivablesReportSupport.FOREIGN));
    return new ReportMetadata(
        CODE,
        "Statement of Account with Matched/Un-Matched Details (Debtors/Creditors)",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Documents of the period split into matched (knocked off) and unmatched",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<ReportRow> rows = new ArrayList<>();
    BigDecimal grand = BigDecimal.ZERO;
    for (PartyStatement s :
        statements.statements(
            p.longValue(GlReportSupport.COMPANY),
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO),
            ReceivablesReportSupport.foreign(p),
            ReceivablesReportSupport.partyFilter(p))) {
      rows.add(
          new ReportRow(RowKind.GROUP_HEADER, 0, s.partyCode() + " - " + s.partyName(), Map.of()));
      section(rows, "Matched Details", s.matched(), s.matchedNet());
      section(rows, "Unmatched Details", s.unmatched(), s.unmatchedNet());
      grand = grand.add(s.unmatchedNet());
    }
    rows.add(new ReportRow(RowKind.TOTAL, 0, "Grand Total", Map.of(BALANCE, grand)));
    List<ReportColumn> columns =
        List.of(
            ReportColumn.date("documentDate", "Document Date"),
            ReportColumn.text("reference", "Document Reference"),
            ReportColumn.text("transactionCode", "Transaction Code"),
            ReportColumn.text("chequeNo", "Cheque Number"),
            ReportColumn.date("chequeDate", "Cheque Date"),
            ReportColumn.amount("debit", "Debit Amount"),
            ReportColumn.amount("credit", "Credit Amount"),
            ReportColumn.amountNoTotal("original", "Original Amount"),
            ReportColumn.amountNoTotal(BALANCE, "Balance Amount"));
    return new ReportResult(
        CODE,
        metadata().title(),
        p.echo(),
        columns,
        rows,
        List.of("Matched = balance zero at the end of the period; Grand Total = unmatched net"));
  }

  private static void section(
      List<ReportRow> rows, String title, List<StatementLine> lines, BigDecimal net) {
    rows.add(new ReportRow(RowKind.SECTION, 1, title, Map.of()));
    for (StatementLine l : lines) {
      Map<String, Object> cells = new LinkedHashMap<>();
      cells.put("documentDate", l.documentDate());
      putIfPresent(cells, "reference", l.reference());
      cells.put("transactionCode", l.transactionCode());
      putIfPresent(cells, "chequeNo", l.chequeNo());
      putIfPresent(cells, "chequeDate", l.chequeDate());
      cells.put("debit", l.debit());
      cells.put("credit", l.credit());
      cells.put("original", l.original());
      cells.put(BALANCE, l.balance());
      rows.add(ReportRow.detail(cells));
    }
    rows.add(new ReportRow(RowKind.SUBTOTAL, 1, NET_BALANCE, Map.of(BALANCE, net)));
  }

  private static void putIfPresent(Map<String, Object> cells, String key, Object value) {
    if (value != null) {
      cells.put(key, value);
    }
  }
}
