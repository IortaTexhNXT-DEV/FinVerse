package com.iortatechnxt.brokerverse.nbadmin.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Working hours of the out-of-hours risk flag (UAM-NFR-40, parameter {@code UAM_WORKING_HOURS},
 * UQ07), in Philippine time. The format is {@code HH:mm-HH:mm,DAY-DAY} (for example {@code
 * 08:00-18:00,MON-FRI}); {@code 24:00} ends at midnight. A blank or unreadable value means "always
 * within working hours", so a wrong parameter never blocks approvals.
 *
 * @param start first minute within working hours
 * @param end first minute after working hours; null for midnight
 * @param days working days
 */
public record WorkingHours(LocalTime start, LocalTime end, Set<DayOfWeek> days) {

  /** Time zone of BDOI. */
  public static final ZoneId ZONE = ZoneId.of("Asia/Manila");

  /** Always within working hours. */
  public static final WorkingHours ALWAYS =
      new WorkingHours(LocalTime.MIDNIGHT, null, EnumSet.allOf(DayOfWeek.class));

  private static final String MIDNIGHT_END = "24:00";

  /** Defensive copy. */
  public WorkingHours {
    days = days.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
  }

  /**
   * Reads the parameter value.
   *
   * @param value parameter value
   * @return working hours, {@link #ALWAYS} when blank or unreadable
   */
  public static WorkingHours parse(String value) {
    if (value == null || value.isBlank()) {
      return ALWAYS;
    }
    String[] parts = value.trim().toUpperCase(Locale.ROOT).split(",", 2);
    String[] hours = parts[0].trim().split("-");
    if (hours.length != 2) {
      return ALWAYS;
    }
    try {
      LocalTime from = LocalTime.parse(hours[0].trim());
      LocalTime to = MIDNIGHT_END.equals(hours[1].trim()) ? null : LocalTime.parse(hours[1].trim());
      Set<DayOfWeek> days =
          parts.length < 2 ? EnumSet.allOf(DayOfWeek.class) : days(parts[1].trim());
      return new WorkingHours(from, to, days);
    } catch (DateTimeParseException | IllegalArgumentException e) {
      return ALWAYS;
    }
  }

  private static Set<DayOfWeek> days(String spec) {
    String[] range = spec.split("-");
    if (range.length == 2) {
      DayOfWeek first = day(range[0]);
      DayOfWeek last = day(range[1]);
      Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
      for (DayOfWeek d = first; d != last; d = d.plus(1)) {
        result.add(d);
      }
      result.add(last);
      return result;
    }
    Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
    for (String d : spec.split("[ ,;/]+")) {
      result.add(day(d));
    }
    return result;
  }

  private static DayOfWeek day(String abbreviation) {
    String a = abbreviation.trim();
    for (DayOfWeek d : DayOfWeek.values()) {
      if (d.name().startsWith(a) && a.length() >= 2) {
        return d;
      }
    }
    throw new IllegalArgumentException("Unknown day " + abbreviation);
  }

  /**
   * Whether a moment is within working hours.
   *
   * @param moment the moment (converted to Philippine time)
   * @return true within working hours
   */
  public boolean contains(ZonedDateTime moment) {
    ZonedDateTime local = moment.withZoneSameInstant(ZONE);
    LocalTime time = local.toLocalTime();
    return days.contains(local.getDayOfWeek())
        && !time.isBefore(start)
        && (end == null || time.isBefore(end));
  }
}
