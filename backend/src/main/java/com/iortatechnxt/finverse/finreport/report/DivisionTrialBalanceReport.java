package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.finreport.service.MovementRow;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-TB-DIVDEPT – Main Accounts Trial Balance by Division / Department (PREMIA FGL013). Division =
 * branch, department = cost centre of the posting line ("-" when the line has none). Order by
 * Division gives Division &gt; Department totals; order by Main A/c gives a total per main account.
 */
@Component
public class DivisionTrialBalanceReport implements ReportDefinition {

  private static final String DIVISION = "division";
  private static final String DEPARTMENT = "department";
  private static final String MAIN = "mainAccount";
  private static final String BY_DIVISION = "DIVISION";
  private static final String NONE = "-";

  private final FinReportQueries queries;
  private final FinReportSupport support;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   * @param clock clock
   */
  public DivisionTrialBalanceReport(
      FinReportQueries queries, FinReportSupport support, Clock clock) {
    this.queries = queries;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.add(FinParams.month(clock));
    params.add(FinParams.year(clock));
    params.add(FinParams.division());
    params.add(FinParams.department());
    params.addAll(FinParams.mainRange());
    params.add(
        ParameterSpec.select(
            FinParams.ORDER_BY, "Order By", List.of(BY_DIVISION, "MAIN_ACCOUNT"), BY_DIVISION));
    return new ReportMetadata(
        "FIN-TB-DIVDEPT",
        "Main Accounts Trial Balance by Division / Department",
        ReportCategory.GENERAL_LEDGER,
        "Monthly trial balance of main accounts per branch and cost centre (FGL013)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    YearMonth month = FinReportSupport.month(p);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    LedgerQuery q =
        new LedgerQuery(
            companyId,
            FinParams.branch(p),
            FinParams.costCenter(p),
            month.atDay(1),
            month.atEndOfMonth(),
            h.postableIds(
                FinReportSupport.range(p, FinParams.MAIN_FROM, FinParams.MAIN_TO), a -> true),
            null,
            null);
    Map<Key, TbAmounts> amounts =
        TrialBalances.rollUp(
            queries.dimensionMovements(q), r -> key(r, h.mainOf(r.accountId()), branches));
    boolean byDivision = BY_DIVISION.equals(p.text(FinParams.ORDER_BY));
    Comparator<Key> order =
        byDivision
            ? Comparator.comparing(Key::division)
                .thenComparing(Key::department)
                .thenComparing(k -> k.main().code())
            : Comparator.<Key, String>comparing(k -> k.main().code())
                .thenComparing(Key::division)
                .thenComparing(Key::department);
    List<Map<String, Object>> rows = new ArrayList<>();
    amounts.entrySet().stream()
        .filter(e -> !e.getValue().isZero())
        .sorted(Map.Entry.comparingByKey(order))
        .forEach(e -> rows.add(cells(e.getKey(), e.getValue())));
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(DIVISION, "Division"));
    columns.add(ReportColumn.text(DEPARTMENT, "Department"));
    columns.addAll(TrialBalances.columns("Main A/c"));
    TabularReportBuilder b = TabularReportBuilder.of(p).columns(columns).presorted().rows(rows);
    if (byDivision) {
      b.groupBy(DIVISION, "Division").groupBy(DEPARTMENT, "Department");
    } else {
      b.groupBy(MAIN, "Main A/c");
    }
    return b.note(TrialBalances.balanceNote(rows))
        .note("Division = branch; Department = cost centre of the posting line.")
        .build();
  }

  private static Key key(MovementRow r, AccountNode main, Map<Long, String> branches) {
    String cc = r.costCenter() == null ? NONE : r.costCenter();
    return new Key(branches.getOrDefault(r.branchId(), NONE), cc, main);
  }

  private static Map<String, Object> cells(Key k, TbAmounts t) {
    Map<String, Object> cells = TrialBalances.cells(k.main(), t);
    cells.put(DIVISION, k.division());
    cells.put(DEPARTMENT, k.department());
    cells.put(MAIN, k.main().caption());
    return cells;
  }

  private record Key(String division, String department, AccountNode main) {}
}
