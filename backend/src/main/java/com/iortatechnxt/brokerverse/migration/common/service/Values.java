package com.iortatechnxt.brokerverse.migration.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Parsing of the values of the extract layouts (DATA_MIGRATION_DESIGN section 5.1): dates {@code
 * yyyy-MM-dd}, timestamps {@code yyyy-MM-dd HH:mm:ss}, amounts with a dot decimal and at most 2
 * decimals, flags Y / N, lists separated by semicolons. Blank means no value.
 */
public final class Values {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int AMOUNT_SCALE = 2;

  private Values() {}

  /**
   * Whether a value is blank.
   *
   * @param value value
   * @return true for null or blank
   */
  public static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * A date.
   *
   * @param value value
   * @return date, empty when blank or invalid
   */
  public static Optional<LocalDate> date(String value) {
    if (blank(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(value.strip()));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /**
   * A timestamp.
   *
   * @param value value
   * @return timestamp, empty when blank or invalid
   */
  public static Optional<LocalDateTime> stamp(String value) {
    if (blank(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDateTime.parse(value.strip(), STAMP));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /**
   * A decimal number.
   *
   * @param value value
   * @return number, empty when blank or invalid
   */
  public static Optional<BigDecimal> decimal(String value) {
    if (blank(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(new BigDecimal(value.strip()));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  /**
   * An amount (0 when blank).
   *
   * @param value value
   * @return amount with 2 decimals
   */
  public static BigDecimal amount(String value) {
    return decimal(value).orElse(BigDecimal.ZERO).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * Whether a value is a valid amount: dot decimal, at most 2 decimals, no thousands separator.
   *
   * @param value value
   * @return true when valid
   */
  public static boolean validAmount(String value) {
    return value != null && value.strip().matches("-?\\d+(\\.\\d{1,2})?");
  }

  /**
   * A Y / N flag.
   *
   * @param value value
   * @return true for Y
   */
  public static boolean flag(String value) {
    return value != null && "Y".equalsIgnoreCase(value.strip());
  }

  /**
   * Items of a list separated by semicolons.
   *
   * @param value value
   * @return items
   */
  public static List<String> items(String value) {
    if (blank(value)) {
      return List.of();
    }
    return Arrays.stream(value.split("[;,]")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }

  /**
   * Upper case of a code.
   *
   * @param value value
   * @return upper case, null when blank
   */
  public static String code(String value) {
    return blank(value) ? null : value.strip().toUpperCase(Locale.ROOT);
  }

  /**
   * The text, null when blank.
   *
   * @param value value
   * @return stripped text or null
   */
  public static String text(String value) {
    return blank(value) ? null : value.strip();
  }
}
