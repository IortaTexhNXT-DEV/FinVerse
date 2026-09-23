package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Figures of the executive dashboard widgets (base currency, natural sign). Each widget is served
 * by its own endpoint, so one widget without data or with an error never blanks the others; a
 * widget without data returns zeros and empty lists.
 */
public final class DashboardWidgets {

  private DashboardWidgets() {}

  /**
   * Current year against the previous year for one month.
   *
   * @param month month label {@code yyyy-MM} of the current year
   * @param current amount of the month
   * @param priorYear amount of the same month one year earlier
   */
  public record TrendPoint(String month, BigDecimal current, BigDecimal priorYear) {}

  /**
   * One value per month.
   *
   * @param month month label {@code yyyy-MM}
   * @param amount amount
   */
  public record MonthlyValue(String month, BigDecimal amount) {}

  /**
   * Labelled amount (ageing bucket, account, statement line).
   *
   * @param label label
   * @param amount amount
   */
  public record LabelledAmount(String label, BigDecimal amount) {}

  /**
   * Gross written premium from the premium income statement lines.
   *
   * @param asOf reference date
   * @param yearStart first day of the fiscal year
   * @param monthToDate premium of the current month up to the reference date
   * @param monthToDatePriorYear same days one year earlier
   * @param yearToDate premium of the fiscal year up to the reference date
   * @param yearToDatePriorYear same period one year earlier
   * @param monthly current year against prior year per month
   */
  public record PremiumWidget(
      LocalDate asOf,
      LocalDate yearStart,
      BigDecimal monthToDate,
      BigDecimal monthToDatePriorYear,
      BigDecimal yearToDate,
      BigDecimal yearToDatePriorYear,
      List<TrendPoint> monthly) {

    /** Canonical constructor copying the list. */
    public PremiumWidget {
      monthly = List.copyOf(monthly);
    }
  }

  /**
   * Claims paid (claims expense accounts) and outstanding (claims reserve accounts).
   *
   * @param asOf reference date
   * @param paidMonthToDate claims paid in the current month
   * @param paidYearToDate claims paid in the fiscal year
   * @param outstanding outstanding claims reserve at the reference date
   * @param monthly claims paid per month against the prior year
   */
  public record ClaimsWidget(
      LocalDate asOf,
      BigDecimal paidMonthToDate,
      BigDecimal paidYearToDate,
      BigDecimal outstanding,
      List<TrendPoint> monthly) {

    /** Canonical constructor copying the list. */
    public ClaimsWidget {
      monthly = List.copyOf(monthly);
    }
  }

  /**
   * Collections (approved receipts) against the debtors' outstanding balance and its ageing.
   *
   * @param asOf reference date
   * @param collectedMonthToDate receipts of the current month
   * @param collectedYearToDate receipts of the fiscal year
   * @param receivables outstanding debit items of debtors
   * @param notYetDue part of the receivables not yet due
   * @param ageingSlots slot definition used for the buckets, e.g. "30/60/90/120"
   * @param ageing receivables per ageing bucket (due-date basis; not-yet-due in the first bucket)
   * @param monthly receipts per month of the fiscal year
   */
  public record CollectionsWidget(
      LocalDate asOf,
      BigDecimal collectedMonthToDate,
      BigDecimal collectedYearToDate,
      BigDecimal receivables,
      BigDecimal notYetDue,
      String ageingSlots,
      List<LabelledAmount> ageing,
      List<MonthlyValue> monthly) {

    /** Canonical constructor copying the lists. */
    public CollectionsWidget {
      ageing = List.copyOf(ageing);
      monthly = List.copyOf(monthly);
    }
  }

  /**
   * Vendor payables by due date.
   *
   * @param asOf reference date
   * @param overdue due before the reference date
   * @param dueIn7Days due within 7 days of the reference date
   * @param dueIn30Days due within 30 days of the reference date (includes the 7 days)
   * @param total all outstanding vendor payables
   * @param openItems number of outstanding items
   */
  public record PayablesWidget(
      LocalDate asOf,
      BigDecimal overdue,
      BigDecimal dueIn7Days,
      BigDecimal dueIn30Days,
      BigDecimal total,
      long openItems) {}

  /**
   * Cash and bank position.
   *
   * @param asOf reference date
   * @param total balance of the cash statement lines
   * @param accounts balance per cash or bank account
   * @param monthly month-end balance of the fiscal year (the current month at the reference date)
   */
  public record CashWidget(
      LocalDate asOf, BigDecimal total, List<LabelledAmount> accounts, List<MonthlyValue> monthly) {

    /** Canonical constructor copying the lists. */
    public CashWidget {
      accounts = List.copyOf(accounts);
      monthly = List.copyOf(monthly);
    }
  }

  /**
   * Expense budget against actual of the approved budget of the current fiscal year.
   *
   * @param fiscalYear fiscal year
   * @param budgetVersion approved version used, null when no budget is approved
   * @param annualBudget annual expense budget
   * @param budgetToDate expense budget up to the current month
   * @param actualToDate actual expenses of the fiscal year
   * @param utilizationPct actual in percent of the annual budget (null without budget)
   * @param lines largest expense accounts: budget to date and actual
   */
  public record BudgetWidget(
      int fiscalYear,
      Integer budgetVersion,
      BigDecimal annualBudget,
      BigDecimal budgetToDate,
      BigDecimal actualToDate,
      BigDecimal utilizationPct,
      List<BudgetLine> lines) {

    /** Canonical constructor copying the list. */
    public BudgetWidget {
      lines = List.copyOf(lines);
    }
  }

  /**
   * Budget against actual of one expense account.
   *
   * @param account account code and name
   * @param budgetToDate budget up to the current month
   * @param actualToDate actual of the fiscal year
   */
  public record BudgetLine(String account, BigDecimal budgetToDate, BigDecimal actualToDate) {}

  /**
   * Work waiting for people: open alerts of the company and the viewer's approval inbox.
   *
   * @param openAlerts open or acknowledged alerts of the company
   * @param pendingApprovals items in the viewer's approval inbox for the company
   * @param approvalsByModule inbox items per module code
   */
  public record WorkloadWidget(
      long openAlerts, long pendingApprovals, Map<String, Long> approvalsByModule) {

    /** Canonical constructor copying the map. */
    public WorkloadWidget {
      approvalsByModule = Map.copyOf(approvalsByModule);
    }
  }
}
