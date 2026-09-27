package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.migration.common.service.Workbooks;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObjectRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The load templates generated from the layout versions in force, so the layout in the system is
 * the single source of the templates BDOI fills (DATA_MIGRATION_DESIGN section 5.1): one CSV per
 * layout (the header row only), one workbook per object, and one workbook of every object with a
 * sheet per layout, the column descriptions, the control file and the filling instructions.
 */
@Service
@Transactional(readOnly = true)
public class TemplateExport {

  /** Columns of the control file sent with every data file (one row per measure). */
  public static final List<String> CONTROL_COLUMNS =
      List.of(
          "object",
          "layout",
          "source_system",
          "data_file",
          "as_of",
          "extracted_at",
          "extracted_by",
          "measure",
          "column_name",
          "currency",
          "filter",
          "value");

  private static final List<String> COLUMN_HEADERS =
      List.of(
          "layout",
          "seq",
          "name",
          "description",
          "type",
          "length",
          "mandatory",
          "allowed values or code map",
          "format",
          "example",
          "validation");

  private static final List<List<String>> HOW_TO_FILL =
      List.of(
          List.of(
              "File name",
              "<LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv or .xlsx, for example F01C_EBIX_20271231_01.csv:"
                  + " the layout code, the source system, the as-of date and the sequence of the day"),
          List.of(
              "One file per",
              "Layout, source system and extract; an object with several layouts sends one file per"
                  + " layout with the same as-of date"),
          List.of(
              "CSV",
              "UTF-8 without byte order mark, comma separator, double quotes around values that hold"
                  + " a comma, a quote or a line break, one header row with the column names exactly"
                  + " as in the template"),
          List.of("Excel", "First sheet, header in row 1, no merged cells, no formulas"),
          List.of(
              "Values",
              "Dates yyyy-MM-dd; timestamps yyyy-MM-dd HH:mm:ss (Philippine time); amounts with a dot"
                  + " decimal, 2 decimals, no thousands separator, minus sign for negatives; currency"
                  + " ISO 4217; codes exactly as stored in the legacy system; blank means no value"),
          List.of(
              "Control file",
              "<data file name>.ctl.csv in the control layout: the row count, the hash total of the"
                  + " key column, the total of each amount column per currency and the SHA-256 of the"
                  + " data file, one row per measure"),
          List.of(
              "Delivery",
              "Upload the data file and its control file together on the Extracts screen of the"
                  + " Migration Console; never by e-mail"),
          List.of(
              "Checks",
              "A file whose checksum, header, row count, amount totals or hash total does not agree"
                  + " with its control file is rejected with the reason; nothing of it is loaded"));

  private final LayoutService layouts;
  private final MigDataObjectRepository objects;

  /**
   * Creates the export.
   *
   * @param layouts layouts
   * @param objects data objects
   */
  public TemplateExport(LayoutService layouts, MigDataObjectRepository objects) {
    this.layouts = layouts;
    this.objects = objects;
  }

  /**
   * The CSV template of a layout (header row of the version in force).
   *
   * @param layoutCode layout
   * @return CSV bytes
   */
  public byte[] layoutCsv(String layoutCode) {
    Layout layout = layouts.requireCurrent(layoutCode);
    return Workbooks.csv(header(layout), List.of());
  }

  /**
   * The CSV template of the control file.
   *
   * @return CSV bytes
   */
  public byte[] controlCsv() {
    return Workbooks.csv(CONTROL_COLUMNS, List.of());
  }

  /**
   * The workbook of one object: a sheet per layout in force, the columns, the control file and the
   * filling instructions.
   *
   * @param objectCode object
   * @return XLSX bytes
   */
  public byte[] objectWorkbook(String objectCode) {
    return workbook(layouts.inForce(objectCode));
  }

  /**
   * The workbook of every object with layouts.
   *
   * @return XLSX bytes
   */
  public byte[] fullWorkbook() {
    List<Layout> all = new ArrayList<>();
    for (MigDataObject o : objects.findAllByOrderByLoadOrderAscCodeAsc()) {
      all.addAll(layouts.inForce(o.getCode()));
    }
    return workbook(all);
  }

  private byte[] workbook(List<Layout> list) {
    try (Workbooks wb = Workbooks.create()) {
      List<List<String>> described = new ArrayList<>();
      for (Layout layout : list) {
        wb.sheet(sheetName(layout), header(layout), List.of());
        for (LayoutColumn c : layouts.columns(layout.getId())) {
          described.add(describe(layout, c));
        }
      }
      wb.sheet("Columns", COLUMN_HEADERS, described);
      wb.sheet("Control file", CONTROL_COLUMNS, List.of());
      wb.sheet("How to fill", List.of("Topic", "Rule"), HOW_TO_FILL);
      return wb.bytes();
    }
  }

  private static String sheetName(Layout layout) {
    String name = layout.getCode() + " " + layout.getTitle().replaceAll("[\\\\/?*\\[\\]:]", " ");
    return name.length() > 31 ? name.substring(0, 31).trim() : name;
  }

  private List<String> header(Layout layout) {
    return layouts.columns(layout.getId()).stream().map(LayoutColumn::getName).toList();
  }

  private static List<String> describe(Layout layout, LayoutColumn c) {
    String allowed = c.getMapSet() != null ? "Code map " + c.getMapSet() : c.getAllowedValues();
    return List.of(
        layout.getCode(),
        String.valueOf(c.getSeq()),
        c.getName(),
        c.getDescription(),
        c.getDataType().name(),
        value(c.getLength()),
        c.getMandatory(),
        value(allowed),
        value(c.getFormat()),
        value(c.getExample()),
        value(c.getValidation()));
  }

  private static String value(String v) {
    return v == null ? "" : v;
  }
}
