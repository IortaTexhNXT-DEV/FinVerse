package com.iortatechnxt.finverse.report.gl;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
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
import com.iortatechnxt.finverse.report.gl.FinancialStatementService.Position;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Statement of financial position (balance sheet) with optional comparative date. */
@Component
public class BalanceSheetReport implements ReportDefinition {

  private static final String CURRENT = "current";
  private static final String PRIOR = "prior";

  private final FinancialStatementService statements;

  /**
   * Creates the report.
   *
   * @param statements statement computations
   */
  public BalanceSheetReport(FinancialStatementService statements) {
    this.statements = statements;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-BS",
        "Balance Sheet (Statement of Financial Position)",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Assets, liabilities and equity as of a date, with optional comparative",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.optional("compareDate", "Comparative Date", ParameterType.DATE)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Long branchId = p.optionalLong(GlReportSupport.BRANCH).orElse(null);
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    Optional<LocalDate> compare = p.optionalDate("compareDate");
    Position cur = statements.position(companyId, branchId, asOf);
    Position prior = compare.map(d -> statements.position(companyId, branchId, d)).orElse(null);

    List<ReportColumn> cols = new ArrayList<>();
    cols.add(ReportColumn.amountNoTotal(CURRENT, asOf.toString()));
    compare.ifPresent(d -> cols.add(ReportColumn.amountNoTotal(PRIOR, d.toString())));

    List<ReportRow> rows = new ArrayList<>();
    BigDecimal[] assets = section(rows, "ASSETS", AccountClass.ASSET, cur, prior, List.of());
    BigDecimal[] liabilities =
        section(rows, "LIABILITIES", AccountClass.LIABILITY, cur, prior, List.of());
    List<Extra> equityExtras =
        List.of(
            new Extra(
                "Current Year Profit / (Loss)",
                cur.currentYearResult(),
                prior == null ? null : prior.currentYearResult()),
            new Extra(
                "Prior Years' Results Not Yet Closed",
                cur.unclosedPriorResults(),
                prior == null ? null : prior.unclosedPriorResults()));
    BigDecimal[] equity = section(rows, "EQUITY", AccountClass.EQUITY, cur, prior, equityExtras);
    rows.add(
        new ReportRow(
            RowKind.TOTAL,
            0,
            "TOTAL LIABILITIES AND EQUITY",
            cells(
                liabilities[0].add(equity[0]),
                prior == null ? null : liabilities[1].add(equity[1]))));
    String note =
        assets[0].compareTo(liabilities[0].add(equity[0])) == 0
            ? "Balance sheet is in balance."
            : "WARNING: assets differ from liabilities and equity.";
    return new ReportResult("GL-BS", metadata().title(), p.echo(), cols, rows, List.of(note));
  }

  private static BigDecimal[] section(
      List<ReportRow> rows,
      String title,
      AccountClass cls,
      Position cur,
      Position prior,
      List<Extra> extras) {
    rows.add(ReportRow.section(title));
    BigDecimal totalCur = BigDecimal.ZERO;
    BigDecimal totalPrior = BigDecimal.ZERO;
    List<String> labels =
        prior == null
            ? FinancialStatementService.labels(cls, cur.lines())
            : FinancialStatementService.labels(cls, cur.lines(), prior.lines());
    for (String label : labels) {
      BigDecimal c = value(cur, cls, label);
      BigDecimal pr = prior == null ? null : value(prior, cls, label);
      rows.add(new ReportRow(RowKind.DETAIL, 1, label, cells(c, pr)));
      totalCur = totalCur.add(c);
      totalPrior = pr == null ? totalPrior : totalPrior.add(pr);
    }
    for (Extra e : extras) {
      rows.add(new ReportRow(RowKind.DETAIL, 1, e.label(), cells(e.current(), e.prior())));
      totalCur = totalCur.add(e.current());
      totalPrior = e.prior() == null ? totalPrior : totalPrior.add(e.prior());
    }
    rows.add(
        new ReportRow(
            RowKind.SUBTOTAL,
            0,
            "Total " + title,
            cells(totalCur, prior == null ? null : totalPrior)));
    return new BigDecimal[] {totalCur, totalPrior};
  }

  private static BigDecimal value(Position pos, AccountClass cls, String label) {
    return pos.lines().getOrDefault(cls, Map.of()).getOrDefault(label, BigDecimal.ZERO);
  }

  private static Map<String, Object> cells(BigDecimal current, BigDecimal prior) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(CURRENT, current);
    if (prior != null) {
      m.put(PRIOR, prior);
    }
    return m;
  }

  private record Extra(String label, BigDecimal current, BigDecimal prior) {}
}
