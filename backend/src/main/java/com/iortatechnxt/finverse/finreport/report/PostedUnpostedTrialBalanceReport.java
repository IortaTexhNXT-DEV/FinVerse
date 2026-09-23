package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.finreport.service.StatusFilter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * FIN-TB-POSTUNP – Main A/C Trial Balance (Posted and Unposted) (PREMIA FR2174). Shows posted and
 * unposted (draft and pending-approval journals) movements separately so balances can be checked
 * before posting. Opening and closing balances include the selected statuses.
 */
@Component
public class PostedUnpostedTrialBalanceReport implements ReportDefinition {

  private static final String SHOW_ALL = "showAllAccounts";
  private static final String POSTED_DR = "postedDebit";
  private static final String POSTED_CR = "postedCredit";
  private static final String UNPOSTED_DR = "unpostedDebit";
  private static final String UNPOSTED_CR = "unpostedCredit";

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
  public PostedUnpostedTrialBalanceReport(
      FinReportQueries queries, FinReportSupport support, Clock clock) {
    this.queries = queries;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-TB-POSTUNP",
        "Main A/C Trial Balance (Posted and Unposted)",
        ReportCategory.GENERAL_LEDGER,
        "Trial balance including saved but not yet posted vouchers (FR2174)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            FinParams.month(clock),
            FinParams.year(clock),
            FinParams.status(StatusFilter.BOTH),
            FinParams.flag(SHOW_ALL, "Show A/c Not in Use")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    YearMonth month = FinReportSupport.month(p);
    StatusFilter status = FinParams.status(p);
    AccountHierarchy h = support.hierarchy(companyId);
    LedgerQuery q =
        LedgerQuery.of(
            companyId,
            FinParams.branch(p),
            month.atDay(1),
            month.atEndOfMonth(),
            h.postableIds(a -> true, a -> true));
    Map<AccountNode, TbAmounts> posted =
        status == StatusFilter.UNPOSTED
            ? Map.of()
            : TrialBalances.rollUp(queries.movements(q), r -> h.mainOf(r.accountId()));
    Map<AccountNode, TbAmounts> unposted =
        status == StatusFilter.POSTED
            ? Map.of()
            : TrialBalances.rollUp(queries.unpostedMovements(q), r -> h.mainOf(r.accountId()));
    boolean showAll = p.flag(SHOW_ALL);
    Set<AccountNode> mains = new LinkedHashSet<>(posted.keySet());
    mains.addAll(unposted.keySet());
    if (showAll) {
      mains.addAll(h.mainAccounts());
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    mains.stream()
        .sorted(Comparator.comparing(AccountNode::code))
        .forEach(
            m ->
                addRow(
                    rows,
                    m,
                    posted.getOrDefault(m, TbAmounts.ZERO),
                    unposted.getOrDefault(m, TbAmounts.ZERO),
                    showAll));
    return TabularReportBuilder.of(p)
        .columns(columns())
        .rows(rows)
        .note(TrialBalances.balanceNote(rows))
        .note(
            showAll
                ? "Selection: All Accounts"
                : "Selection: A/c With Transaction During Period (or a balance)")
        .note("Unposted = journals in DRAFT or PENDING_APPROVAL status.")
        .build();
  }

  private static void addRow(
      List<Map<String, Object>> rows,
      AccountNode main,
      TbAmounts posted,
      TbAmounts unposted,
      boolean showAll) {
    TbAmounts total = posted.plus(unposted);
    if (total.isZero() && !showAll) {
      return;
    }
    Map<String, Object> cells = TrialBalances.cells(main, total);
    cells.put(POSTED_DR, posted.debit());
    cells.put(POSTED_CR, posted.credit());
    cells.put(UNPOSTED_DR, unposted.debit());
    cells.put(UNPOSTED_CR, unposted.credit());
    rows.add(cells);
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text(TrialBalances.CODE, "Main A/c"),
        ReportColumn.text(TrialBalances.NAME, "Account Name"),
        ReportColumn.amount(TrialBalances.OPENING, "Opening Balance Dr/(Cr)"),
        ReportColumn.amount(POSTED_DR, "Posted Debits"),
        ReportColumn.amount(POSTED_CR, "Posted Credits"),
        ReportColumn.amount(UNPOSTED_DR, "Unposted Debits"),
        ReportColumn.amount(UNPOSTED_CR, "Unposted Credits"),
        ReportColumn.amount(TrialBalances.CLOSING_DR, "Closing Debits"),
        ReportColumn.amount(TrialBalances.CLOSING_CR, "Closing Credits"));
  }
}
