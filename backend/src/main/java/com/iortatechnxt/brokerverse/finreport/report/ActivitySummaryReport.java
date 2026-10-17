package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.LedgerQuery;
import com.iortatechnxt.brokerverse.finreport.service.MovementRow;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-ACT-SUM2 – Activity Analysis Report Summary II (PREMIA FMI023B): period debits and credits
 * per activity, by division, department and main account (aggregated in SQL from the ledger).
 */
@Component
public class ActivitySummaryReport implements ReportDefinition {

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public ActivitySummaryReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-ACT-SUM2",
        "Activity Analysis Summary II",
        ReportCategory.GENERAL_LEDGER,
        "Main account movements by activity per division and department (FMI023B)",
        ActivityAnalysis.parameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    Map<String, String> names =
        queries.dimensionNames(companyId, ActivityAnalysis.dimensionType(p));
    LedgerQuery q = FinReportSupport.dimensionQuery(p, h);
    boolean combine = p.flag(FinParams.COMBINE);
    Map<Key, MovementRow> sums =
        TrialBalances.sumByKey(
            queries.dimensionMovements(q),
            r ->
                new Key(
                    branches.getOrDefault(r.branchId(), ActivityAnalysis.NONE),
                    r.costCenter() == null ? ActivityAnalysis.NONE : r.costCenter(),
                    combine ? "" : h.mainOf(r.accountId()).caption(),
                    ActivityAnalysis.activity(p, r.businessLine(), r.costCenter()),
                    r.currency()));
    List<Map<String, Object>> rows = new ArrayList<>();
    sums.forEach(
        (key, r) -> {
          boolean moved = r.debitBase().signum() != 0 || r.creditBase().signum() != 0;
          if (moved && ActivityAnalysis.selected(p, key.activity())) {
            rows.add(
                FinRows.cells(
                    Vouchers.DIVISION, key.division(),
                    Vouchers.DEPARTMENT, key.department(),
                    ActivityAnalysis.MAIN, key.main(),
                    ActivityAnalysis.ACTIVITY, ActivityAnalysis.caption(key.activity(), names),
                    Vouchers.CURRENCY, r.currency(),
                    ActivityAnalysis.FC_AMOUNT, r.debitFc().subtract(r.creditFc()),
                    Vouchers.DEBIT, r.debitBase(),
                    Vouchers.CREDIT, r.creditBase()));
          }
        });
    rows.sort(Comparator.comparing(r -> ActivityAnalysis.sortKey(r, combine)));
    TabularReportBuilder b =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text(ActivityAnalysis.ACTIVITY, "Main Acty / Name"),
                ReportColumn.text(Vouchers.CURRENCY, "Currency"),
                ReportColumn.amountNoTotal(ActivityAnalysis.FC_AMOUNT, "FC Amount"),
                ReportColumn.amount(Vouchers.DEBIT, "LC Amount Debits"),
                ReportColumn.amount(Vouchers.CREDIT, "LC Amount Credits"))
            .presorted()
            .rows(rows)
            .note("Activity = " + ActivityAnalysis.dimensionType(p) + " of the posting line.");
    return ActivityAnalysis.groups(b, p).build();
  }

  private record Key(
      String division, String department, String main, String activity, String currency) {}
}
