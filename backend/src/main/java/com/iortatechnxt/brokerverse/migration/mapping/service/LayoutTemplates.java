package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The guided Excel load templates of the extract layouts: one guided sheet per layout whose column
 * guide comes from the layout columns in force (mandatory or when, format, allowed values or code
 * map, what to enter), the control file sheet, and the rules of a delivery. The Excel template is
 * uploaded as it is on the Extracts screen: the intake reads the sheet of the layout named in the
 * file name, below its guide, without its example row, with the same checksum and control checks as
 * a CSV file.
 */
final class LayoutTemplates {

  /** Who fills the load templates in. */
  static final String FILLED_BY =
      "The data stewards of the legacy systems (EBIX, QPS, ISYS and the other sources), from their"
          + " extracts";

  private static final int MAX_SHEET_NAME = 31;
  private static final Pattern CODE = Pattern.compile("[A-Z0-9_]+");

  private LayoutTemplates() {}

  /**
   * The rules of a delivery (title block or Start here sheet).
   *
   * @return rules
   */
  static List<String> rules() {
    return TemplateExport.HOW_TO_FILL.stream().map(r -> r.get(0) + ": " + r.get(1) + ".").toList();
  }

  /**
   * The how-to-upload text of the templates.
   *
   * @return text
   */
  static String howToUpload() {
    return "Data Migration > Extracts, box Upload an extract: choose the data file and its control"
        + " file together; never send extracts by e-mail.";
  }

  /**
   * A one-layout template.
   *
   * @param layout layout in force
   * @param columns its columns
   * @return template
   */
  static GuidedTemplate single(Layout layout, List<LayoutColumn> columns) {
    return new GuidedTemplate(
        "Load template " + layout.getCode() + " – " + layout.getTitle(),
        purpose(layout),
        FILLED_BY,
        howToUpload(),
        rules(),
        List.of(sheet(layout, columns)),
        GuidedTemplate.DEFAULT_MAX_ROWS);
  }

  /**
   * A template of several layouts: a Start here sheet and one guided sheet per layout.
   *
   * @param name template name
   * @param purpose what it is for
   * @param sheets sheets of the layouts
   * @return template
   */
  static GuidedTemplate workbook(String name, String purpose, List<GuidedSheet> sheets) {
    return new GuidedTemplate(
        name, purpose, FILLED_BY, howToUpload(), rules(), sheets, GuidedTemplate.DEFAULT_MAX_ROWS);
  }

  /**
   * The guided sheet of a layout.
   *
   * @param layout layout
   * @param columns columns
   * @return sheet
   */
  static GuidedSheet sheet(Layout layout, List<LayoutColumn> columns) {
    return GuidedSheet.of(
        sheetName(layout),
        layout.getCode() + " " + layout.getTitle(),
        "Layout "
            + layout.getCode()
            + " version "
            + layout.getVersionNo()
            + " of object "
            + layout.getObjectCode()
            + ": one row per record"
            + (layout.keys().isEmpty() ? "" : ", identified by " + String.join(", ", layout.keys()))
            + ".",
        columns.stream().map(LayoutTemplates::column).toList());
  }

  private static String purpose(Layout layout) {
    return "Delivers the legacy records of layout "
        + layout.getCode()
        + " ("
        + layout.getTitle()
        + ") for loading into BIBS.";
  }

  private static String sheetName(Layout layout) {
    String name = layout.getCode() + " " + layout.getTitle().replaceAll("[\\\\/?*\\[\\]:]", " ");
    return name.length() > MAX_SHEET_NAME ? name.substring(0, MAX_SHEET_NAME).trim() : name;
  }

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
      g = g.when(text(c.getValidation()));
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
