package com.iortatechnxt.brokerverse.report.domain;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * A date period relative to the business date of a run (G04): the date parameters of a subscribed
 * report are set from it at every run, so a monthly subscription to the previous month always
 * reports the month just closed.
 */
public enum RelativePeriod {

  /** The day before the run. */
  PREVIOUS_DAY,
  /** From the first day of the month to the day of the run. */
  MONTH_TO_DATE,
  /** The whole previous month. */
  PREVIOUS_MONTH,
  /** From the first day of the quarter to the day of the run. */
  QUARTER_TO_DATE,
  /** The whole previous quarter. */
  PREVIOUS_QUARTER,
  /** From the first day of the year to the day of the run. */
  YEAR_TO_DATE,
  /** The whole previous year. */
  PREVIOUS_YEAR;

  private static final int QUARTER_MONTHS = 3;

  /**
   * First and last day of the period.
   *
   * @param from first day
   * @param to last day
   */
  public record Range(LocalDate from, LocalDate to) {}

  /**
   * The period for a business date.
   *
   * @param date business date of the run
   * @return first and last day
   */
  public Range on(LocalDate date) {
    LocalDate quarterStart = quarterStart(date);
    return switch (this) {
      case PREVIOUS_DAY -> new Range(date.minusDays(1), date.minusDays(1));
      case MONTH_TO_DATE -> new Range(date.withDayOfMonth(1), date);
      case PREVIOUS_MONTH -> {
        LocalDate start = date.withDayOfMonth(1).minusMonths(1);
        yield new Range(start, start.withDayOfMonth(start.lengthOfMonth()));
      }
      case QUARTER_TO_DATE -> new Range(quarterStart, date);
      case PREVIOUS_QUARTER ->
          new Range(quarterStart.minusMonths(QUARTER_MONTHS), quarterStart.minusDays(1));
      case YEAR_TO_DATE -> new Range(date.withDayOfYear(1), date);
      case PREVIOUS_YEAR -> {
        LocalDate start = date.withDayOfYear(1).minusYears(1);
        yield new Range(start, start.withDayOfYear(start.lengthOfYear()));
      }
    };
  }

  private static LocalDate quarterStart(LocalDate date) {
    int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
    return LocalDate.of(date.getYear(), (quarter - 1) * QUARTER_MONTHS + 1, 1);
  }
}
