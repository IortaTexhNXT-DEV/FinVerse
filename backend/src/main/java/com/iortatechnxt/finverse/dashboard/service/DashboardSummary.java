package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Executive dashboard figures (base currency).
 *
 * @param asOf as-of date
 * @param fiscalYearStart start of the current fiscal year
 * @param totalIncomeYtd income year to date
 * @param totalExpenseYtd expenses year to date
 * @param netResultYtd net result year to date
 * @param cashPosition cash and bank balances
 * @param receivables insurance receivables
 * @param technicalReserves technical reserves
 * @param totalAssets total assets
 * @param totalEquity total equity including current year result
 * @param pendingJournals journals pending authorization
 * @param draftJournals draft or rejected journals
 * @param monthly monthly income/expense trend of the fiscal year
 * @param incomeComposition income by statement line
 * @param expenseComposition expenses by statement line
 */
public record DashboardSummary(
    LocalDate asOf,
    LocalDate fiscalYearStart,
    BigDecimal totalIncomeYtd,
    BigDecimal totalExpenseYtd,
    BigDecimal netResultYtd,
    BigDecimal cashPosition,
    BigDecimal receivables,
    BigDecimal technicalReserves,
    BigDecimal totalAssets,
    BigDecimal totalEquity,
    long pendingJournals,
    long draftJournals,
    List<MonthlyPoint> monthly,
    List<CompositionItem> incomeComposition,
    List<CompositionItem> expenseComposition) {

  /**
   * One month of the trend.
   *
   * @param month label yyyy-MM
   * @param income income
   * @param expense expenses
   * @param netResult income minus expenses
   */
  public record MonthlyPoint(
      String month, BigDecimal income, BigDecimal expense, BigDecimal netResult) {}

  /**
   * Statement line amount.
   *
   * @param label statement line
   * @param amount amount (natural sign)
   */
  public record CompositionItem(String label, BigDecimal amount) {}
}
