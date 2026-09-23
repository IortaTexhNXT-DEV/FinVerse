package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * A taxable period: a calendar month, a calendar quarter or a calendar year. Philippine returns are
 * filed on calendar periods (a fiscal-year taxpayer still files VAT and withholding returns on
 * calendar quarters).
 *
 * @param from first day
 * @param to last day
 */
public record TaxPeriod(LocalDate from, LocalDate to) {

  private static final int MONTHS_PER_QUARTER = 3;
  private static final int LAST_QUARTER = 4;

  /** Validates the bounds. */
  public TaxPeriod {
    if (from == null || to == null || to.isBefore(from)) {
      throw new BusinessRuleException("INVALID_TAX_PERIOD", "Tax period end precedes its start");
    }
  }

  /**
   * A calendar month.
   *
   * @param month month
   * @return period
   */
  public static TaxPeriod month(YearMonth month) {
    return new TaxPeriod(month.atDay(1), month.atEndOfMonth());
  }

  /**
   * A calendar quarter.
   *
   * @param year year
   * @param quarter 1 to 4
   * @return period
   */
  public static TaxPeriod quarter(int year, int quarter) {
    if (quarter < 1 || quarter > LAST_QUARTER) {
      throw new BusinessRuleException("INVALID_QUARTER", "Quarter must be 1 to 4");
    }
    YearMonth first = YearMonth.of(year, (quarter - 1) * MONTHS_PER_QUARTER + 1);
    return new TaxPeriod(first.atDay(1), first.plusMonths(2).atEndOfMonth());
  }

  /**
   * The calendar quarter containing a date.
   *
   * @param date date
   * @return quarter period
   */
  public static TaxPeriod quarterOf(LocalDate date) {
    return quarter(date.getYear(), quarterNumber(date));
  }

  /**
   * Quarter number of a date.
   *
   * @param date date
   * @return 1 to 4
   */
  public static int quarterNumber(LocalDate date) {
    return (date.getMonthValue() - 1) / MONTHS_PER_QUARTER + 1;
  }

  /**
   * Month of the period (1, 2 or 3 for a quarter) in which a date falls: column of the income
   * payment on BIR Form 2307 and of the QAP.
   *
   * @param date date inside the period
   * @return 1-based month index
   */
  public int monthIndex(LocalDate date) {
    if (!contains(date)) {
      throw new BusinessRuleException("DATE_OUTSIDE_PERIOD", date + " is outside " + label());
    }
    return (int) YearMonth.from(from).until(YearMonth.from(date), ChronoUnit.MONTHS) + 1;
  }

  /**
   * Whether a date falls in the period.
   *
   * @param date date
   * @return true when from ≤ date ≤ to
   */
  public boolean contains(LocalDate date) {
    return !date.isBefore(from) && !date.isAfter(to);
  }

  /**
   * Whether the period is exactly one calendar quarter.
   *
   * @return true for a quarter
   */
  public boolean isQuarter() {
    return from.getDayOfMonth() == 1
        && (from.getMonthValue() - 1) % MONTHS_PER_QUARTER == 0
        && to.equals(YearMonth.from(from).plusMonths(2).atEndOfMonth());
  }

  /**
   * The previous period of the same length in months (the previous quarter of a quarter).
   *
   * @return previous period
   */
  public TaxPeriod previous() {
    YearMonth start = YearMonth.from(from);
    long months = start.until(YearMonth.from(to), ChronoUnit.MONTHS) + 1;
    YearMonth previousStart = start.minusMonths(months);
    return new TaxPeriod(previousStart.atDay(1), start.minusMonths(1).atEndOfMonth());
  }

  /**
   * Readable label, e.g. "2026-Q1", "2026-03" or "2026-01-01..2026-02-15".
   *
   * @return label
   */
  public String label() {
    if (isQuarter()) {
      return from.getYear() + "-Q" + quarterNumber(from);
    }
    if (from.getDayOfMonth() == 1 && to.equals(YearMonth.from(from).atEndOfMonth())) {
      return YearMonth.from(from).toString();
    }
    return from + ".." + to;
  }
}
