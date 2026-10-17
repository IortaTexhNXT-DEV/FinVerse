package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The change window of production imports, as set in {@code CONFIG_PROMOTION_PRODUCTION_WINDOW}:
 * the days on which the window opens, then the hours, e.g. {@code SAT-SUN 20:00-06:00} or {@code
 * FRI,SAT 22:00-23:59}. An end before the start runs into the next day. Times are in the business
 * zone.
 *
 * @param days days on which the window opens
 * @param from opening time
 * @param to closing time
 */
public record ChangeWindow(Set<DayOfWeek> days, LocalTime from, LocalTime to) {

  private static final List<String> DAY_CODES =
      List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

  /** Defensive copy. */
  public ChangeWindow {
    days = Set.copyOf(days);
  }

  /**
   * Reads a window.
   *
   * @param text e.g. "SAT-SUN 20:00-06:00"
   * @return window
   */
  public static ChangeWindow parse(String text) {
    String[] parts = text == null ? new String[0] : text.trim().split("\\s+");
    if (parts.length != 2) {
      throw invalid(text);
    }
    String[] hours = parts[1].split("-");
    if (hours.length != 2) {
      throw invalid(text);
    }
    try {
      return new ChangeWindow(
          days(parts[0], text), LocalTime.parse(hours[0]), LocalTime.parse(hours[1]));
    } catch (DateTimeParseException e) {
      throw invalid(text, e);
    }
  }

  private static Set<DayOfWeek> days(String spec, String text) {
    Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    for (String item : spec.toUpperCase(Locale.ROOT).split(",")) {
      String[] range = item.split("-");
      int start = DAY_CODES.indexOf(range[0]);
      int end = DAY_CODES.indexOf(range[range.length - 1]);
      if (start < 0 || end < 0 || range.length > 2) {
        throw invalid(text);
      }
      for (int d = start; ; d = (d + 1) % DAY_CODES.size()) {
        days.add(DayOfWeek.of(d + 1));
        if (d == end) {
          break;
        }
      }
    }
    return days;
  }

  private static BusinessRuleException invalid(String text) {
    return invalid(text, null);
  }

  private static BusinessRuleException invalid(String text, Throwable cause) {
    return new BusinessRuleException(
        "CONFIG_WINDOW_INVALID",
        "The change window \""
            + text
            + "\" is not valid; write the days and the hours, for example"
            + " SAT-SUN 20:00-06:00",
        cause);
  }

  /**
   * Whether the window is open at a time.
   *
   * @param now business date and time
   * @return true when open
   */
  public boolean isOpen(LocalDateTime now) {
    LocalTime time = now.toLocalTime();
    if (!to.isBefore(from)) {
      return days.contains(now.getDayOfWeek()) && !time.isBefore(from) && !time.isAfter(to);
    }
    boolean lateToday = days.contains(now.getDayOfWeek()) && !time.isBefore(from);
    boolean earlyAfterYesterday = days.contains(now.getDayOfWeek().minus(1)) && !time.isAfter(to);
    return lateToday || earlyAfterYesterday;
  }
}
