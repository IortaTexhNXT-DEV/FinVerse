package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-TB-YTD – Main A/C Trial Balance with Annual Figure (PREMIA FR2393): year opening balance (at
 * the fiscal year start), month-to-date and year-to-date debits and credits, and the YTD closing
 * balance per main account.
 */
@Component
public class AnnualTrialBalanceReport implements ReportDefinition {

  private static final String MTD_DR = "mtdDebit";
  private static final String MTD_CR = "mtdCredit";
  private static final String CLOSING = "closing";
  private static final String CLOSING_SIDE = "closingSide";

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
  public AnnualTrialBalanceReport(FinReportQueries queries, FinReportSupport support, Clock clock) {
    this.queries = queries;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-TB-YTD",
        "Main A/C Trial Balance with Annual Figure (YTD)",
        ReportCategory.GENERAL_LEDGER,
        "Year opening, month-to-date and year-to-date movement per main account (FR2393)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            FinParams.year(clock),
            FinParams.month(clock)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    YearMonth month = FinReportSupport.month(p);
    LocalDate yearStart = support.fiscalYearStart(companyId, month.atEndOfMonth());
    AccountHierarchy h = support.hierarchy(companyId);
    LedgerQuery ytdQuery =
        LedgerQuery.of(
            companyId,
            FinParams.branch(p),
            yearStart,
            month.atEndOfMonth(),
            h.postableIds(a -> true, a -> true));
    Map<AccountNode, TbAmounts> ytd =
        TrialBalances.rollUp(queries.movements(ytdQuery), r -> h.mainOf(r.accountId()));
    Map<AccountNode, TbAmounts> mtd =
        TrialBalances.rollUp(
            queries.movements(ytdQuery.withPeriod(month.atDay(1), month.atEndOfMonth())),
            r -> h.mainOf(r.accountId()));
    List<Map<String, Object>> rows = new ArrayList<>();
    ytd.keySet().stream()
        .sorted(Comparator.comparing(AccountNode::code))
        .filter(m -> !ytd.get(m).isZero())
        .forEach(m -> rows.add(cells(m, ytd.get(m), mtd.getOrDefault(m, TbAmounts.ZERO))));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(TrialBalances.CODE, "Main Account"),
            ReportColumn.text(TrialBalances.NAME, "Account Name"),
            ReportColumn.amount(TrialBalances.OPENING, "Year Opening Dr/(Cr)"),
            ReportColumn.amount(MTD_DR, "MTD Debit"),
            ReportColumn.amount(MTD_CR, "MTD Credit"),
            ReportColumn.amount(TrialBalances.DEBIT, "YTD Debit"),
            ReportColumn.amount(TrialBalances.CREDIT, "YTD Credit"),
            ReportColumn.amount(CLOSING, "YTD Closing Dr/(Cr)"),
            ReportColumn.text(CLOSING_SIDE, "Dr/Cr"))
        .rows(rows)
        .note("Fiscal year start: " + yearStart + ". Month: " + month + ".")
        .build();
  }

  private static Map<String, Object> cells(AccountNode m, TbAmounts year, TbAmounts current) {
    return FinRows.cells(
        TrialBalances.CODE,
        m.code(),
        TrialBalances.NAME,
        m.name(),
        TrialBalances.OPENING,
        year.opening(),
        MTD_DR,
        current.debit(),
        MTD_CR,
        current.credit(),
        TrialBalances.DEBIT,
        year.debit(),
        TrialBalances.CREDIT,
        year.credit(),
        CLOSING,
        year.closing(),
        CLOSING_SIDE,
        FinRows.drCr(year.closing()));
  }
}
