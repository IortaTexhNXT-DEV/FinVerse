package com.iortatechnxt.finverse.investment.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Day count conventions for interest accrual.
 *
 * <ul>
 *   <li>{@link #ACT_365}: Actual/365 Fixed - actual calendar days / 365 (Philippine peso government
 *       securities and deposits).
 *   <li>{@link #THIRTY_360}: 30E/360 (Eurobond basis) - each month counts 30 days: day 31 is
 *       treated as day 30 for both dates; days = 360 x years + 30 x months + days difference, over
 *       360.
 * </ul>
 *
 * <p>Both conventions are additive (days(a, c) = days(a, b) + days(b, c)), so accruing month by
 * month gives exactly the interest of the whole coupon period.
 */
public enum DayCountConvention {
  ACT_365(365),
  THIRTY_360(360);

  private static final int MONTHS_PER_YEAR = 12;
  private static final int DAYS_PER_MONTH = 30;

  private final int yearDays;

  DayCountConvention(int yearDays) {
    this.yearDays = yearDays;
  }

  /**
   * Days between two dates under the convention.
   *
   * @param from start (exclusive)
   * @param to end (inclusive)
   * @return day count
   */
  public int days(LocalDate from, LocalDate to) {
    if (this == ACT_365) {
      return (int) ChronoUnit.DAYS.between(from, to);
    }
    int d1 = Math.min(from.getDayOfMonth(), DAYS_PER_MONTH);
    int d2 = Math.min(to.getDayOfMonth(), DAYS_PER_MONTH);
    int months =
        (to.getYear() - from.getYear()) * MONTHS_PER_YEAR
            + to.getMonthValue()
            - from.getMonthValue();
    return months * DAYS_PER_MONTH + d2 - d1;
  }

  /**
   * Days in the year of the convention (the denominator).
   *
   * @return 365 or 360
   */
  public int yearDays() {
    return yearDays;
  }
}
