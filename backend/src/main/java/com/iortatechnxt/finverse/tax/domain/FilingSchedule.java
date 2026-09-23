package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Filing periods and due dates of a form (pure calendar arithmetic).
 *
 * <p>Due date = day {@code dueDay} of the month that is {@code dueMonthsAfter} months after the
 * month in which the period ends; a day beyond the month's length means the last day of that month
 * (e.g. 1601-EQ "last day of the month following the quarter" is stored as day 31). Due dates
 * falling on weekends or holidays are not moved: BIR regulations move them to the next working day
 * only for manual filing, and electronic filers are expected to file on or before the date
 * (assumption).
 *
 * @param frequency filing frequency
 * @param dueMonthsAfter months after the period-end month
 * @param dueDay day of month (1 to 31)
 */
public record FilingSchedule(FilingFrequency frequency, int dueMonthsAfter, int dueDay) {

  private static final int MONTHS_PER_QUARTER = 3;
  private static final int MONTHS_PER_YEAR = 12;

  /**
   * Due date of a period ending on a date.
   *
   * @param periodEnd last day of the period
   * @return due date
   */
  public LocalDate dueDate(LocalDate periodEnd) {
    YearMonth month = YearMonth.from(periodEnd).plusMonths(dueMonthsAfter);
    return month.atDay(Math.min(dueDay, month.lengthOfMonth()));
  }

  /**
   * Filing periods of a calendar year, in order.
   *
   * @param year year
   * @return periods
   */
  public List<TaxPeriod> periods(int year) {
    List<TaxPeriod> out = new ArrayList<>();
    switch (frequency) {
      case MONTHLY -> {
        for (int m = 1; m <= MONTHS_PER_YEAR; m++) {
          out.add(TaxPeriod.month(YearMonth.of(year, m)));
        }
      }
      case MONTHLY_EXCEPT_QUARTER_END -> {
        for (int m = 1; m <= MONTHS_PER_YEAR; m++) {
          if (m % MONTHS_PER_QUARTER != 0) {
            out.add(TaxPeriod.month(YearMonth.of(year, m)));
          }
        }
      }
      case QUARTERLY -> {
        for (int q = 1; q <= MONTHS_PER_YEAR / MONTHS_PER_QUARTER; q++) {
          out.add(TaxPeriod.quarter(year, q));
        }
      }
      default ->
          out.add(
              new TaxPeriod(
                  LocalDate.of(year, 1, 1), YearMonth.of(year, MONTHS_PER_YEAR).atEndOfMonth()));
    }
    return out;
  }

  /**
   * The filing period of this schedule that starts on a date.
   *
   * @param start first day of the period
   * @return period
   * @throws BusinessRuleException when no filing period of the form starts on that date
   */
  public TaxPeriod periodStarting(LocalDate start) {
    return periods(start.getYear()).stream()
        .filter(p -> p.from().equals(start))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "INVALID_FILING_PERIOD",
                    "No " + frequency + " filing period starts on " + start));
  }
}
