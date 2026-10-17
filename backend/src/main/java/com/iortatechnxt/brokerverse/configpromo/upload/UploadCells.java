package com.iortatechnxt.brokerverse.configpromo.upload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Cells of the workbook that hold several values, e.g. "50; Y" (order and active) or "01-Jan-2028;
 * 31-Dec-2028" (effective from and to): the parts are separated by semicolons; a blank part or a
 * dash is an empty value.
 */
final class UploadCells {

  private static final Pattern SEPARATOR = Pattern.compile(";");
  private static final Pattern RANGE_SEPARATOR = Pattern.compile(";|\\s+/\\s+|\\s+to\\s+");
  private static final DateTimeFormatter SHOWN =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("d-MMM-uuuu")
          .toFormatter(Locale.ENGLISH);
  private static final String DASH = "-";
  private static final java.util.Set<String> YES = java.util.Set.of("Y", "YES");
  private static final java.util.Set<String> NO = java.util.Set.of("N", "NO");
  private static final Pattern ISO_DAY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final int ISO_LENGTH = 10;

  private UploadCells() {}

  /**
   * The parts of a cell.
   *
   * @param cell cell, may be null
   * @param count number of parts expected (missing parts are null)
   * @return parts, null for a blank part
   */
  static List<String> parts(String cell, int count) {
    List<String> parts = new ArrayList<>();
    if (cell != null) {
      Arrays.stream(SEPARATOR.split(cell, -1)).map(UploadCells::clean).forEach(parts::add);
    }
    while (parts.size() < count) {
      parts.add(null);
    }
    return parts;
  }

  /**
   * The number of parts of a cell.
   *
   * @param cell cell
   * @return count, 0 for a blank cell
   */
  static int count(String cell) {
    return cell == null || cell.isBlank() ? 0 : SEPARATOR.split(cell, -1).length;
  }

  /**
   * A period of validity.
   *
   * @param from first day
   * @param to last day, null for an open period
   */
  record Period(LocalDate from, LocalDate to) {}

  /**
   * A date range "from; to" (or "from / to"), to blank for an open range.
   *
   * @param cell cell
   * @return the period, empty when the cell is not a date range
   */
  static Optional<Period> period(String cell) {
    String[] parts = cell == null ? new String[0] : RANGE_SEPARATOR.split(cell.trim(), -1);
    if (parts.length == 0 || parts.length > 2) {
      return Optional.empty();
    }
    LocalDate from = date(parts[0]);
    String toText = parts.length == 2 ? clean(parts[1]) : null;
    LocalDate to = date(toText);
    return from != null && (toText == null || to != null && !to.isBefore(from))
        ? Optional.of(new Period(from, to))
        : Optional.empty();
  }

  /**
   * The period of a validated row.
   *
   * @param cell cell checked with {@link #period(String)}
   * @return the period
   */
  static Period periodOf(String cell) {
    return period(cell).orElseThrow();
  }

  /**
   * A date as dd-MMM-yyyy or yyyy-MM-dd (with an optional time of day, as Excel writes it).
   *
   * @param text text
   * @return date, null when blank or not a date
   */
  static LocalDate date(String text) {
    String t = clean(text);
    if (t == null) {
      return null;
    }
    boolean iso = ISO_DAY.matcher(t).lookingAt();
    try {
      return iso ? LocalDate.parse(t.substring(0, ISO_LENGTH)) : LocalDate.parse(t, SHOWN);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /**
   * A number.
   *
   * @param text text
   * @return number, null when blank or not a number
   */
  static BigDecimal number(String text) {
    String t = clean(text);
    if (t == null) {
      return null;
    }
    try {
      return new BigDecimal(t.replace(",", ""));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Y or N.
   *
   * @param text text
   * @return true or false, empty when neither
   */
  static Optional<Boolean> yes(String text) {
    String t = clean(text);
    String upper = t == null ? "" : t.toUpperCase(Locale.ROOT);
    if (YES.contains(upper)) {
      return Optional.of(Boolean.TRUE);
    }
    return NO.contains(upper) ? Optional.of(Boolean.FALSE) : Optional.empty();
  }

  /**
   * A date range as the workbook shows it.
   *
   * @param from from
   * @param to to, may be null
   * @return text
   */
  static String range(Object from, Object to) {
    return to == null ? text(from) : text(from) + "; " + text(to);
  }

  /**
   * Parts joined with semicolons.
   *
   * @param parts values, null for blank
   * @return text
   */
  static String join(Object... parts) {
    List<String> texts = new ArrayList<>();
    for (Object p : parts) {
      texts.add(p == null ? DASH : text(p));
    }
    return String.join("; ", texts);
  }

  private static String text(Object v) {
    Object value = v instanceof java.sql.Date d ? d.toLocalDate() : v;
    return switch (value) {
      case BigDecimal n -> n.stripTrailingZeros().toPlainString();
      case Boolean b -> Boolean.TRUE.equals(b) ? "Y" : "N";
      case LocalDate d -> d.format(SHOWN);
      case null -> "null";
      default -> String.valueOf(value);
    };
  }

  private static String clean(String part) {
    if (part == null) {
      return null;
    }
    String t = part.trim();
    return t.isEmpty() || DASH.equals(t) ? null : t;
  }
}
