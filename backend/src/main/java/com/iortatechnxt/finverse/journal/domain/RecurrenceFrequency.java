package com.iortatechnxt.finverse.journal.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How often a recurring journal template generates a journal. */
public enum RecurrenceFrequency {
  MONTHLY(1),
  QUARTERLY(3),
  ANNUALLY(12);

  private final int months;

  RecurrenceFrequency(int months) {
    this.months = months;
  }

  public int months() {
    return months;
  }

  /**
   * Occurrence dates of a schedule up to (and including) a date.
   *
   * <p>Occurrences fall in the start month and every {@link #months()} months after it, on {@code
   * dayOfMonth} or the last day of shorter months (31 = always month end). Dates before the start
   * date or after the end date are excluded.
   *
   * @param dayOfMonth day of month 1-31
   * @param start first possible date
   * @param end last possible date (null = open ended)
   * @param upTo last date to generate
   * @return ascending occurrence dates
   */
  public List<LocalDate> occurrences(
      int dayOfMonth, LocalDate start, LocalDate end, LocalDate upTo) {
    LocalDate limit = end != null && end.isBefore(upTo) ? end : upTo;
    List<LocalDate> result = new ArrayList<>();
    YearMonth month = YearMonth.from(start);
    LocalDate candidate = dateIn(month, dayOfMonth);
    while (!candidate.isAfter(limit)) {
      if (!candidate.isBefore(start)) {
        result.add(candidate);
      }
      month = month.plusMonths(months);
      candidate = dateIn(month, dayOfMonth);
    }
    return result;
  }

  /**
   * First occurrence strictly after a date.
   *
   * @param dayOfMonth day of month
   * @param start schedule start
   * @param end schedule end (null = open ended)
   * @param after reference date
   * @return next occurrence, or null when the schedule has ended
   */
  public LocalDate nextAfter(int dayOfMonth, LocalDate start, LocalDate end, LocalDate after) {
    YearMonth month = YearMonth.from(start);
    LocalDate candidate = dateIn(month, dayOfMonth);
    while (candidate.isBefore(start) || !candidate.isAfter(after)) {
      month = month.plusMonths(months);
      candidate = dateIn(month, dayOfMonth);
    }
    return end != null && candidate.isAfter(end) ? null : candidate;
  }

  private static LocalDate dateIn(YearMonth month, int dayOfMonth) {
    return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
  }
}
