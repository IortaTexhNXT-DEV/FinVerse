package com.iortatechnxt.brokerverse.bulk.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One data row handed to a handler: values by column header, already sanitised and type-checked by
 * the framework.
 *
 * @param rowNo row number in the file (the header row is 1)
 * @param values values by header; blank cells are absent
 */
public record BulkRow(int rowNo, Map<String, String> values) {

  /** Immutable copy preserving column order. */
  public BulkRow {
    values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  /**
   * Text value.
   *
   * @param header column
   * @return value or null
   */
  public String text(String header) {
    return values.get(header);
  }

  /**
   * Number value (framework-checked for NUMBER columns).
   *
   * @param header column
   * @return value or null
   */
  public BigDecimal number(String header) {
    String v = values.get(header);
    return v == null ? null : new BigDecimal(v);
  }

  /**
   * Date value (framework-checked for DATE columns).
   *
   * @param header column
   * @return value or null
   */
  public LocalDate date(String header) {
    String v = values.get(header);
    return v == null ? null : LocalDate.parse(v);
  }

  /**
   * Yes/no value (framework-checked for YES_NO columns).
   *
   * @param header column
   * @return true for Y
   */
  public boolean yes(String header) {
    return "Y".equals(values.get(header));
  }
}
