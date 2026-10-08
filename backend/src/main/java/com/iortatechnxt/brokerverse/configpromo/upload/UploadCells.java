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
   * A date range "from; to" (or "from / to"), to blank for an open range.
   *
   * @param cell cell
   * @return from and to (to may be null), null when the cell is not a date range
   */
  static LocalDate[] dates(String cell) {
    if (cell == null) {
      return null;
    }
    String[] parts = RANGE_SEPARATOR.split(cell.trim(), -1);
    if (parts.length > 2) {
      return null;
    }
    LocalDate from = date(parts[0]);
    LocalDate to = parts.length == 2 ? date(parts[1]) : null;
    boolean toGiven = parts.length == 2 && clean(parts[1]) != null;
    if (from == null || toGiven && to == null || to != null && to.isBefore(from)) {
      return null;
    }
    return new LocalDate[] {from, to};
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
    String day = t.length() > 10 && t.charAt(4) == '-' ? t.substring(0, 10) : t;
    try {
      return day.charAt(4) == '-' ? LocalDate.parse(day) : LocalDate.parse(day, SHOWN);
    } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
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
   * @return true, false, or null when neither
   */
  static Boolean yes(String text) {
    String t = clean(text);
    if (t == null) {
      return null;
    }
    return switch (t.toUpperCase(Locale.ROOT)) {
      case "Y", "YES" -> Boolean.TRUE;
      case "N", "NO" -> Boolean.FALSE;
      default -> null;
    };
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
    if (v instanceof BigDecimal n) {
      return n.stripTrailingZeros().toPlainString();
    }
    if (v instanceof Boolean b) {
      return b ? "Y" : "N";
    }
    if (v instanceof java.sql.Date d) {
      return d.toLocalDate().format(SHOWN);
    }
    if (v instanceof LocalDate d) {
      return d.format(SHOWN);
    }
    return String.valueOf(v);
  }

  private static String clean(String part) {
    if (part == null) {
      return null;
    }
    String t = part.trim();
    return t.isEmpty() || DASH.equals(t) ? null : t;
  }
}
