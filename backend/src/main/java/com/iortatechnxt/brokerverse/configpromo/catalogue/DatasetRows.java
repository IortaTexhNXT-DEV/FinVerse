package com.iortatechnxt.brokerverse.configpromo.catalogue;

import java.util.List;

/**
 * The rows of a table that make up a dataset when one table holds more than one dataset (for
 * example the standard report variants, configuration of every environment, and the variants the
 * users saved for themselves). Rows outside the dataset are neither exported nor changed by an
 * import of it.
 *
 * @param column column identifying the rows
 * @param values values of that column
 */
public record DatasetRows(String column, List<String> values) {

  /** Defensive copy. */
  public DatasetRows {
    values = values == null ? List.of() : List.copyOf(values);
  }

  /**
   * Whether a value of the column marks a row of the dataset.
   *
   * @param value value of the column
   * @return true for a row of the dataset
   */
  public boolean matches(Object value) {
    return value != null && values.contains(value.toString());
  }
}
