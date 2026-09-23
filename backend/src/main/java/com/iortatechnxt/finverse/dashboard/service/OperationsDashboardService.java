package com.iortatechnxt.finverse.dashboard.service;

import com.iortatechnxt.finverse.alert.service.AlertService;
import com.iortatechnxt.finverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.finverse.approval.service.ApprovalInboxService.ApprovalCounts;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService.BudgetComparison;
import com.iortatechnxt.finverse.budget.service.VarianceLine;
import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.receivables.service.CollectionQueries;
import com.iortatechnxt.finverse.receivables.service.CollectionQueries.DueAmount;
import com.iortatechnxt.finverse.receivables.service.CollectionQueries.MonthlyAmount;
import com.iortatechnxt.finverse.subledger.service.AgeingService;
import com.iortatechnxt.finverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executive dashboard widgets that need business modules, read only through their query services
 * (the dashboard depends on them, never the reverse): collections and debtors ageing (receivables),
 * expense budget against actual (budget), open alerts and the viewer's approval inbox.
 */
@Service
@Transactional(readOnly = true)
public class OperationsDashboardService {

  private static final int TOP_BUDGET_LINES = 6;

  private final CollectionQueries collections;
  private final AgeingService ageing;
  private final BudgetMonitoringService budgets;
  private final AlertService alerts;
  private final ApprovalInboxService inbox;
  private final DashboardCalendar calendar;

  /**
   * Creates the service.
   *
   * @param collections receivables collection figures
   * @param ageing sub-ledger ageing (default slots)
   * @param budgets budget monitoring
   * @param alerts alerts
   * @param inbox approval inbox
   * @param calendar fiscal year and reference date
   */
  public OperationsDashboardService(
      CollectionQueries collections,
      AgeingService ageing,
      BudgetMonitoringService budgets,
      AlertService alerts,
      ApprovalInboxService inbox,
      DashboardCalendar calendar) {
    this.collections = collections;
    this.ageing = ageing;
    this.budgets = budgets;
    this.alerts = alerts;
    this.inbox = inbox;
    this.calendar = calendar;
  }

  /**
   * Collections of the month and year against the debtors' outstanding balance aged into the
   * company's default slots (current balances, due-date basis).
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param asOf reference date, null for today
   * @return collections widget
   */
  public CollectionsWidget collections(Long companyId, Long branchId, LocalDate asOf) {
    LocalDate date = calendar.dateOrToday(asOf);
    LocalDate yearStart = calendar.yearStart(companyId, date);
    List<MonthlyAmount> received =
        collections.monthlyCollections(companyId, branchId, yearStart, date);
    List<MonthlyValue> monthly =
        received.stream()
            .map(m -> new MonthlyValue(m.month().toString(), Money.round(m.amount())))
            .toList();
    BigDecimal ytd = sum(received, MonthlyAmount::amount);
    BigDecimal mtd =
        sum(
            received.stream().filter(m -> m.month().equals(YearMonth.from(date))).toList(),
            MonthlyAmount::amount);
    AgeingSlots slots = ageing.defaultSlots();
    BigDecimal[] buckets = new BigDecimal[slots.size()];
    Arrays.fill(buckets, Money.zero());
    BigDecimal notYetDue = Money.zero();
    for (DueAmount due : collections.debtorOutstandingByDueDate(companyId, branchId)) {
      long days = ChronoUnit.DAYS.between(due.dueDate(), date);
      int bucket = slots.index(days);
      buckets[bucket] = buckets[bucket].add(due.amount());
      if (days <= 0) {
        notYetDue = notYetDue.add(due.amount());
      }
    }
    List<LabelledAmount> aged = new ArrayList<>();
    for (int i = 0; i < buckets.length; i++) {
      aged.add(new LabelledAmount(slots.label(i), Money.round(buckets[i])));
    }
    return new CollectionsWidget(
        date,
        mtd,
        ytd,
        sum(aged, LabelledAmount::amount),
        Money.round(notYetDue),
        slots.describe(),
        aged,
        monthly);
  }

  /**
   * Expense budget against actual of the approved budget of the fiscal year; without fiscal year or
   * approved budget the budget figures are zero and the version is null.
   *
   * @param companyId company
   * @param asOf reference date, null for today
   * @return budget widget
   */
  public BudgetWidget budget(Long companyId, LocalDate asOf) {
    LocalDate date = calendar.dateOrToday(asOf);
    if (calendar.fiscalYear(companyId, date).isEmpty()) {
      return new BudgetWidget(
          date.getYear(), null, Money.zero(), Money.zero(), Money.zero(), null, List.of());
    }
    BudgetComparison comparison = budgets.compare(companyId, date, false);
    List<VarianceLine> expenses =
        comparison.lines().stream().filter(l -> l.accountClass() == AccountClass.EXPENSE).toList();
    BigDecimal annual = sum(expenses, VarianceLine::annualBudget);
    BigDecimal actual = sum(expenses, VarianceLine::actualYtd);
    List<BudgetAccountFigures> top =
        expenses.stream()
            .sorted(
                Comparator.comparing((VarianceLine l) -> l.actualYtd().max(l.budgetYtd()))
                    .reversed())
            .limit(TOP_BUDGET_LINES)
            .map(
                l ->
                    new BudgetAccountFigures(
                        l.accountCode() + " " + l.accountName(),
                        Money.round(l.budgetYtd()),
                        Money.round(l.actualYtd())))
            .toList();
    return new BudgetWidget(
        comparison.fiscalYear(),
        comparison.budgetVersion(),
        annual,
        sum(expenses, VarianceLine::budgetYtd),
        actual,
        annual.signum() == 0 ? null : percent(actual, annual),
        top);
  }

  /**
   * Open alerts of the company and the signed-in user's approval inbox.
   *
   * @param companyId company
   * @return workload widget
   */
  public WorkloadWidget workload(Long companyId) {
    ApprovalCounts counts = inbox.counts(companyId);
    return new WorkloadWidget(alerts.liveCount(companyId), counts.total(), counts.byModule());
  }

  private static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> amount) {
    return Money.round(items.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private static BigDecimal percent(BigDecimal value, BigDecimal base) {
    return value.multiply(BigDecimal.valueOf(100)).divide(base, Money.SCALE, Money.ROUNDING);
  }
}
