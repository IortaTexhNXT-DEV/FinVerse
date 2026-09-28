package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
 * the single source of the templates BDOI fills (DATA_MIGRATION_DESIGN section 5.1): the guided
 * Excel template of a layout (title block, column guide above the header, example row), one
 * workbook per object and one of every object (a Start here sheet and one guided sheet per layout),
 * the guided control file template, and the CSV layout of a layout or of the control file (header
 * row only) for large extracts. The guided Excel files are uploaded as they are on the Extracts
 * screen: the intake skips their guide and example rows.
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

  /**
   * The rules of a delivery (topic and rule), shown on every load template (title block or Start
   * here sheet) and in the Migration Workbook.
   */
  public static final List<List<String>> HOW_TO_FILL =
      List.of(
          List.of(
              "File name",
              "<LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.xlsx or .csv, for example F01C_EBIX_20271231_01.xlsx:"
                  + " the layout code, the source system, the as-of date and the sequence of the day"),
          List.of(
              "One file per",
              "Layout, source system and extract; an object with several layouts sends one file per"
                  + " layout with the same as-of date"),
          List.of(
              "Excel",
              "The load template of the console, uploaded as it is: its guide rows and example row"
                  + " are skipped and the sheet of the layout is read; a workbook of several layouts"
                  + " is uploaded once per layout, named after that layout. A plain workbook has the"
                  + " header in row 1 of its first sheet, no merged cells, no formulas"),
          List.of(
              "CSV",
              "For large extracts: UTF-8 without byte order mark, comma separator, double quotes"
                  + " around values that hold a comma, a quote or a line break, one header row with"
                  + " the column names exactly as in the template"),
          List.of(
              "Values",
              "Dates as Excel dates or yyyy-MM-dd; timestamps yyyy-MM-dd HH:mm:ss (Philippine time);"
                  + " amounts with a dot decimal, 2 decimals, no thousands separator, minus sign for"
                  + " negatives; currency ISO 4217; codes exactly as stored in the legacy system;"
                  + " blank means no value"),
          List.of(
              "Control file",
              "<data file name>.ctl.xlsx or .ctl.csv in the control layout: the row count, the hash"
                  + " total of the key column, the total of each amount column per currency and the"
                  + " SHA-256 of the data file, one row per measure"),
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
   * The guided Excel template of a layout.
   *
   * @param layoutCode layout
   * @return XLSX bytes
   */
  public byte[] layoutWorkbook(String layoutCode) {
    Layout layout = layouts.requireCurrent(layoutCode);
    return GuidedTemplateWriter.write(
        LayoutTemplates.single(layout, layouts.columns(layout.getId())));
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
   * The guided Excel template of the control file.
   *
   * @return XLSX bytes
   */
  public byte[] controlWorkbook() {
    return GuidedTemplateWriter.write(ControlTemplate.of(CONTROL_COLUMNS));
  }

  /**
   * The workbook of one object: a Start here sheet and one guided sheet per layout in force (the
   * layout's own template when the object has one layout).
   *
   * @param objectCode object
   * @return XLSX bytes
   */
  public byte[] objectWorkbook(String objectCode) {
    List<Layout> list = layouts.inForce(objectCode);
    if (list.size() == 1) {
      return GuidedTemplateWriter.write(
          LayoutTemplates.single(list.get(0), layouts.columns(list.get(0).getId())));
    }
    MigDataObject object = objects.findByCode(objectCode).orElse(null);
    String name = object == null ? objectCode : objectCode + " " + object.getName();
    return workbook(
        "Load templates of " + name,
        "Delivers the legacy records of the data object " + name + ", one sheet per layout.",
        list);
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
    return workbook(
        "Migration load templates",
        "Delivers the legacy records of every data object, one sheet per layout in force, in the"
            + " load order of the objects.",
        all);
  }

  private byte[] workbook(String name, String purpose, List<Layout> list) {
    if (list.isEmpty()) {
      throw new BusinessRuleException("MIG_NO_LAYOUT", "There is no layout in force to export");
    }
    List<GuidedSheet> sheets = new ArrayList<>();
    for (Layout layout : list) {
      sheets.add(LayoutTemplates.sheet(layout, layouts.columns(layout.getId())));
    }
    return GuidedTemplateWriter.write(LayoutTemplates.workbook(name, purpose, sheets));
  }

  private List<String> header(Layout layout) {
    return layouts.columns(layout.getId()).stream().map(LayoutColumn::getName).toList();
  }
}
