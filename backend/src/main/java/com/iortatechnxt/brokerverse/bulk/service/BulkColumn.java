package com.iortatechnxt.brokerverse.bulk.service;

/**
 * A column of a bulk template.
 *
 * @param header column header (exact text expected in the file)
 * @param description what to enter (template instructions sheet)
 * @param required whether a value is mandatory
 * @param type value type checked before the handler's own validation
 * @param example example value
 */
public record BulkColumn(
    String header, String description, boolean required, Type type, String example) {

  /** Value types checked by the framework. */
  public enum Type {
    /** Any text. */
    TEXT,
    /** Decimal number (dot as decimal separator, no thousands separators). */
    NUMBER,
    /** Date as yyyy-MM-dd. */
    DATE,
    /** Y or N. */
    YES_NO
  }

  /**
   * Mandatory text column.
   *
   * @param header header
   * @param description description
   * @param example example
   * @return column
   */
  public static BulkColumn required(String header, String description, String example) {
    return new BulkColumn(header, description, true, Type.TEXT, example);
  }

  /**
   * Optional text column.
   *
   * @param header header
   * @param description description
   * @param example example
   * @return column
   */
  public static BulkColumn optional(String header, String description, String example) {
    return new BulkColumn(header, description, false, Type.TEXT, example);
  }
}
