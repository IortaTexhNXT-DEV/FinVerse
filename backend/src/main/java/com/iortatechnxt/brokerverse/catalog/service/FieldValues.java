package com.iortatechnxt.brokerverse.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The given values of a record and of each of its risk items, by field key, as text (BRPM.004;
 * checked by {@link ProductRuleService#violations}). Blank values are ignored: presence is the
 * minimum-field matrix ({@link FieldPresence}).
 *
 * @param record record-level values (account, quotation)
 * @param items values of each risk item, in item order
 */
public record FieldValues(Map<String, String> record, List<Map<String, String>> items) {

  /** Defensive copies without null values. */
  public FieldValues {
    record = clean(record);
    items = items == null ? List.of() : items.stream().map(FieldValues::clean).toList();
  }

  private static Map<String, String> clean(Map<String, String> values) {
    if (values == null) {
      return Map.of();
    }
    return values.entrySet().stream()
        .filter(e -> e.getKey() != null && e.getValue() != null && !e.getValue().isBlank())
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
