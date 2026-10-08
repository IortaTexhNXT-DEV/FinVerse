package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.List;
import java.util.Map;

/**
 * The data file of one dataset in a package.
 *
 * @param dataset dataset code
 * @param columns promoted columns
 * @param rows rows sorted by natural key; references hold the natural key of the referenced row
 */
public record DatasetFile(String dataset, List<String> columns, List<Map<String, Object>> rows) {

  /** Defensive copies. */
  public DatasetFile {
    columns = columns == null ? List.of() : List.copyOf(columns);
    rows = rows == null ? List.of() : List.copyOf(rows);
  }
}
