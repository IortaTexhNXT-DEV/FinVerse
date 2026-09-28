package com.iortatechnxt.brokerverse.common.util;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * How dates and numbers are written for people (UX checklist: dd-MMM-yyyy, amounts with thousands
 * separators and two decimals): generated documents, human-readable renders of files and texts
 * built on the server. Files read by another system keep the layout agreed with that system.
 */
public final class DisplayFormat {

  /** Date pattern shown to users, e.g. 17-Oct-2026. */
  public static final String DATE_PATTERN = "dd-MMM-yyyy";

  /** Date format of spreadsheet cells, e.g. 17-Oct-2026 (the cell keeps a real date). */
  public static final String SHEET_DATE_FORMAT = "dd-mmm-yyyy";

  /** Number format of amount cells in spreadsheets, e.g. 80,000,000.00. */
  public static final String SHEET_AMOUNT_FORMAT = "#,##0.00";

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ENGLISH);
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern(DATE_PATTERN + " HH:mm", Locale.ENGLISH);
  private static final int RATE_DECIMALS = 4;

  /** Codes kept in capitals when a status is written as words (DV_ASSIGNED is "DV assigned"). */
  private static final Set<String> ACRONYMS =
      Set.of(
          "AP", "AR", "ARN", "BDOI", "BIR", "CBG", "CPC2", "CWT", "DST", "DTIP", "DV", "EOD", "FFY",
          "IA", "KYC", "OR", "OTC", "PDC", "PN", "PRF", "PS", "QS", "SI", "TSU", "VAT");

  private DisplayFormat() {}

  /**
   * A status or other code written as words inside a sentence: READY_FOR_PLACEMENT becomes "ready
   * for placement", QS_SENT becomes "QS sent".
   *
   * @param code enum or code, may be null
   * @return words, empty when null
   */
  public static String words(Object code) {
    if (code == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (String part : code.toString().split("_")) {
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(ACRONYMS.contains(part) ? part : part.toLowerCase(Locale.ROOT));
    }
    return out.toString();
  }

  /**
   * A date as dd-MMM-yyyy.
   *
   * @param date date, may be null
   * @return text, empty when null
   */
  public static String date(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }

  /**
   * A point in time as dd-MMM-yyyy HH:mm in the business zone (Philippine time).
   *
   * @param instant instant, may be null
   * @return text, empty when null
   */
  public static String dateTime(Instant instant) {
    return instant == null ? "" : DATE_TIME.format(instant.atZone(BusinessClock.zone()));
  }

  /**
   * A period, e.g. "01-Nov-2026 to 01-Nov-2027"; "from 01-Nov-2026" or "to 01-Nov-2027" when one
   * end is open, empty when neither is known (the same rule as the screens' formatPeriod).
   *
   * @param from first day, may be null
   * @param to last day, may be null
   * @return text, empty when both are null
   */
  public static String period(LocalDate from, LocalDate to) {
    if (from == null) {
      return to == null ? "" : "to " + date(to);
    }
    return to == null ? "from " + date(from) : date(from) + " to " + date(to);
  }

  /**
   * An amount with thousands separators and two decimals, e.g. 80,000,000.00.
   *
   * @param amount amount, may be null
   * @return text, empty when null
   */
  public static String amount(BigDecimal amount) {
    if (amount == null) {
      return "";
    }
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        .format(amount.setScale(Money.SCALE, RoundingMode.HALF_UP));
  }

  /**
   * A rate in percent as users read it: at least two and at most four decimals (0.42500000 becomes
   * 0.425, 1.1 becomes 1.10, 0.123456 becomes 0.1235), the precision rates are keyed on the
   * screens; the column header or the caller adds the percent sign.
   *
   * @param rate rate in percent, may be null
   * @return text without the percent sign, empty when null
   */
  public static String rate(BigDecimal rate) {
    if (rate == null) {
      return "";
    }
    BigDecimal rounded = rate.setScale(RATE_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros();
    return rounded.setScale(Math.max(2, rounded.scale()), RoundingMode.UNNECESSARY).toPlainString();
  }

  /**
   * Any value as users read it: dates as dd-MMM-yyyy, amounts (two decimals) with thousands
   * separators, other decimals without trailing zeros, other values as text.
   *
   * @param value value, may be null
   * @return text, empty when null
   */
  public static String value(Object value) {
    return switch (value) {
      case null -> "";
      case LocalDate d -> date(d);
      case Instant i -> dateTime(i);
      case BigDecimal n when n.scale() == Money.SCALE -> amount(n);
      case BigDecimal n -> n.stripTrailingZeros().toPlainString();
      default -> value.toString();
    };
  }
}
