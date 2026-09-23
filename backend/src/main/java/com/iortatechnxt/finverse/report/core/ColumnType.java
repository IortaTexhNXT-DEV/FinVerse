package com.iortatechnxt.finverse.report.core;

/** Column data types (drive alignment, formatting and totals). */
public enum ColumnType {
  TEXT,
  DATE,
  NUMBER,
  AMOUNT,
  PERCENT;

  /**
   * Whether values of this type are summed in subtotals.
   *
   * @return true for AMOUNT and NUMBER
   */
  public boolean isSummable() {
    return this == AMOUNT || this == NUMBER;
  }
}
