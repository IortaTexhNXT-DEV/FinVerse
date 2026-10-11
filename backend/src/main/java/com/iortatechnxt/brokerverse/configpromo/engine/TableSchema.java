package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A table: its columns in database order and its foreign keys.
 *
 * @param name table name
 * @param columns columns by name, in database order
 * @param foreignKeys foreign keys
 */
public record TableSchema(
    String name, Map<String, ColumnInfo> columns, List<ForeignKey> foreignKeys) {

  /** Defensive copies keeping the column order. */
  public TableSchema {
    columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
    foreignKeys = List.copyOf(foreignKeys);
  }

  /**
   * Whether the table has a column.
   *
   * @param column column
   * @return true when present
   */
  public boolean has(String column) {
    return columns.containsKey(column);
  }

  /**
   * A column that must exist.
   *
   * @param column column
   * @return column
   */
  public ColumnInfo column(String column) {
    ColumnInfo c = columns.get(column);
    if (c == null) {
      throw new IllegalArgumentException("Table " + name + " has no column " + column);
    }
    return c;
  }

  /**
   * Fingerprint of a set of columns: names and types, so two environments can tell whether a
   * dataset has the same shape on both sides.
   *
   * @param names columns
   * @return "name:type" entries joined by commas
   */
  public String fingerprint(List<String> names) {
    return names.stream().map(n -> n + ":" + column(n).type()).collect(Collectors.joining(","));
  }
}
