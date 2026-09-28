package com.iortatechnxt.brokerverse.common.excel;

import java.util.List;

/**
 * A fill-in template for users: what it is, who fills it in, how it is uploaded, the rules of the
 * whole file and its data sheet(s). One sheet is the rule; several sheets (e.g. header and lines)
 * get a "Start here" sheet with the order and links.
 *
 * @param name template name (title of the file)
 * @param purpose what the file is for
 * @param filledBy who fills it in
 * @param howToUpload how it is uploaded (screen and menu path in words)
 * @param rules rules of the whole file (one sentence each)
 * @param sheets data sheets in the order they are filled in
 * @param maxRows largest number of data rows of a sheet (drop-downs and checks cover them)
 */
public record GuidedTemplate(
    String name,
    String purpose,
    String filledBy,
    String howToUpload,
    List<String> rules,
    List<GuidedSheet> sheets,
    int maxRows) {

  /** Default number of rows covered by the drop-downs and checks. */
  public static final int DEFAULT_MAX_ROWS = 5000;

  /** Copies of the lists, null-safe texts. */
  public GuidedTemplate {
    purpose = purpose == null ? "" : purpose;
    filledBy = filledBy == null ? "" : filledBy;
    howToUpload = howToUpload == null ? "" : howToUpload;
    rules = rules == null ? List.of() : List.copyOf(rules);
    sheets = List.copyOf(sheets);
    if (sheets.isEmpty()) {
      throw new IllegalArgumentException("A template has at least one data sheet");
    }
    maxRows = maxRows <= 0 ? DEFAULT_MAX_ROWS : maxRows;
  }

  /**
   * A one-sheet template.
   *
   * @param name template name
   * @param purpose what it is for
   * @param filledBy who fills it in
   * @param howToUpload how it is uploaded
   * @param rules rules of the file
   * @param sheet the data sheet
   * @return template
   */
  public static GuidedTemplate single(
      String name,
      String purpose,
      String filledBy,
      String howToUpload,
      List<String> rules,
      GuidedSheet sheet) {
    return new GuidedTemplate(
        name, purpose, filledBy, howToUpload, rules, List.of(sheet), DEFAULT_MAX_ROWS);
  }

  /**
   * This template covering another number of rows.
   *
   * @param rows largest number of data rows
   * @return template
   */
  public GuidedTemplate withMaxRows(int rows) {
    return new GuidedTemplate(name, purpose, filledBy, howToUpload, rules, sheets, rows);
  }

  /**
   * This template with other sheets (e.g. the error file's sheet without examples).
   *
   * @param list sheets
   * @return template
   */
  public GuidedTemplate withSheets(List<GuidedSheet> list) {
    return new GuidedTemplate(name, purpose, filledBy, howToUpload, rules, list, maxRows);
  }
}
