package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * The guided Excel template of the control file sent with every data file of a migration extract:
 * one row per measure (row count, hash total, amount totals, checksum).
 */
final class ControlTemplate {

  private static final int CONTROL_ROWS = 200;
  private static final List<String> IDENTITY =
      List.of(
          "object",
          "layout",
          "source_system",
          "data_file",
          "as_of",
          "extracted_at",
          "extracted_by");

  private ControlTemplate() {}

  /**
   * The control file template: one row per measure of a data file.
   *
   * @param columns control columns
   * @return template
   */
  static GuidedTemplate of(List<String> columns) {
    List<GuideColumn> guide = new ArrayList<>();
    for (String name : columns) {
      guide.add(controlColumn(name));
    }
    return new GuidedTemplate(
        "Control file of a migration extract",
        "Proves that a data file arrived complete and unchanged: the intake compares every measure"
            + " with the data file before anything is loaded.",
        LayoutTemplates.FILLED_BY,
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
    return IDENTITY.contains(name) ? identityColumn(name) : measureColumn(name);
  }

  private static GuideColumn identityColumn(String name) {
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
      default -> GuideColumn.of(name, Kind.TEXT, "Name of who took the extract");
    };
  }

  private static GuideColumn measureColumn(String name) {
    return switch (name) {
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
}
