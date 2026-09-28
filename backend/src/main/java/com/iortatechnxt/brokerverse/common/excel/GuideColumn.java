package com.iortatechnxt.brokerverse.common.excel;

import java.util.List;

/**
 * One column of a guided template: its header and the guide shown directly above it (mandatory,
 * format, allowed values, what to enter), the note of the header cell and the example value.
 *
 * @param header header text, exactly as the upload reads it (a mandatory column gets " *" added on
 *     the sheet; the readers ignore the star)
 * @param need whether a value is mandatory
 * @param condition when a conditional column is mandatory (Need.CONDITIONAL), else empty
 * @param kind value kind (decides the format, the drop-down and the cell validation)
 * @param format format text; empty for the kind's default
 * @param choices allowed values of a list column (code and label), empty otherwise
 * @param allowed allowed values in words (e.g. "Code of an existing client"); empty for none
 * @param whatToEnter short guide shown in the band
 * @param note full description shown as the note of the header cell; empty for the guide text
 * @param example example value (ISO date for a date, plain number for a number); empty for none
 * @param width column width in characters; 0 for the default
 */
public record GuideColumn(
    String header,
    Need need,
    String condition,
    Kind kind,
    String format,
    List<Choice> choices,
    String allowed,
    String whatToEnter,
    String note,
    String example,
    int width) {

  /** Whether a column must be filled in. */
  public enum Need {
    /** Always mandatory. */
    YES,
    /** Optional. */
    NO,
    /** Mandatory when a condition holds. */
    CONDITIONAL
  }

  /** Kind of value (format, drop-down and cell validation). */
  public enum Kind {
    /** Free text (the cell keeps leading zeros). */
    TEXT,
    /** Whole number. */
    INTEGER,
    /** Decimal number. */
    NUMBER,
    /** Amount with two decimals. */
    AMOUNT,
    /** Date. */
    DATE,
    /** Y or N. */
    YES_NO,
    /** One of the listed codes. */
    LIST
  }

  /**
   * An allowed value of a list column.
   *
   * @param code code entered in the file
   * @param label business label
   */
  public record Choice(String code, String label) {

    /** Null-safe values. */
    public Choice {
      code = code == null ? "" : code;
      label = label == null || label.isBlank() ? code : label;
    }
  }

  /** Null-safe values and a copy of the choices. */
  public GuideColumn {
    need = need == null ? Need.NO : need;
    kind = kind == null ? Kind.TEXT : kind;
    condition = text(condition);
    format = text(format);
    choices = choices == null ? List.of() : List.copyOf(choices);
    allowed = text(allowed);
    whatToEnter = text(whatToEnter);
    note = text(note);
    example = text(example);
  }

  /**
   * A new optional column.
   *
   * @param header header text
   * @param kind value kind
   * @param whatToEnter what to enter
   * @return column
   */
  public static GuideColumn of(String header, Kind kind, String whatToEnter) {
    return new GuideColumn(header, Need.NO, "", kind, "", List.of(), "", whatToEnter, "", "", 0);
  }

  /**
   * This column, mandatory.
   *
   * @return column
   */
  public GuideColumn mandatory() {
    return new GuideColumn(
        header, Need.YES, "", kind, format, choices, allowed, whatToEnter, note, example, width);
  }

  /**
   * This column, mandatory or optional.
   *
   * @param required true for mandatory
   * @return column
   */
  public GuideColumn mandatory(boolean required) {
    return required ? mandatory() : this;
  }

  /**
   * This column, mandatory when a condition holds.
   *
   * @param when the condition in words
   * @return column
   */
  public GuideColumn when(String when) {
    return new GuideColumn(
        header,
        Need.CONDITIONAL,
        when,
        kind,
        format,
        choices,
        allowed,
        whatToEnter,
        note,
        example,
        width);
  }

  /**
   * This column with its allowed values (a list column when it was text).
   *
   * @param values codes and labels
   * @return column
   */
  public GuideColumn choices(List<Choice> values) {
    Kind k = kind == Kind.TEXT && !values.isEmpty() ? Kind.LIST : kind;
    return new GuideColumn(
        header, need, condition, k, format, values, allowed, whatToEnter, note, example, width);
  }

  /**
   * This column with its allowed values in words.
   *
   * @param text e.g. "Code of an existing client"
   * @return column
   */
  public GuideColumn allowed(String text) {
    return new GuideColumn(
        header, need, condition, kind, format, choices, text, whatToEnter, note, example, width);
  }

  /**
   * This column with its own format text.
   *
   * @param text format
   * @return column
   */
  public GuideColumn format(String text) {
    return new GuideColumn(
        header, need, condition, kind, text, choices, allowed, whatToEnter, note, example, width);
  }

  /**
   * This column with the full description of its header note.
   *
   * @param text description
   * @return column
   */
  public GuideColumn note(String text) {
    return new GuideColumn(
        header, need, condition, kind, format, choices, allowed, whatToEnter, text, example, width);
  }

  /**
   * This column with its example value.
   *
   * @param value example
   * @return column
   */
  public GuideColumn example(String value) {
    return new GuideColumn(
        header, need, condition, kind, format, choices, allowed, whatToEnter, note, value, width);
  }

  /**
   * This column with its width.
   *
   * @param characters width in characters
   * @return column
   */
  public GuideColumn width(int characters) {
    return new GuideColumn(
        header,
        need,
        condition,
        kind,
        format,
        choices,
        allowed,
        whatToEnter,
        note,
        example,
        characters);
  }

  /**
   * The header as shown on the sheet: a mandatory column carries a star.
   *
   * @return header text
   */
  public String shownHeader() {
    return need == Need.YES ? header + GuidedTables.MANDATORY_MARK : header;
  }

  /**
   * The Mandatory cell of the band.
   *
   * @return Yes, No or "Conditional: ..."
   */
  public String needText() {
    return switch (need) {
      case YES -> "Yes";
      case NO -> "No";
      case CONDITIONAL -> condition.isEmpty() ? "Conditional" : "Conditional: " + condition;
    };
  }

  /**
   * The Format cell of the band.
   *
   * @return format in words
   */
  public String formatText() {
    if (!format.isEmpty()) {
      return format;
    }
    return switch (kind) {
      case TEXT -> "Text";
      case INTEGER -> "Whole number, e.g. 12";
      case NUMBER -> "Number without thousands separators, e.g. 1500000.50";
      case AMOUNT -> "Amount without thousands separators, 2 decimals, e.g. 1500000.00";
      case DATE -> "Date dd-MMM-yyyy, e.g. 15-Jan-2026";
      case YES_NO -> "Y or N";
      case LIST -> "Code from the drop-down";
    };
  }

  /**
   * The Allowed values cell of the band.
   *
   * @param shown largest number of values written out
   * @return allowed values in words; "Any" when free
   */
  public String allowedText(int shown) {
    if (!choices.isEmpty()) {
      StringBuilder out = new StringBuilder();
      int n = Math.min(shown, choices.size());
      for (int i = 0; i < n; i++) {
        if (i > 0) {
          out.append('\n');
        }
        out.append(choiceText(choices.get(i)));
      }
      if (choices.size() > n) {
        out.append("\n… ").append(choices.size() - n).append(" more on the Lists sheet");
      }
      return allowed.isEmpty() ? out.toString() : allowed + "\n" + out;
    }
    if (!allowed.isEmpty()) {
      return allowed;
    }
    return kind == Kind.YES_NO ? "Y = Yes, N = No" : "Any";
  }

  /**
   * The full description of the header note.
   *
   * @return note text
   */
  public String noteText() {
    return note.isEmpty() ? whatToEnter : note;
  }

  /**
   * A choice as "CODE – Label" (only the code when both are the same).
   *
   * @param c choice
   * @return text
   */
  static String choiceText(Choice c) {
    return c.label().equals(c.code()) ? c.code() : c.code() + " – " + c.label();
  }

  private static String text(String value) {
    return value == null ? "" : value.strip();
  }
}
