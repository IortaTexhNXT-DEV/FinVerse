package com.iortatechnxt.brokerverse.budget.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Budget against actual of one account (and optionally cost centre), all amounts in natural sign
 * (income and expense positive).
 *
 * <p>Variance = actual − budget. It is favourable when income exceeds budget or expense stays below
 * budget. Percentages are relative to the budget and undefined (null) when the budget is 0.
 *
 * @param accountCode account
 * @param accountName account name
 * @param accountClass INCOME or EXPENSE
 * @param costCenter cost centre, or null for the whole account
 * @param budgetMonth budget of the selected month
 * @param actualMonth actual of the selected month (to the as-of date)
 * @param budgetYtd budget from fiscal year start to the selected month
 * @param actualYtd actual from fiscal year start to the as-of date
 * @param annualBudget budget of the whole year
 */
public record VarianceLine(
    String accountCode,
    String accountName,
    AccountClass accountClass,
    String costCenter,
    BigDecimal budgetMonth,
    BigDecimal actualMonth,
    BigDecimal budgetYtd,
    BigDecimal actualYtd,
    BigDecimal annualBudget) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int PCT_SCALE = 2;
  private static final int WORK_SCALE = 10;

  /**
   * Month variance.
   *
   * @return actual minus budget
   */
  public BigDecimal monthVariance() {
    return actualMonth.subtract(budgetMonth);
  }

  /**
   * Year-to-date variance.
   *
   * @return actual minus budget
   */
  public BigDecimal ytdVariance() {
    return actualYtd.subtract(budgetYtd);
  }

  /**
   * Month variance in percent of the month budget.
   *
   * @return percent, or null without budget
   */
  public BigDecimal monthVariancePct() {
    return percent(monthVariance(), budgetMonth);
  }

  /**
   * Year-to-date variance in percent of the YTD budget.
   *
   * @return percent, or null without budget
   */
  public BigDecimal ytdVariancePct() {
    return percent(ytdVariance(), budgetYtd);
  }

  /**
   * Budget utilization: YTD actual in percent of the annual budget.
   *
   * @return percent, or null without budget
   */
  public BigDecimal utilizationPct() {
    return percent(actualYtd, annualBudget);
  }

  /**
   * Remaining budget for the year.
   *
   * @return annual budget minus YTD actual
   */
  public BigDecimal available() {
    return annualBudget.subtract(actualYtd);
  }

  /**
   * Whether the YTD variance is favourable.
   *
   * @return true when income is at or above budget, or expense at or below budget
   */
  public boolean favourable() {
    int sign = ytdVariance().signum();
    return accountClass == AccountClass.INCOME ? sign >= 0 : sign <= 0;
  }

  /**
   * Computes {@code value / base * 100}.
   *
   * @param value numerator
   * @param base denominator
   * @return percent at scale 2, or null when the base is zero
   */
  static BigDecimal percent(BigDecimal value, BigDecimal base) {
    if (base.signum() == 0) {
      return null;
    }
    return value
        .multiply(HUNDRED)
        .divide(base.abs(), WORK_SCALE, RoundingMode.HALF_EVEN)
        .setScale(PCT_SCALE, RoundingMode.HALF_EVEN);
  }
}
