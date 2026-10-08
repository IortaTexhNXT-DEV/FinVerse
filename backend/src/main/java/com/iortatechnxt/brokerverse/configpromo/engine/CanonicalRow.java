package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A row in package form: the promoted columns with references replaced by the natural key of the
 * referenced row, its natural key and the canonical text of that key.
 *
 * @param id surrogate id in the database it was read from (null for package rows and tables
 *     without id)
 * @param values promoted columns
 * @param key natural key columns
 * @param keyText canonical JSON of the key, unique within the dataset
 */
public record CanonicalRow(Long id, Map<String, Object> values, Map<String, Object> key, String keyText) {

  /** Unmodifiable copies keeping the column order. */
  public CanonicalRow {
    values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    key = Collections.unmodifiableMap(new LinkedHashMap<>(key));
  }

  /**
   * A row from its values.
   *
   * @param id id or null
   * @param values promoted columns
   * @param model dataset
   * @return row
   */
  public static CanonicalRow of(Long id, Map<String, Object> values, DatasetModel model) {
    Map<String, Object> key = new LinkedHashMap<>();
    for (String column : model.dataset().key()) {
      key.put(column, values.get(column));
    }
    return new CanonicalRow(id, values, key, CanonicalJson.text(key));
  }

  /**
   * The value of a column.
   *
   * @param column column
   * @return value or null
   */
  public Object get(String column) {
    return values.get(column);
  }
}
