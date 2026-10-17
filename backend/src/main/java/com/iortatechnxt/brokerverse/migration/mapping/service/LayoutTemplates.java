package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import java.util.List;

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
        columns.stream().map(LayoutColumnGuide::column).toList());
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
}
