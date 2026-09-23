package com.iortatechnxt.finverse.system.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Data type of a business parameter; each type validates the text it stores. */
public enum ParameterValueType {
  STRING,
  INTEGER,
  DECIMAL,
  BOOLEAN,
  /** Comma separated ascending positive integers, e.g. ageing buckets "30,60,90". */
  INTEGER_LIST,
  /** Comma separated codes (letters, digits, dash, underscore); may be empty. */
  CODE_LIST;

  private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,30}");
  private static final Pattern NUMBER = Pattern.compile("-?\\d{1,9}");
  private static final Pattern BOOLEAN_VALUE = Pattern.compile("true|false");
  private static final Pattern DECIMAL_NUMBER = Pattern.compile("-?\\d{1,17}(\\.\\d{1,8})?");

  /**
   * Validates a value.
   *
   * @param value text to store
   * @param min lower bound for INTEGER / INTEGER_LIST values (null = none)
   * @param max upper bound for INTEGER / INTEGER_LIST values (null = none)
   * @return error message, empty when valid
   */
  public Optional<String> validate(String value, Integer min, Integer max) {
    return switch (this) {
      case STRING -> Optional.empty();
      case INTEGER -> validateInteger(value.trim(), min, max);
      case DECIMAL -> require(DECIMAL_NUMBER.matcher(value.trim()).matches(), "a decimal number");
      case BOOLEAN -> require(BOOLEAN_VALUE.matcher(value).matches(), "true or false");
      case INTEGER_LIST -> validateIntegerList(value, min, max);
      case CODE_LIST -> validateCodes(value);
    };
  }

  private static Optional<String> require(boolean valid, String expected) {
    return valid ? Optional.empty() : Optional.of("must be " + expected);
  }

  /**
   * Splits a list value into trimmed, non-empty items.
   *
   * @param value comma separated text
   * @return items
   */
  public static List<String> items(String value) {
    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static Optional<String> validateInteger(String value, Integer min, Integer max) {
    if (!NUMBER.matcher(value).matches()) {
      return Optional.of("must be a whole number");
    }
    return checkRange(Integer.parseInt(value), min, max);
  }

  private static Optional<String> checkRange(int n, Integer min, Integer max) {
    if (min != null && n < min || max != null && n > max) {
      return Optional.of("must be between " + min + " and " + max);
    }
    return Optional.empty();
  }

  private static Optional<String> validateIntegerList(String value, Integer min, Integer max) {
    List<String> parts = items(value);
    if (parts.isEmpty()) {
      return Optional.of("needs at least one value");
    }
    int previous = Integer.MIN_VALUE;
    for (String part : parts) {
      Optional<String> error = validateInteger(part, min, max);
      if (error.isPresent()) {
        return error;
      }
      int n = Integer.parseInt(part);
      if (n <= previous) {
        return Optional.of("values must be in ascending order");
      }
      previous = n;
    }
    return Optional.empty();
  }

  private static Optional<String> validateCodes(String value) {
    return items(value).stream().allMatch(c -> CODE.matcher(c).matches())
        ? Optional.empty()
        : Optional.of("must be a comma separated list of codes");
  }

  /**
   * Parses a DECIMAL value.
   *
   * @param value validated text
   * @return number
   */
  public static BigDecimal decimal(String value) {
    return new BigDecimal(value.trim());
  }
}
