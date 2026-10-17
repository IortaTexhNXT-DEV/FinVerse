package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The column guide of a layout column on the load templates: mandatory or when, format, allowed
 * values (a drop-down of codes, a code map or the layout's text), what to enter and the note.
 */
final class LayoutColumnGuide {

  private static final Pattern CODE = Pattern.compile("[A-Z0-9_]+");

  private LayoutColumnGuide() {}

  /**
   * The guide of a layout column.
   *
   * @param c column
   * @return guide column
   */
  static GuideColumn column(LayoutColumn c) {
    Kind kind = kind(c);
    GuideColumn g =
        GuideColumn.of(c.getName(), kind, text(c.getDescription()))
            .example(text(c.getExample()))
            .note(note(c));
    String mandatory = text(c.getMandatory());
    if ("Y".equals(mandatory)) {
      g = g.mandatory();
    } else if ("C".equals(mandatory)) {
      g = g.when(condition(text(c.getValidation())));
    }
    return kind == Kind.YES_NO ? g.format(format(c)) : allowed(g, c).format(format(c));
  }

  /** Allowed values: a list of codes as a drop-down, a code map, or the text of the layout. */
  private static GuideColumn allowed(GuideColumn g, LayoutColumn c) {
    String allowed = text(c.getAllowedValues());
    List<String> codes = Arrays.stream(allowed.split(",")).map(String::strip).toList();
    if (codes.size() > 1 && codes.stream().allMatch(v -> CODE.matcher(v).matches())) {
      return g.choices(codes.stream().map(v -> new Choice(v, v)).toList());
    }
    if (c.getMapSet() != null && !c.getMapSet().isBlank()) {
      return g.allowed(
          "Code as stored in the legacy system, translated by the code map "
              + c.getMapSet()
              + (allowed.isEmpty() || allowed.startsWith("Code map") ? "" : "; " + allowed));
    }
    return g.allowed(allowed);
  }

  /** The condition of a conditional column: its check without a leading "Mandatory". */
  private static String condition(String validation) {
    return validation.startsWith("Mandatory ")
        ? validation.substring("Mandatory ".length())
        : validation;
  }

  private static Kind kind(LayoutColumn c) {
    return switch (c.getDataType()) {
      case DATE -> Kind.DATE;
      case AMOUNT -> Kind.AMOUNT;
      case INTEGER -> Kind.INTEGER;
      case DECIMAL -> Kind.NUMBER;
      case FLAG -> Kind.YES_NO;
      case TEXT, CODE, TIMESTAMP -> Kind.TEXT;
    };
  }

  private static String format(LayoutColumn c) {
    List<String> parts = new ArrayList<>();
    String kindFormat = kindFormat(c.getDataType());
    String format = text(c.getFormat());
    if (!kindFormat.isEmpty()) {
      parts.add(kindFormat);
    } else if (!format.isEmpty()) {
      parts.add(format);
    }
    String length = text(c.getLength());
    boolean text =
        c.getDataType() == LayoutColumn.DataType.TEXT
            || c.getDataType() == LayoutColumn.DataType.CODE;
    if (!length.isEmpty() && text) {
      parts.add("at most " + length + " characters");
    }
    return String.join(", ", parts);
  }

  private static String kindFormat(LayoutColumn.DataType type) {
    return switch (type) {
      case DATE -> "Date, e.g. 31-Dec-2027 (an Excel date or yyyy-MM-dd)";
      case TIMESTAMP -> "Text yyyy-MM-dd HH:mm:ss";
      case AMOUNT -> "Amount with a dot decimal, e.g. 1500000.00";
      case FLAG -> "Y or N";
      default -> "";
    };
  }

  private static String note(LayoutColumn c) {
    StringBuilder out = new StringBuilder(text(c.getDescription()));
    String validation = text(c.getValidation());
    if (!validation.isEmpty()) {
      out.append("\nCheck: ").append(validation);
    }
    String source = text(c.getSourceHint());
    if (!source.isEmpty()) {
      out.append("\nSource: ").append(source);
    }
    return out.toString();
  }

  private static String text(String v) {
    return v == null ? "" : v.strip();
  }
}
