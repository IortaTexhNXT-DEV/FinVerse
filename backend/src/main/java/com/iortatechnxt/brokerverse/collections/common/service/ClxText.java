package com.iortatechnxt.brokerverse.collections.common.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Values in the texts of the Collections notices as the screens show them (screen standards): dates
 * as dd-MMM-yyyy, times as dd-MMM-yyyy HH:mm in Philippine time, amounts with two decimals and
 * thousand separators after the currency, codes in words.
 */
public final class ClxText {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);

  private ClxText() {}

  /**
   * A date as the screens show it.
   *
   * @param date date, may be null
   * @return 05-Oct-2026, or an empty text
   */
  public static String date(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }

  /**
   * A time in the business time zone as the screens show it.
   *
   * @param instant time, may be null
   * @return 05-Oct-2026 08:00, or an empty text
   */
  public static String dateTime(Instant instant) {
    return instant == null ? "" : DATE_TIME.format(instant.atZone(BusinessClock.zone()));
  }

  /**
   * An amount with its currency, two decimals and thousand separators.
   *
   * @param currency currency code, may be null
   * @param amount amount, may be null
   * @return PHP 22,268.75
   */
  public static String amount(String currency, BigDecimal amount) {
    DecimalFormat format =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    String value = amount == null ? "" : format.format(amount.setScale(2, RoundingMode.HALF_UP));
    return currency == null || currency.isBlank() ? value : currency + " " + value;
  }

  /**
   * A code in words, first letter capital: APPLY_TO_INVOICE as "Apply to invoice".
   *
   * @param code code, may be null
   * @return the words, or an empty text
   */
  public static String words(String code) {
    if (code == null || code.isBlank()) {
      return "";
    }
    String text = code.replace('_', ' ').toLowerCase(Locale.ROOT).strip();
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
