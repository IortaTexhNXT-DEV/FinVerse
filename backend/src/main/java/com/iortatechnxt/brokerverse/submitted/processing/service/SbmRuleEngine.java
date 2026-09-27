package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleCondition;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates rule conditions against the facts of a record (BRIDSP-08): every condition of a rule
 * must hold. Text compares without case, numbers numerically; IN and NOT_IN take a comma list.
 */
public final class SbmRuleEngine {

  /** The operators of a condition. */
  public static final List<String> OPERATORS =
      List.of("EQ", "NE", "IN", "NOT_IN", "GT", "GTE", "LT", "LTE", "EMPTY", "NOT_EMPTY");

  private SbmRuleEngine() {}

  /**
   * Whether a rule matches.
   *
   * @param rule rule
   * @param facts facts by name
   * @return true when every condition holds
   */
  public static boolean matches(SbmRule rule, Map<String, Object> facts) {
    return !rule.getConditions().isEmpty()
        && rule.getConditions().stream().allMatch(c -> holds(c, facts.get(c.field())));
  }

  /**
   * Whether a condition holds for a fact value.
   *
   * @param c condition
   * @param fact value, may be null
   * @return true when it holds
   */
  static boolean holds(SbmRuleCondition c, Object fact) {
    String actual = fact == null ? null : fact.toString();
    boolean empty = actual == null || actual.isBlank();
    return switch (c.operator()) {
      case "EMPTY" -> empty;
      case "NOT_EMPTY" -> !empty;
      case "EQ" -> !empty && same(actual, c.value());
      case "NE" -> empty || !same(actual, c.value());
      case "IN" -> !empty && list(c.value()).stream().anyMatch(v -> same(actual, v));
      case "NOT_IN" -> empty || list(c.value()).stream().noneMatch(v -> same(actual, v));
      default -> compare(c.operator(), actual, c.value());
    };
  }

  private static boolean compare(String operator, String actual, String expected) {
    Optional<BigDecimal> a = number(actual);
    Optional<BigDecimal> e = number(expected);
    if (a.isEmpty() || e.isEmpty()) {
      return false;
    }
    int cmp = a.get().compareTo(e.get());
    return switch (operator) {
      case "GT" -> cmp > 0;
      case "GTE" -> cmp >= 0;
      case "LT" -> cmp < 0;
      case "LTE" -> cmp <= 0;
      default -> false;
    };
  }

  private static boolean same(String actual, String expected) {
    if (expected == null) {
      return false;
    }
    Optional<BigDecimal> a = number(actual);
    Optional<BigDecimal> e = number(expected.strip());
    if (a.isPresent() && e.isPresent()) {
      return a.get().compareTo(e.get()) == 0;
    }
    return actual
        .strip()
        .toUpperCase(Locale.ROOT)
        .equals(expected.strip().toUpperCase(Locale.ROOT));
  }

  private static List<String> list(String value) {
    return value == null ? List.of() : Arrays.stream(value.split(",")).map(String::strip).toList();
  }

  private static Optional<BigDecimal> number(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new BigDecimal(value.strip()));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
