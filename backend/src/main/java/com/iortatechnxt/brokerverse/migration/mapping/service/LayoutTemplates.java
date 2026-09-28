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
  private static final int CONTROL_ROWS = 200;
  private static final Pattern CODE_LIST = Pattern.compile("[A-Z0-9_]+(, ?[A-Z0-9_]+)+");

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
    String allowed = text(c.getAllowedValues());
    if (kind == Kind.YES_NO) {
      return g.format(format(c));
    }
    if (CODE_LIST.matcher(allowed).matches()) {
      g = g.choices(Arrays.stream(allowed.split(", ?")).map(v -> new Choice(v, v)).toList());
    } else if (c.getMapSet() != null && !c.getMapSet().isBlank()) {
      g =
          g.allowed(
              "Code as stored in the legacy system, translated by the code map "
                  + c.getMapSet()
                  + (allowed.isEmpty() || allowed.startsWith("Code map") ? "" : "; " + allowed));
    } else {
      g = g.allowed(allowed);
    }
    return g.format(format(c));
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
    String format = text(c.getFormat());
    switch (c.getDataType()) {
      case DATE -> parts.add("Date, e.g. 31-Dec-2027 (an Excel date or yyyy-MM-dd)");
      case TIMESTAMP -> parts.add("Text yyyy-MM-dd HH:mm:ss");
      case AMOUNT -> parts.add("Amount with a dot decimal, e.g. 1500000.00");
      case FLAG -> parts.add("Y or N");
      default -> {
        if (!format.isEmpty()) {
          parts.add(format);
        }
      }
    }
    String length = text(c.getLength());
    if (!length.isEmpty()
        && (c.getDataType() == LayoutColumn.DataType.TEXT
            || c.getDataType() == LayoutColumn.DataType.CODE)) {
      parts.add("at most " + length + " characters");
    }
    return parts.isEmpty() ? "" : String.join(", ", parts);
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

  /**
   * The control file template: one row per measure of a data file.
   *
   * @param columns control columns
   * @return template
   */
  static GuidedTemplate control(List<String> columns) {
    List<GuideColumn> guide = new ArrayList<>();
    for (String name : columns) {
      guide.add(controlColumn(name));
    }
    return new GuidedTemplate(
        "Control file of a migration extract",
        "Proves that a data file arrived complete and unchanged: the intake compares every measure"
            + " with the data file before anything is loaded.",
        FILLED_BY,
        "Data Migration > Extracts, box Upload an extract: upload it together with its data"
            + " file, named <data file name>.ctl.xlsx (or .ctl.csv).",
        List.of(
            "One row per measure: ROW_COUNT, HASH_TOTAL of the key column, AMOUNT_TOTAL of each"
                + " amount column per currency, and SHA256 of the data file.",
            "The same object, layout, source system, data file and as-of date on every row."),
        List.of(
            new GuidedSheet(
                "Control",
                "Control file",
                "One row per measure of the data file.",
                guide,
                List.of(
                    List.of(
                        "F01",
                        "F01C",
                        "EBIX",
                        "F01C_EBIX_20271231_01.xlsx",
                        "2027-12-31",
                        "2027-12-31 18:00:00",
                        "Maria Reyes",
                        "ROW_COUNT",
                        "",
                        "",
                        "",
                        "1250")))),
        CONTROL_ROWS);
  }

  private static GuideColumn controlColumn(String name) {
    return switch (name) {
      case "object" -> GuideColumn.of(name, Kind.TEXT, "Data object of the file").mandatory();
      case "layout" -> GuideColumn.of(name, Kind.TEXT, "Layout code of the file").mandatory();
      case "source_system" ->
          GuideColumn.of(name, Kind.TEXT, "Legacy system the data comes from").mandatory();
      case "data_file" ->
          GuideColumn.of(name, Kind.TEXT, "File name of the data file, as uploaded").mandatory();
      case "as_of" -> GuideColumn.of(name, Kind.DATE, "As-of date of the extract").mandatory();
      case "extracted_at" ->
          GuideColumn.of(name, Kind.TEXT, "When the extract was taken")
              .format("Text yyyy-MM-dd HH:mm:ss");
      case "extracted_by" -> GuideColumn.of(name, Kind.TEXT, "Name of who took the extract");
      case "measure" ->
          GuideColumn.of(name, Kind.TEXT, "Measure of the row")
              .mandatory()
              .choices(
                  List.of(
                      new Choice("ROW_COUNT", "Number of data rows"),
                      new Choice("HASH_TOTAL", "Hash total of the key column"),
                      new Choice("AMOUNT_TOTAL", "Total of an amount column"),
                      new Choice("SHA256", "Checksum of the data file")));
      case "column_name" ->
          GuideColumn.of(name, Kind.TEXT, "Column the measure is on")
              .when("measure is HASH_TOTAL or AMOUNT_TOTAL");
      case "currency" ->
          GuideColumn.of(name, Kind.TEXT, "Currency of an amount total")
              .when("measure is AMOUNT_TOTAL and the file has a currency column");
      case "filter" ->
          GuideColumn.of(name, Kind.TEXT, "Filter of an amount total, when any")
              .format("column=value, e.g. currency=USD");
      default ->
          GuideColumn.of(name, Kind.TEXT, "Value of the measure (the checksum for SHA256)")
              .mandatory();
    };
  }

  private static String text(String v) {
    return v == null ? "" : v.strip();
  }
}
