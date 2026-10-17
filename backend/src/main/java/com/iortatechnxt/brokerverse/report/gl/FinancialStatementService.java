package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes statement lines (by account report group) for the balance sheet and income statement.
 *
 * <p>The balance sheet always balances: equity includes the current fiscal year result and any
 * prior-year results not yet transferred by year-end closing.
 */
@Service
@Transactional(readOnly = true)
public class FinancialStatementService {

  private static final String UNGROUPED = "Other";

  private final LedgerQueryService ledger;
  private final PeriodService periods;
  private final GlReportSupport support;

  /**
   * Creates the service.
   *
   * @param ledger ledger read model
   * @param periods period service
   * @param support GL helpers
   */
  public FinancialStatementService(
      LedgerQueryService ledger, PeriodService periods, GlReportSupport support) {
    this.ledger = ledger;
    this.periods = periods;
    this.support = support;
  }

  /**
   * Balance sheet position as of a date.
   *
   * @param companyId company
   * @param branchId branch or null
   * @param asOf date
   * @return position
   */
  public Position position(Long companyId, Long branchId, LocalDate asOf) {
    Map<Long, GlAccount> accounts = support.accountsById(companyId);
    Map<AccountClass, Map<String, BigDecimal>> lines = new EnumMap<>(AccountClass.class);
    BigDecimal pnlBeforeYear = BigDecimal.ZERO;
    BigDecimal pnlAll = BigDecimal.ZERO;
    LocalDate yearStart = periods.yearContaining(companyId, asOf).getStartDate();
    for (AccountBalance b :
        ledger.balances(new BalanceQuery(companyId, branchId, null, asOf, false))) {
      GlAccount a = accounts.get(b.accountId());
      if (a.getAccountClass().isBalanceSheet()) {
        if (a.getAccountClass() != AccountClass.MEMORANDUM) {
          addLine(lines, a, b.netBase());
        }
      } else {
        pnlAll = pnlAll.add(b.netBase().negate());
      }
    }
    for (AccountBalance b :
        ledger.balances(
            new BalanceQuery(companyId, branchId, null, yearStart.minusDays(1), false))) {
      if (!accounts.get(b.accountId()).getAccountClass().isBalanceSheet()) {
        pnlBeforeYear = pnlBeforeYear.add(b.netBase().negate());
      }
    }
    return new Position(lines, pnlAll.subtract(pnlBeforeYear), pnlBeforeYear);
  }

  /**
   * Income and expense lines for a date range.
   *
   * @param companyId company
   * @param branchId branch or null
   * @param from start date
   * @param to end date
   * @return lines by class (INCOME, EXPENSE)
   */
  public Map<AccountClass, Map<String, BigDecimal>> performance(
      Long companyId, Long branchId, LocalDate from, LocalDate to) {
    Map<Long, GlAccount> accounts = support.accountsById(companyId);
    Map<AccountClass, Map<String, BigDecimal>> lines = new EnumMap<>(AccountClass.class);
    for (AccountBalance b :
        ledger.balances(new BalanceQuery(companyId, branchId, from, to, false))) {
      GlAccount a = accounts.get(b.accountId());
      if (!a.getAccountClass().isBalanceSheet()) {
        addLine(lines, a, b.netBase());
      }
    }
    return lines;
  }

  /**
   * Sums the lines of a class.
   *
   * @param lines lines
   * @param accountClass class
   * @return total in natural sign
   */
  public static BigDecimal total(
      Map<AccountClass, Map<String, BigDecimal>> lines, AccountClass accountClass) {
    return lines.getOrDefault(accountClass, Map.of()).values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Lists the ordered line labels of a class across several line sets (for comparatives).
   *
   * @param accountClass class
   * @param sets line sets
   * @return sorted labels
   */
  @SafeVarargs
  public static List<String> labels(
      AccountClass accountClass, Map<AccountClass, Map<String, BigDecimal>>... sets) {
    Set<String> all = new TreeSet<>();
    for (Map<AccountClass, Map<String, BigDecimal>> s : sets) {
      s.getOrDefault(accountClass, Map.of()).keySet().forEach(all::add);
    }
    return List.copyOf(all);
  }

  private static void addLine(
      Map<AccountClass, Map<String, BigDecimal>> lines, GlAccount a, BigDecimal netDebit) {
    String label = a.getReportGroup() == null ? UNGROUPED : a.getReportGroup();
    lines
        .computeIfAbsent(a.getAccountClass(), k -> new TreeMap<>())
        .merge(label, GlReportSupport.natural(a.getAccountClass(), netDebit), BigDecimal::add);
  }

  /**
   * Balance sheet position.
   *
   * @param lines balance sheet lines by class, natural sign
   * @param currentYearResult profit (positive) or loss of the current fiscal year to date
   * @param unclosedPriorResults prior-year results not yet closed to retained earnings
   */
  public record Position(
      Map<AccountClass, Map<String, BigDecimal>> lines,
      BigDecimal currentYearResult,
      BigDecimal unclosedPriorResults) {}
}
