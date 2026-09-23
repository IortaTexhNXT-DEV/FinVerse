package com.iortatechnxt.finverse.investment.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.finverse.investment.domain.HoldingTerms;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Interest, amortization and yield arithmetic for investment holdings (pure functions).
 *
 * <ul>
 *   <li>Coupon interest for (from, to] = face value x coupon rate x days / year days under the
 *       holding's day count convention ({@link
 *       com.iortatechnxt.finverse.investment.domain.DayCountConvention}).
 *   <li>Effective interest: the effective annual rate y is solved by bisection so that, stepping
 *       month end by month end from the start date, carrying amount + carrying amount x ((1 +
 *       y)^(actual days / 365) - 1) - coupon interest reaches face value at maturity. The
 *       exponential is evaluated in double precision; every posted amount is rounded to centavos.
 *   <li>Straight line: (face value - carrying amount) x actual days in period / actual days to
 *       maturity.
 *   <li>The period ending at maturity amortizes the whole remaining difference (rounding true-up).
 * </ul>
 *
 * <p>Rates are expressed in percent per annum (6.25 = 6.25 %).
 */
public final class InterestCalculator {

  private static final double YEAR = 365.0;
  private static final double PERCENT = 100.0;
  private static final double LOW_RATE = -50.0;
  private static final double HIGH_RATE = 200.0;
  private static final double TOLERANCE = 1e-10;
  private static final int MAX_ITERATIONS = 200;
  private static final int RATE_SCALE = 8;
  private static final int WORK_SCALE = 10;

  private InterestCalculator() {}

  /**
   * Coupon interest accrued over (from, to].
   *
   * @param t terms
   * @param from start (exclusive)
   * @param to end (inclusive)
   * @return interest, rounded to centavos
   */
  public static BigDecimal couponInterest(HoldingTerms t, LocalDate from, LocalDate to) {
    if (!to.isAfter(from) || t.couponRate().signum() == 0) {
      return Money.zero();
    }
    int days = t.dayCount().days(from, to);
    return Money.round(
        t.faceValue()
            .multiply(t.couponRate())
            .multiply(BigDecimal.valueOf(days))
            .divide(
                BigDecimal.valueOf(100L * t.dayCount().yearDays()),
                WORK_SCALE,
                RoundingMode.HALF_EVEN));
  }

  /**
   * Solves the effective annual rate (percent) that amortizes a carrying amount to face value at
   * maturity.
   *
   * @param t terms (must have a maturity date)
   * @param start date of the carrying amount
   * @param carrying carrying amount at start
   * @return effective annual rate in percent, 8 decimals
   */
  public static BigDecimal effectiveRate(HoldingTerms t, LocalDate start, BigDecimal carrying) {
    double low = LOW_RATE;
    double high = HIGH_RATE;
    double face = t.faceValue().doubleValue();
    double mid = 0;
    for (int i = 0; i < MAX_ITERATIONS && high - low > TOLERANCE; i++) {
      mid = (low + high) / 2;
      if (simulate(t, start, carrying.doubleValue(), mid) > face) {
        high = mid;
      } else {
        low = mid;
      }
    }
    return BigDecimal.valueOf(mid).setScale(RATE_SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Premium / discount amortization over (from, to]: positive accretes a discount, negative
   * amortizes a premium.
   *
   * @param t terms
   * @param ratePercent effective annual rate in percent (effective interest method)
   * @param carrying carrying amount at {@code from}
   * @param from start (exclusive)
   * @param to end (inclusive)
   * @return amortization, rounded to centavos
   */
  public static BigDecimal amortization(
      HoldingTerms t, BigDecimal ratePercent, BigDecimal carrying, LocalDate from, LocalDate to) {
    if (t.amortizationMethod() == AmortizationMethod.NONE
        || t.maturityDate() == null
        || !to.isAfter(from)) {
      return Money.zero();
    }
    BigDecimal remaining = t.faceValue().subtract(carrying);
    if (!to.isBefore(t.maturityDate())) {
      return Money.round(remaining);
    }
    long days = ChronoUnit.DAYS.between(from, to);
    if (t.amortizationMethod() == AmortizationMethod.STRAIGHT_LINE) {
      long toMaturity = ChronoUnit.DAYS.between(from, t.maturityDate());
      return Money.round(
          remaining
              .multiply(BigDecimal.valueOf(days))
              .divide(BigDecimal.valueOf(toMaturity), WORK_SCALE, RoundingMode.HALF_EVEN));
    }
    BigDecimal income =
        carrying.multiply(growth(ratePercent.doubleValue(), days), MathContext.DECIMAL64);
    return Money.round(income.subtract(couponInterest(t, from, to)));
  }

  /**
   * Carrying amount at a date, amortizing month by month from a start position (used to compute the
   * opening amortized cost of a take-on holding).
   *
   * @param t terms
   * @param ratePercent effective annual rate in percent
   * @param carrying carrying amount at start
   * @param start start date
   * @param date target date
   * @return carrying amount at the date
   */
  public static BigDecimal carryingAt(
      HoldingTerms t,
      BigDecimal ratePercent,
      BigDecimal carrying,
      LocalDate start,
      LocalDate date) {
    BigDecimal value = carrying;
    LocalDate previous = start;
    for (LocalDate step : monthEnds(start, date)) {
      value = value.add(amortization(t, ratePercent, value, previous, step));
      previous = step;
    }
    return value;
  }

  /**
   * Last coupon date on or before a date: the start of the current coupon period. Coupon dates are
   * counted back from maturity; instruments paying interest only at maturity accrue from
   * settlement.
   *
   * @param t terms
   * @param date date
   * @return start of the coupon period containing the date
   */
  public static LocalDate lastCouponDate(HoldingTerms t, LocalDate date) {
    int months = t.couponFrequency().months();
    if (months == 0 || t.maturityDate() == null) {
      return t.settlementDate();
    }
    LocalDate coupon = t.maturityDate();
    for (int n = 1; coupon.isAfter(date); n++) {
      coupon = t.maturityDate().minusMonths((long) months * n);
    }
    return coupon;
  }

  /**
   * Coupon payment dates in (from, to].
   *
   * @param t terms
   * @param from start (exclusive)
   * @param to end (inclusive)
   * @return coupon dates in ascending order
   */
  public static List<LocalDate> couponDates(HoldingTerms t, LocalDate from, LocalDate to) {
    List<LocalDate> dates = new ArrayList<>();
    if (t.maturityDate() == null || t.couponRate().signum() == 0) {
      return dates;
    }
    int months = t.couponFrequency().months();
    LocalDate coupon = t.maturityDate();
    for (int n = 1; coupon.isAfter(from); n++) {
      if (!coupon.isAfter(to)) {
        dates.add(0, coupon);
      }
      if (months == 0) {
        break;
      }
      coupon = t.maturityDate().minusMonths((long) months * n);
    }
    return dates;
  }

  /**
   * Month ends strictly after start and before end, followed by end.
   *
   * @param start start
   * @param end end
   * @return step dates
   */
  static List<LocalDate> monthEnds(LocalDate start, LocalDate end) {
    List<LocalDate> steps = new ArrayList<>();
    LocalDate step = YearMonth.from(start).atEndOfMonth();
    if (!step.isAfter(start)) {
      step = YearMonth.from(start).plusMonths(1).atEndOfMonth();
    }
    while (step.isBefore(end)) {
      steps.add(step);
      step = YearMonth.from(step).plusMonths(1).atEndOfMonth();
    }
    if (end.isAfter(start)) {
      steps.add(end);
    }
    return steps;
  }

  private static double simulate(HoldingTerms t, LocalDate start, double carrying, double rate) {
    double value = carrying;
    LocalDate previous = start;
    for (LocalDate step : monthEnds(start, t.maturityDate())) {
      long days = ChronoUnit.DAYS.between(previous, step);
      value +=
          value * growth(rate, days).doubleValue()
              - couponInterest(t, previous, step).doubleValue();
      previous = step;
    }
    return value;
  }

  private static BigDecimal growth(double ratePercent, long days) {
    return BigDecimal.valueOf(Math.pow(1 + ratePercent / PERCENT, days / YEAR) - 1);
  }
}
