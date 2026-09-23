package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.LedgerLine;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-ACT-DET – Activity Analysis Report Detailed (PREMIA FMI024): the posted entries of each main
 * account by activity, with totals per activity, main account, department and division.
 */
@Component
public class ActivityDetailReport implements ReportDefinition {

  private static final String DOC_DATE = "docDate";
  private static final String TYPE = "type";
  private static final String TC = "tc";
  private static final String DOC_NO = "docNo";
  private static final String NARRATION = "narration";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public ActivityDetailReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-ACT-DET",
        "Activity Analysis Detailed",
        ReportCategory.GENERAL_LEDGER,
        "Transactions of each main account by activity (FMI024)",
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
    List<Map<String, Object>> rows = new ArrayList<>();
    for (LedgerLine l : queries.ledgerLines(q, false)) {
      String activity = ActivityAnalysis.activity(p, l.businessLine(), l.costCenter());
      if (ActivityAnalysis.selected(p, activity)) {
        rows.add(
            FinRows.cells(
                Vouchers.DIVISION,
                branches.getOrDefault(l.branchId(), ActivityAnalysis.NONE),
                Vouchers.DEPARTMENT,
                l.costCenter() == null ? ActivityAnalysis.NONE : l.costCenter(),
                ActivityAnalysis.MAIN,
                h.mainOf(l.accountId()).caption(),
                ActivityAnalysis.ACTIVITY,
                ActivityAnalysis.caption(activity, names),
                DOC_DATE,
                l.valueDate(),
                TYPE,
                l.journalType(),
                TC,
                l.transactionCode(),
                DOC_NO,
                l.batchNo(),
                NARRATION,
                l.narration(),
                Vouchers.CURRENCY,
                l.currency(),
                ActivityAnalysis.FC_AMOUNT,
                l.debitFc().subtract(l.creditFc()),
                Vouchers.DEBIT,
                l.debitBase(),
                Vouchers.CREDIT,
                l.creditBase()));
      }
    }
    rows.sort(Comparator.comparing(r -> ActivityAnalysis.sortKey(r, combine)));
    TabularReportBuilder b = TabularReportBuilder.of(p).columns(columns()).presorted().rows(rows);
    ActivityAnalysis.groups(b, p).groupBy(ActivityAnalysis.ACTIVITY, "Activity");
    return b.note("Activity = " + ActivityAnalysis.dimensionType(p) + " of the posting line.")
        .build();
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.date(DOC_DATE, "Doc Date"),
        ReportColumn.text(TYPE, "Type"),
        ReportColumn.text(TC, "TC"),
        ReportColumn.text(DOC_NO, "Doc No."),
        ReportColumn.text(NARRATION, "Narration"),
        ReportColumn.text(Vouchers.CURRENCY, "Cur"),
        ReportColumn.amountNoTotal(ActivityAnalysis.FC_AMOUNT, "FC Amount"),
        ReportColumn.amount(Vouchers.DEBIT, "LC Amount Debits"),
        ReportColumn.amount(Vouchers.CREDIT, "LC Amount Credits"));
  }
}
