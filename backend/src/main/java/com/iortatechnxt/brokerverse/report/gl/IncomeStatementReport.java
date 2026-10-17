package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
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
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Income statement (profit and loss) for a date range, optionally compared with the same range of
 * the previous year.
 */
@Component
public class IncomeStatementReport implements ReportDefinition {

  private static final String CURRENT = "current";
  private static final String PRIOR = "prior";

  private final FinancialStatementService statements;

  /**
   * Creates the report.
   *
   * @param statements statement computations
   */
  public IncomeStatementReport(FinancialStatementService statements) {
    this.statements = statements;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-PL",
        "Income Statement (Profit and Loss)",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Income, expenses and net result for a period, with prior year comparative",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            ParameterSpec.required(GlReportSupport.FROM, "From Date", ParameterType.DATE)
                .withDefault("YEAR_START"),
            GlReportSupport.toParam(),
            ParameterSpec.optional(
                "comparePriorYear", "Compare with prior year", ParameterType.BOOLEAN)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Long branchId = p.optionalLong(GlReportSupport.BRANCH).orElse(null);
    LocalDate from = p.date(GlReportSupport.FROM);
    LocalDate to = p.date(GlReportSupport.TO);
    boolean compare = p.flag("comparePriorYear");
    var cur = statements.performance(companyId, branchId, from, to);
    var prior =
        compare
            ? statements.performance(companyId, branchId, from.minusYears(1), to.minusYears(1))
            : null;

    List<ReportColumn> cols = new ArrayList<>();
    cols.add(ReportColumn.amountNoTotal(CURRENT, from + " to " + to));
    if (compare) {
      cols.add(ReportColumn.amountNoTotal(PRIOR, from.minusYears(1) + " to " + to.minusYears(1)));
    }
    List<ReportRow> rows = new ArrayList<>();
    BigDecimal[] income = section(rows, "INCOME", AccountClass.INCOME, cur, prior);
    BigDecimal[] expense = section(rows, "EXPENSES", AccountClass.EXPENSE, cur, prior);
    rows.add(
        new ReportRow(
            RowKind.TOTAL,
            0,
            "NET PROFIT / (LOSS)",
            cells(
                income[0].subtract(expense[0]), compare ? income[1].subtract(expense[1]) : null)));
    return new ReportResult("GL-PL", metadata().title(), p.echo(), cols, rows, List.of());
  }

  private static BigDecimal[] section(
      List<ReportRow> rows,
      String title,
      AccountClass cls,
      Map<AccountClass, Map<String, BigDecimal>> cur,
      Map<AccountClass, Map<String, BigDecimal>> prior) {
    rows.add(ReportRow.section(title));
    List<String> labels =
        prior == null
            ? FinancialStatementService.labels(cls, cur)
            : FinancialStatementService.labels(cls, cur, prior);
    for (String label : labels) {
      rows.add(
          new ReportRow(
              RowKind.DETAIL,
              1,
              label,
              cells(value(cur, cls, label), prior == null ? null : value(prior, cls, label))));
    }
    BigDecimal totalCur = FinancialStatementService.total(cur, cls);
    BigDecimal totalPrior =
        prior == null ? BigDecimal.ZERO : FinancialStatementService.total(prior, cls);
    rows.add(
        new ReportRow(
            RowKind.SUBTOTAL,
            0,
            "Total " + title,
            cells(totalCur, prior == null ? null : totalPrior)));
    return new BigDecimal[] {totalCur, totalPrior};
  }

  private static BigDecimal value(
      Map<AccountClass, Map<String, BigDecimal>> lines, AccountClass cls, String label) {
    return lines.getOrDefault(cls, Map.of()).getOrDefault(label, BigDecimal.ZERO);
  }

  private static Map<String, Object> cells(BigDecimal current, BigDecimal prior) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(CURRENT, current);
    if (prior != null) {
      m.put(PRIOR, prior);
    }
    return m;
  }
}
