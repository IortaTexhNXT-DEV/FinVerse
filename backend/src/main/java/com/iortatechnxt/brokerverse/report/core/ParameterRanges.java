package com.iortatechnxt.brokerverse.report.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * From / To parameter pairs of a report and the check that a range is not reversed.
 *
 * <p>Pairs follow the naming convention of every report: {@code fromDate} / {@code toDate}, and
 * {@code xxxFrom} / {@code xxxTo} (e.g. {@code expiryFrom} / {@code expiryTo}, {@code uwYearFrom} /
 * {@code uwYearTo}). Only dates and numbers are compared; code ranges (accounts, parties) are free
 * text.
 */
final class ParameterRanges {

  private static final String FROM_DATE = "fromDate";
  private static final String TO_DATE = "toDate";
  private static final String FROM = "From";
  private static final String TO = "To";
  private static final Set<ParameterType> ORDERED =
      EnumSet.of(ParameterType.DATE, ParameterType.NUMBER);

  private ParameterRanges() {}

  /**
   * Adds an error for every range whose upper bound is below its lower bound.
   *
   * @param specs parameter declarations
   * @param values resolved values by name (absent = not supplied)
   * @param errors receives the messages
   */
  static void check(List<ParameterSpec> specs, Map<String, String> values, List<String> errors) {
    Map<String, ParameterSpec> byName =
        specs.stream().collect(Collectors.toMap(ParameterSpec::name, Function.identity()));
    for (ParameterSpec from : specs) {
      ParameterSpec to = partnerName(from.name()).map(byName::get).orElse(null);
      if (to == null || from.type() != to.type() || !ORDERED.contains(from.type())) {
        continue;
      }
      String low = values.get(from.name());
      String high = values.get(to.name());
      if (low != null && high != null && reversed(from.type(), low, high)) {
        errors.add(to.label() + " must not be before " + from.label());
      }
    }
  }

  /**
   * Name of the upper bound belonging to a lower-bound parameter.
   *
   * @param name parameter name
   * @return partner name, empty when the parameter is not a lower bound
   */
  static Optional<String> partnerName(String name) {
    if (FROM_DATE.equals(name)) {
      return Optional.of(TO_DATE);
    }
    if (name.endsWith(FROM) && name.length() > FROM.length()) {
      return Optional.of(name.substring(0, name.length() - FROM.length()) + TO);
    }
    return Optional.empty();
  }

  private static boolean reversed(ParameterType type, String low, String high) {
    try {
      return type == ParameterType.DATE
          ? LocalDate.parse(high).isBefore(LocalDate.parse(low))
          : new BigDecimal(high).compareTo(new BigDecimal(low)) < 0;
    } catch (DateTimeParseException | NumberFormatException ex) {
      // Malformed values are reported by the type check.
      return false;
    }
  }
}
