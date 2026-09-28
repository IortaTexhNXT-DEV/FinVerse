package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.util.Arrays;
import java.util.List;

/**
 * A column of a bulk template, with the guide shown above it on the template (mandatory or when,
 * format, allowed values, what to enter).
 *
 * @param header column header (exact text expected in the file)
 * @param description what to enter (column guide and header note)
 * @param required whether a value is mandatory
 * @param type value type checked before the handler's own validation
 * @param example example value
 * @param condition when the column is mandatory although not always ("" when it is not conditional)
 * @param choices allowed codes with their labels (a drop-down on the template)
 * @param lov platform list of values whose codes are allowed ("" for none); resolved when the
 *     template is written
 * @param allowed allowed values in words, e.g. "Code of an existing client" ("" for none)
 * @param format format in words when the type's default does not say it ("" for the default)
 */
public record BulkColumn(
    String header,
    String description,
    boolean required,
    Type type,
    String example,
    String condition,
    List<Choice> choices,
    String lov,
    String allowed,
    String format) {

  /** Value types checked by the framework. */
  public enum Type {
    /** Any text. */
    TEXT,
    /** Decimal number (dot as decimal separator, no thousands separators). */
    NUMBER,
    /** Date as yyyy-MM-dd or dd-MMM-yyyy (or an Excel date cell). */
    DATE,
    /** Y or N. */
    YES_NO
  }

  /** Null-safe guide values. */
  public BulkColumn {
    condition = condition == null ? "" : condition;
    choices = choices == null ? List.of() : List.copyOf(choices);
    lov = lov == null ? "" : lov;
    allowed = allowed == null ? "" : allowed;
    format = format == null ? "" : format;
  }

  /**
   * A column without guide details.
   *
   * @param header header
   * @param description description
   * @param required mandatory
   * @param type type
   * @param example example
   */
  public BulkColumn(
      String header, String description, boolean required, Type type, String example) {
    this(header, description, required, type, example, "", List.of(), "", "", "");
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

  /**
   * This column, mandatory when a condition holds (the template says "Conditional: ...").
   *
   * @param when condition in words
   * @return column
   */
  public BulkColumn when(String when) {
    return new BulkColumn(
        header, description, required, type, example, when, choices, lov, allowed, format);
  }

  /**
   * This column with its allowed codes and labels.
   *
   * @param values choices
   * @return column
   */
  public BulkColumn choices(List<Choice> values) {
    return new BulkColumn(
        header, description, required, type, example, condition, values, lov, allowed, format);
  }

  /**
   * This column with its allowed codes, labelled in words (NEW_BUSINESS: "New business").
   *
   * @param codes codes
   * @return column
   */
  public BulkColumn codes(String... codes) {
    return choices(Arrays.stream(codes).map(c -> new Choice(c, label(c))).toList());
  }

  /**
   * This column with the constants of an enum as allowed codes, labelled in words.
   *
   * @param type enum
   * @return column
   */
  public BulkColumn codes(Class<? extends Enum<?>> type) {
    return codes(Arrays.stream(type.getEnumConstants()).map(Enum::name).toArray(String[]::new));
  }

  /**
   * This column with the codes of a platform list of values (read when the template is written).
   *
   * @param typeCode list type, e.g. CIVIL_STATUS
   * @return column
   */
  public BulkColumn lov(String typeCode) {
    return new BulkColumn(
        header,
        description,
        required,
        type,
        example,
        condition,
        choices,
        typeCode,
        allowed,
        format);
  }

  /**
   * This column holding the code of an existing record ("Code of an existing client").
   *
   * @param master record in words, e.g. "client"
   * @return column
   */
  public BulkColumn master(String master) {
    return allowed("Code of an existing " + master);
  }

  /**
   * This column with its allowed values in words.
   *
   * @param text allowed values
   * @return column
   */
  public BulkColumn allowed(String text) {
    return new BulkColumn(
        header, description, required, type, example, condition, choices, lov, text, format);
  }

  /**
   * This column with its own format text.
   *
   * @param text format in words
   * @return column
   */
  public BulkColumn format(String text) {
    return new BulkColumn(
        header, description, required, type, example, condition, choices, lov, allowed, text);
  }

  /**
   * A code in words with a capital: NEW_BUSINESS becomes "New business".
   *
   * @param code code
   * @return label
   */
  static String label(String code) {
    String words = DisplayFormat.words(code);
    return words.isEmpty() ? code : Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }
}
