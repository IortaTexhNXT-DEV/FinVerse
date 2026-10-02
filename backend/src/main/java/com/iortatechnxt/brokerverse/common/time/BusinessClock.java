package com.iortatechnxt.brokerverse.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The single source of the platform's business date.
 *
 * <p>Every business date (today, "not in the future" and cut-off checks, accounting periods, report
 * date keywords, job run dates, the business day of a recorded timestamp) is taken on the wall
 * clock of the business zone, {@code brokerverse.business-zone} (default {@value
 * #DEFAULT_ZONE_ID}). The UTC date is the previous day between 16:00 and 24:00 UTC, so a date taken
 * from the UTC clock would refuse a Manila "today" as a future date in the evening.
 *
 * <p>Timestamps ({@link Instant}, audit columns) stay in UTC; only the calendar date is taken in
 * the business zone. The injected {@link Clock} keeps tests in control of time: the methods read
 * the instant of the given clock and ignore its zone.
 *
 * <p>The zone is set once at start-up by {@link BusinessZoneSettings}, before any bean is created.
 * Read it with {@link #zone()} or {@link #zoneId()} where it is used, never into a static field,
 * and bind {@link #zoneId()} as a parameter of SQL that needs the business day of a timestamp. The
 * architecture test forbids {@code LocalDate.now}, {@code LocalDateTime.now}, {@code
 * LocalTime.now}, {@code ZonedDateTime.now}, {@code OffsetDateTime.now}, {@code YearMonth.now},
 * {@code Year.now} and {@code Instant.now} outside this class, and a {@code ZoneId} constant
 * anywhere.
 */
public final class BusinessClock {

  /** Property naming the business zone. */
  public static final String ZONE_PROPERTY = "brokerverse.business-zone";

  /** Business zone of the platform unless configured otherwise. */
  public static final String DEFAULT_ZONE_ID = "Asia/Manila";

  private static final AtomicReference<ZoneId> ZONE =
      new AtomicReference<>(ZoneId.of(DEFAULT_ZONE_ID));

  private BusinessClock() {}

  /**
   * The business zone.
   *
   * @return zone of the business day
   */
  public static ZoneId zone() {
    return ZONE.get();
  }

  /**
   * Identifier of the business zone, as PostgreSQL {@code at time zone} reads it.
   *
   * @return zone identifier, e.g. {@code Asia/Manila}
   */
  public static String zoneId() {
    return zone().getId();
  }

  /**
   * Today's business date.
   *
   * @param clock clock supplying the current instant
   * @return date in the business zone
   */
  public static LocalDate today(Clock clock) {
    return LocalDate.now(clock.withZone(zone()));
  }

  /**
   * The current moment on the wall clock of the business zone.
   *
   * @param clock clock supplying the current instant
   * @return date and time in the business zone
   */
  public static ZonedDateTime now(Clock clock) {
    return clock.instant().atZone(zone());
  }

  /**
   * The current business month.
   *
   * @param clock clock supplying the current instant
   * @return month in the business zone
   */
  public static YearMonth currentMonth(Clock clock) {
    return YearMonth.now(clock.withZone(zone()));
  }

  /**
   * The current business year.
   *
   * @param clock clock supplying the current instant
   * @return year in the business zone
   */
  public static Year currentYear(Clock clock) {
    return Year.now(clock.withZone(zone()));
  }

  /**
   * Business date of a recorded instant.
   *
   * @param instant timestamp, may be null
   * @return date in the business zone, or null
   */
  public static LocalDate dateOf(Instant instant) {
    return instant == null ? null : LocalDate.ofInstant(instant, zone());
  }

  /**
   * First instant of a business day, the lower bound of a date filter on timestamps.
   *
   * @param date business date
   * @return midnight of the date in the business zone
   */
  public static Instant startOf(LocalDate date) {
    return date.atStartOfDay(zone()).toInstant();
  }

  /**
   * Sets the business zone; called once at start-up.
   *
   * @param businessZone zone of the business day
   */
  static void use(ZoneId businessZone) {
    ZONE.set(Objects.requireNonNull(businessZone, "businessZone"));
  }
}
