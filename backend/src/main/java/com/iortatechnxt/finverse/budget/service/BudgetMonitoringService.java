package com.iortatechnxt.finverse.budget.service;

import com.iortatechnxt.finverse.budget.domain.Budget;
import com.iortatechnxt.finverse.budget.domain.BudgetLine;
import com.iortatechnxt.finverse.budget.service.BudgetActualsQuery.MonthlyActual;
import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.period.domain.FiscalYear;
import com.iortatechnxt.finverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Budget monitoring: Budget vs Actual (month and year to date), utilization and threshold alerts,
 * always against the latest APPROVED budget version.
 */
@Service
@Transactional(readOnly = true)
public class BudgetMonitoringService {

  private final BudgetService budgets;
  private final BudgetActualsQuery actuals;
  private final ChartOfAccountsService accounts;
  private final PeriodService periods;

  /**
   * Creates the service.
   *
   * @param budgets budget service
   * @param actuals ledger actuals
   * @param accounts chart of accounts
   * @param periods period service
   */
  public BudgetMonitoringService(
      BudgetService budgets,
      BudgetActualsQuery actuals,
      ChartOfAccountsService accounts,
      PeriodService periods) {
    this.budgets = budgets;
    this.actuals = actuals;
    this.accounts = accounts;
    this.periods = periods;
  }

  /**
   * Compares the approved budget with actuals.
   *
   * @param companyId company
   * @param asOf as-of date (its month is the "month" column; YTD runs from fiscal year start)
   * @param byCostCenter true to split lines per cost centre
   * @return comparison
   */
  public BudgetComparison compare(Long companyId, LocalDate asOf, boolean byCostCenter) {
    FiscalYear year = periods.yearContaining(companyId, asOf);
    int periodNo = (int) ChronoUnit.MONTHS.between(year.getStartDate(), asOf.withDayOfMonth(1)) + 1;
    Optional<Budget> budget = budgets.approved(companyId, year.getYearCode());
    Map<Long, GlAccount> byId =
        accounts.list(companyId).stream()
            .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
    Map<String, GlAccount> byCode =
        byId.values().stream().collect(Collectors.toMap(GlAccount::getCode, Function.identity()));
    Map<String, Accumulator> rows = new TreeMap<>();
    budget.ifPresent(
        b -> b.getLines().forEach(l -> addBudget(rows, l, byCode, periodNo, byCostCenter)));
    for (MonthlyActual a : actuals.monthly(companyId, year.getStartDate(), asOf)) {
      GlAccount account = byId.get(a.accountId());
      if (account != null && !account.getAccountClass().isBalanceSheet()) {
        int month = (int) ChronoUnit.MONTHS.between(year.getStartDate(), a.monthStart()) + 1;
        rows.computeIfAbsent(
                key(account.getCode(), a.costCenter(), byCostCenter),
                k -> new Accumulator(account, byCostCenter ? a.costCenter() : null))
            .addActual(a.netDebit(), month == periodNo);
      }
    }
    List<VarianceLine> lines =
        rows.values().stream()
            .map(Accumulator::toLine)
            .sorted(
                Comparator.comparing(VarianceLine::accountClass)
                    .thenComparing(VarianceLine::accountCode)
                    .thenComparing(l -> Objects.toString(l.costCenter(), "")))
            .toList();
    return new BudgetComparison(
        year.getYearCode(),
        periodNo,
        budget.map(Budget::getId).orElse(null),
        budget.map(Budget::getVersionNo).orElse(null),
        lines);
  }

  /**
   * Budget threshold alerts: expense accounts whose YTD actual has reached a percentage of the
   * annual budget ("Budget Threshold Exceeded").
   *
   * @param companyId company
   * @param asOf as-of date
   * @param thresholdPct utilization threshold in percent
   * @return accounts at or above the threshold, highest utilization first
   */
  public List<VarianceLine> alerts(Long companyId, LocalDate asOf, BigDecimal thresholdPct) {
    return compare(companyId, asOf, false).lines().stream()
        .filter(l -> l.accountClass() == AccountClass.EXPENSE)
        .filter(l -> l.utilizationPct() != null && l.utilizationPct().compareTo(thresholdPct) >= 0)
        .sorted(Comparator.comparing(VarianceLine::utilizationPct).reversed())
        .toList();
  }

  private static void addBudget(
      Map<String, Accumulator> rows,
      BudgetLine line,
      Map<String, GlAccount> byCode,
      int periodNo,
      boolean byCostCenter) {
    rows.computeIfAbsent(
            key(line.getAccountCode(), line.getCostCenter(), byCostCenter),
            k ->
                new Accumulator(
                    byCode.get(line.getAccountCode()), byCostCenter ? line.getCostCenter() : null))
        .addBudget(line, periodNo);
  }

  private static String key(String accountCode, String costCenter, boolean byCostCenter) {
    return byCostCenter ? accountCode + "|" + Objects.toString(costCenter, "") : accountCode;
  }

  /**
   * Result of a comparison.
   *
   * @param fiscalYear fiscal year
   * @param periodNo selected month within the fiscal year (1..12)
   * @param budgetId approved budget version used, or null when none is approved
   * @param budgetVersion its version number
   * @param lines lines by account class and code
   */
  public record BudgetComparison(
      int fiscalYear,
      int periodNo,
      Long budgetId,
      Integer budgetVersion,
      List<VarianceLine> lines) {

    /** Canonical constructor copying lines. */
    public BudgetComparison {
      lines = List.copyOf(lines);
    }
  }

  /** Mutable totals of one comparison row. */
  private static final class Accumulator {
    private final GlAccount account;
    private final String costCenter;
    private BigDecimal budgetMonth = Money.zero();
    private BigDecimal budgetYtd = Money.zero();
    private BigDecimal annual = Money.zero();
    private BigDecimal actualMonth = Money.zero();
    private BigDecimal actualYtd = Money.zero();

    Accumulator(GlAccount account, String costCenter) {
      this.account = account;
      this.costCenter = costCenter;
    }

    void addBudget(BudgetLine line, int periodNo) {
      budgetMonth = budgetMonth.add(line.amount(periodNo));
      budgetYtd = budgetYtd.add(line.upTo(periodNo));
      annual = annual.add(line.annual());
    }

    void addActual(BigDecimal netDebit, boolean inMonth) {
      BigDecimal natural =
          account.getAccountClass() == AccountClass.INCOME ? netDebit.negate() : netDebit;
      actualYtd = actualYtd.add(natural);
      if (inMonth) {
        actualMonth = actualMonth.add(natural);
      }
    }

    VarianceLine toLine() {
      return new VarianceLine(
          account.getCode(),
          account.getName(),
          account.getAccountClass(),
          costCenter,
          budgetMonth,
          Money.round(actualMonth),
          budgetYtd,
          Money.round(actualYtd),
          annual);
    }
  }
}
