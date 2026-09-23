package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.finreport.service.MovementRow;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
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
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * FIN-TB-SUB – Sub Account Trial Balance (PREMIA FGL014): monthly balances of the postable sub
 * accounts, grouped under their main account, with a total per main account. Only main accounts
 * that have sub accounts are included; the main account total equals the control balance by
 * construction (every posting is on a leaf).
 */
@Component
public class SubAccountTrialBalanceReport implements ReportDefinition {

  private static final String MAIN = "mainAccount";

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
  public SubAccountTrialBalanceReport(
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
    params.addAll(FinParams.subRange());
    return new ReportMetadata(
        "FIN-TB-SUB",
        "Sub Account Trial Balance",
        ReportCategory.GENERAL_LEDGER,
        "Monthly balances of sub accounts under their main account (FGL014)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    YearMonth month = FinReportSupport.month(p);
    AccountHierarchy h = support.hierarchy(companyId);
    Set<Long> ids = FinReportSupport.selectedAccounts(h, p);
    ids.removeIf(id -> !h.isSubAccount(id));
    String costCenter = FinParams.costCenter(p);
    LedgerQuery q =
        new LedgerQuery(
            companyId,
            FinParams.branch(p),
            costCenter,
            month.atDay(1),
            month.atEndOfMonth(),
            ids,
            null,
            null);
    List<MovementRow> movements =
        costCenter == null ? queries.movements(q) : queries.dimensionMovements(q);
    Map<AccountNode, TbAmounts> bySub = TrialBalances.rollUp(movements, r -> h.node(r.accountId()));
    List<Map<String, Object>> rows = new ArrayList<>();
    bySub.entrySet().stream()
        .filter(e -> !e.getValue().isZero())
        .sorted(
            Map.Entry.comparingByKey(
                Comparator.<AccountNode, String>comparing(a -> h.mainOf(a.id()).code())
                    .thenComparing(AccountNode::code)))
        .forEach(
            e -> {
              Map<String, Object> cells = TrialBalances.cells(e.getKey(), e.getValue());
              cells.put(MAIN, h.mainOf(e.getKey().id()).caption());
              rows.add(cells);
            });
    return TabularReportBuilder.of(p)
        .columns(TrialBalances.columns("Sub A/c"))
        .groupBy(MAIN, "Main A/c")
        .presorted()
        .rows(rows)
        .note("Main A/c totals give the Sub Account Closing Balance (closing debits less credits).")
        .build();
  }
}
