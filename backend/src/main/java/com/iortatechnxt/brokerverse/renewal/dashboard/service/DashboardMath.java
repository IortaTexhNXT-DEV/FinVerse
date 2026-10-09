package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The formulas of the Renewal dashboard (FRRN.002.02.01, .04, .06, .08): percentages to two
 * decimals, rounded half up; a percentage of a zero base is not shown (null).
 */
public final class DashboardMath {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int SCALE = 2;
  private static final int WORK_SCALE = 10;

  private DashboardMath() {}

  /**
   * {@code part / base x 100}.
   *
   * @param part numerator
   * @param base denominator
   * @return percentage with two decimals, or null when the base is zero
   */
  public static BigDecimal percent(BigDecimal part, BigDecimal base) {
    if (base == null || base.signum() == 0 || part == null) {
      return null;
    }
    return part.divide(base, WORK_SCALE, RoundingMode.HALF_UP)
        .multiply(HUNDRED)
        .setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * {@code part / base x 100} for counts.
   *
   * @param part numerator
   * @param base denominator
   * @return percentage, or null when the base is zero
   */
  public static BigDecimal percent(long part, long base) {
    return percent(BigDecimal.valueOf(part), BigDecimal.valueOf(base));
  }

  /**
   * Variance: actual minus budget.
   *
   * @param actual actual
   * @param budget budget
   * @return difference
   */
  public static BigDecimal variance(BigDecimal actual, BigDecimal budget) {
    return actual.subtract(budget).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * Growth as BDOI states it: previous year's actual over the current budget, times 100.
   *
   * @param previousActual actual of the same period one year earlier
   * @param budget current budget
   * @return percentage, or null without a budget
   */
  public static BigDecimal growth(BigDecimal previousActual, BigDecimal budget) {
    return percent(previousActual, budget);
  }
}
