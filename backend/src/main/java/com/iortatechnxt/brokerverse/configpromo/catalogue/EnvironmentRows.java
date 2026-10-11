package com.iortatechnxt.brokerverse.configpromo.catalogue;

import java.util.List;

/**
 * Rows of a dataset that belong to the environment: they are never exported and an import never
 * changes them (for example the mail sender address or the sign-in mode among the parameters).
 *
 * @param column column identifying the rows
 * @param values values of that column
 */
public record EnvironmentRows(String column, List<String> values) {

  /** Defensive copy. */
  public EnvironmentRows {
    values = values == null ? List.of() : List.copyOf(values);
  }

  /**
   * Whether a value of the column marks an environment row.
   *
   * @param value value of the column
   * @return true for an environment row
   */
  public boolean matches(Object value) {
    return value != null && values.contains(value.toString());
  }
}
