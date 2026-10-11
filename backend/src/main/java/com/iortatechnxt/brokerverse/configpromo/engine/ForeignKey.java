package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.List;

/**
 * A foreign key of a table.
 *
 * @param columns referencing columns
 * @param table referenced table
 * @param targetColumns referenced columns, in the order of {@code columns}
 */
public record ForeignKey(List<String> columns, String table, List<String> targetColumns) {

  /** Defensive copies. */
  public ForeignKey {
    columns = List.copyOf(columns);
    targetColumns = List.copyOf(targetColumns);
  }

  /**
   * Whether the key references the surrogate id of the target table.
   *
   * @return true for a single column referencing {@code id}
   */
  public boolean byId() {
    return columns.size() == 1 && "id".equals(targetColumns.get(0));
  }
}
