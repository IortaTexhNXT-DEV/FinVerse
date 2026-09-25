package com.iortatechnxt.brokerverse.collections.files.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The periods and availability times of the Collections files (BRCLXN.024-029), in Philippine time:
 * the Saturday-to-Friday week available the following Monday at 08:00, and the previous month
 * available on the first working day at 08:00.
 */
public final class FilePeriods {

  /** Philippine time. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private static final LocalTime OFFICE_OPENS = LocalTime.of(8, 0);
  private static final int WEEK_DAYS = 7;

  private FilePeriods() {}

  /**
   * The week (Saturday to Friday) that ends on the last Friday on or before a day (BRCLXN.028).
   *
   * @param day any day
   * @return the week
   */
  public static Period weekEnding(LocalDate day) {
    LocalDate to = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
    LocalDate from = to.minusDays(WEEK_DAYS - 1L);
    String key =
        String.format(
            Locale.ROOT,
            "%d-W%02d",
            to.get(IsoFields.WEEK_BASED_YEAR),
            to.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    return new Period(key, from, to);
  }

  /**
   * The Monday 08:00 after a week (BRCLXN.029).
   *
   * @param week the week
   * @return availability time
   */
  public static Instant mondayAfter(Period week) {
    return week.to()
        .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        .atTime(OFFICE_OPENS)
        .atZone(MANILA)
        .toInstant();
  }

  /**
   * The month before a day (BRCLXN.024-027).
   *
   * @param day any day
   * @return the previous month
   */
  public static Period previousMonth(LocalDate day) {
    YearMonth month = YearMonth.from(day).minusMonths(1);
    return new Period(month.toString(), month.atDay(1), month.atEndOfMonth());
  }

  /**
   * Whether a day is the first working day of its month (BRCLXN.025/027).
   *
   * @param day day
   * @param working working-day calendar
   * @return true on the first working day
   */
  public static boolean isFirstWorkingDay(LocalDate day, Predicate<LocalDate> working) {
    if (!working.test(day)) {
      return false;
    }
    for (LocalDate d = day.withDayOfMonth(1); d.isBefore(day); d = d.plusDays(1)) {
      if (working.test(d)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 08:00 of a day, Philippine time.
   *
   * @param day day
   * @return instant
   */
  public static Instant officeOpens(LocalDate day) {
    return day.atTime(OFFICE_OPENS).atZone(MANILA).toInstant();
  }

  /**
   * A file period.
   *
   * @param key period key (2026-W39, 2026-08, 2026-09-24)
   * @param from first day
   * @param to last day
   */
  public record Period(String key, LocalDate from, LocalDate to) {}
}
