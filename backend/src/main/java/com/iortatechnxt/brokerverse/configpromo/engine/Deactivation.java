package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * How an item of a dataset is deactivated (items are never deleted).
 *
 * @param column status or flag column
 * @param inactive value of an inactive item ("INACTIVE" or false)
 */
public record Deactivation(String column, Object inactive) {

  /**
   * Whether a canonical value means "inactive".
   *
   * @param value canonical value of the column
   * @return true when inactive
   */
  public boolean isInactive(Object value) {
    return inactive.equals(value);
  }
}
