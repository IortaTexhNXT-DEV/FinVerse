package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.reserves.domain.EarningUnits;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 * Earning units of a cover period at a valuation date under the three UPR bases.
 *
 * <ul>
 *   <li><b>1/365 (DAYS_365)</b>, spec PGIBR072: units are days. Total = days of cover (both dates
 *       inclusive); earned = days from the start up to and including the valuation date; unearned =
 *       total − earned. A leap year simply has a 366-day cover.
 *   <li><b>1/24 (TWENTY_FOURTHS)</b>: every transaction is assumed written in the middle of its
 *       first month. Total = 2 × cover months; earned at the end of the e-th month of cover = 2e −
 *       1 (so a 12-month policy is 1/24 earned after its first month and 23/24 after its twelfth).
 *   <li><b>1/8 (EIGHTHS)</b>: the same rule by calendar quarters: total = 2 × cover quarters,
 *       earned at the end of the e-th quarter = 2e − 1.
 * </ul>
 *
 * <p>A cover whose end is before its start (e.g. a cancellation effective after expiry) has no
 * units and no UPR. Before the cover starts nothing is earned.
 */
public final class UprMath {

  private static final int MONTHS_PER_QUARTER = 3;
  private static final int QUARTERS_PER_YEAR = 4;

  private UprMath() {}

  /**
   * Earning units at a valuation date.
   *
   * @param basis earning basis
   * @param from first day of cover
   * @param to last day of cover
   * @param valuationDate valuation date
   * @return units
   */
  public static EarningUnits units(
      UprBasis basis, LocalDate from, LocalDate to, LocalDate valuationDate) {
    if (to.isBefore(from)) {
      return EarningUnits.of(0, 0);
    }
    boolean started = !valuationDate.isBefore(from);
    return switch (basis) {
      case DAYS_365 -> days(from, to, valuationDate);
      case TWENTY_FOURTHS ->
          halves(months(from, to), started ? monthIndex(valuationDate) - monthIndex(from) + 1 : 0);
      case EIGHTHS ->
          halves(
              ceilDiv(months(from, to), MONTHS_PER_QUARTER),
              started ? quarterIndex(valuationDate) - quarterIndex(from) + 1 : 0);
    };
  }

  private static EarningUnits days(LocalDate from, LocalDate to, LocalDate valuationDate) {
    int total = (int) ChronoUnit.DAYS.between(from, to) + 1;
    int earned = (int) ChronoUnit.DAYS.between(from, valuationDate) + 1;
    return EarningUnits.of(total, earned);
  }

  /** Units in halves of a period: total 2n, earned 2e − 1 in the e-th period of cover. */
  private static EarningUnits halves(int periods, int elapsed) {
    int earned = elapsed <= 0 ? 0 : 2 * elapsed - 1;
    return EarningUnits.of(2 * periods, earned);
  }

  /** Cover length in months, a started month counting as a whole one (at least one). */
  static int months(LocalDate from, LocalDate to) {
    LocalDate end = to.plusDays(1);
    long whole = ChronoUnit.MONTHS.between(from, end);
    boolean partial = from.plusMonths(whole).isBefore(end);
    return (int) Math.max(1, whole + (partial ? 1 : 0));
  }

  private static int monthIndex(LocalDate date) {
    return (int) YearMonth.from(date).getLong(ChronoField.PROLEPTIC_MONTH);
  }

  private static int quarterIndex(LocalDate date) {
    return date.getYear() * QUARTERS_PER_YEAR + (date.getMonthValue() - 1) / MONTHS_PER_QUARTER;
  }

  private static int ceilDiv(int value, int divisor) {
    return (value + divisor - 1) / divisor;
  }
}
