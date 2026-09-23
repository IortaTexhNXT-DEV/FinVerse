package com.iortatechnxt.finverse.report.core;

/**
 * Report column.
 *
 * @param key cell key
 * @param label heading
 * @param type data type
 * @param summed whether the column is totalled (numeric columns only)
 */
public record ReportColumn(String key, String label, ColumnType type, boolean summed) {

  /**
   * Text column.
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn text(String key, String label) {
    return new ReportColumn(key, label, ColumnType.TEXT, false);
  }

  /**
   * Date column.
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn date(String key, String label) {
    return new ReportColumn(key, label, ColumnType.DATE, false);
  }

  /**
   * Amount column, totalled.
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn amount(String key, String label) {
    return new ReportColumn(key, label, ColumnType.AMOUNT, true);
  }

  /**
   * Amount column, not totalled (e.g. rates or balances that must not be summed).
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn amountNoTotal(String key, String label) {
    return new ReportColumn(key, label, ColumnType.AMOUNT, false);
  }

  /**
   * Count column, totalled.
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn count(String key, String label) {
    return new ReportColumn(key, label, ColumnType.NUMBER, true);
  }

  /**
   * Percentage column (never totalled).
   *
   * @param key key
   * @param label label
   * @return column
   */
  public static ReportColumn percent(String key, String label) {
    return new ReportColumn(key, label, ColumnType.PERCENT, false);
  }
}
