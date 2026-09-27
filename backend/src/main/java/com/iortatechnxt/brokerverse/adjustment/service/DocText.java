package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/** Text formatting of the adjustment documents and reports. */
public final class DocText {

  /** Placeholder of an empty value. */
  public static final String NONE = "-";

  private static final ZoneId MANILA = BusinessClock.zone();

  private DocText() {}

  /**
   * A value as people read it (dates dd-MMM-yyyy), or the placeholder.
   *
   * @param value value
   * @return text
   */
  public static String text(Object value) {
    return value == null ? NONE : DisplayFormat.value(value);
  }

  /**
   * An amount with thousands separators and two decimals.
   *
   * @param value amount
   * @return text
   */
  public static String amount(BigDecimal value) {
    if (value == null) {
      return NONE;
    }
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }

  /**
   * The Philippine date of an instant.
   *
   * @param at instant
   * @return date, null when none
   */
  public static LocalDate date(Instant at) {
    return at == null ? null : at.atZone(MANILA).toLocalDate();
  }
}
