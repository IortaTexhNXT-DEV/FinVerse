package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.LedgerQuery;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-TB-MAIN – Main Accounts Trial Balance (PREMIA FGL012): opening balance, the month's debits
 * and credits and the closing balance of every main account (sub and micro accounts rolled up).
 */
@Component
public class MainTrialBalanceReport implements ReportDefinition {

  private static final String INCLUDE_ZERO = "includeZero";

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
  public MainTrialBalanceReport(FinReportQueries queries, FinReportSupport support, Clock clock) {
    this.queries = queries;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-TB-MAIN",
        "Main Accounts Trial Balance",
        ReportCategory.GENERAL_LEDGER,
        "Monthly opening, movement and closing balance of main accounts (FGL012)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            FinParams.month(clock),
            FinParams.year(clock),
            FinParams.flag(INCLUDE_ZERO, "Show A/c Not in Use")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    YearMonth month = FinReportSupport.month(p);
    AccountHierarchy h = support.hierarchy(companyId);
    LedgerQuery q =
        LedgerQuery.of(
            companyId,
            FinParams.branch(p),
            month.atDay(1),
            month.atEndOfMonth(),
            h.postableIds(a -> true, a -> true));
    Map<AccountNode, TbAmounts> byMain =
        TrialBalances.rollUp(queries.movements(q), r -> h.mainOf(r.accountId()));
    if (p.flag(INCLUDE_ZERO)) {
      h.mainAccounts().forEach(m -> byMain.putIfAbsent(m, TbAmounts.ZERO));
    }
    List<Map<String, Object>> rows = TrialBalances.mainRows(byMain, p.flag(INCLUDE_ZERO));
    return TabularReportBuilder.of(p)
        .columns(TrialBalances.columns("Main A/c"))
        .rows(rows)
        .note(TrialBalances.balanceNote(rows))
        .note("Period: " + month.atDay(1) + " to " + month.atEndOfMonth() + ".")
        .build();
  }
}
