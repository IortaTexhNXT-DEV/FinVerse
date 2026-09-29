package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * The guided template of a bulk upload type: the handler's title, who fills it in, where it is
 * uploaded and its rules, one "Data" sheet whose column guide comes from the handler's columns, and
 * the lists of values of the platform as drop-downs.
 */
final class BulkTemplates {

  /** Name of the data sheet (template and error file). */
  static final String DATA_SHEET = "Data";

  /** Header of the error column of the error file. */
  static final String ERROR_COLUMN = "Error";

  private static final int ERROR_WIDTH = 60;

  private BulkTemplates() {}

  /**
   * The template of a handler.
   *
   * @param handler handler
   * @param lists platform lists of values
   * @param maxRows largest number of rows of a file
   * @return template
   */
  static GuidedTemplate of(BulkImportHandler handler, BulkTemplateLists lists, int maxRows) {
    return of(handler, lists, maxRows, null);
  }

  /**
   * The template of a handler for a company: currency examples show its base currency.
   *
   * @param handler handler
   * @param lists platform lists of values
   * @param maxRows largest number of rows of a file
   * @param baseCurrency base currency of the company the template is for, may be null
   * @return template
   */
  static GuidedTemplate of(
      BulkImportHandler handler, BulkTemplateLists lists, int maxRows, String baseCurrency) {
    List<GuideColumn> columns =
        handler.columns().stream().map(c -> column(c.forCompany(baseCurrency), lists)).toList();
    GuidedSheet sheet =
        GuidedSheet.of(DATA_SHEET, handler.title(), handler.instructions(), columns);
    return new GuidedTemplate(
        handler.title(),
        "Upload file of the " + handler.title() + " screen.",
        handler.filledBy(),
        handler.uploadPath()
            + ": download this template, fill it in, upload it, check the validation result of"
            + " every row, then commit the valid rows.",
        rules(handler, maxRows),
        List.of(sheet),
        maxRows);
  }

  /**
   * The error file layout of a handler: the template's sheet without the example, with an Error
   * column last.
   *
   * @param template template of the handler
   * @return template of the error file
   */
  static GuidedTemplate errorFile(GuidedTemplate template) {
    GuidedSheet data = template.sheets().get(0);
    List<GuideColumn> columns = new ArrayList<>(data.columns());
    columns.add(
        GuideColumn.of(
                ERROR_COLUMN,
                Kind.TEXT,
                "Why the row was not processed. Correct the highlighted cells and upload this file"
                    + " again; this column is not read.")
            .width(ERROR_WIDTH));
    GuidedSheet sheet =
        new GuidedSheet(
            data.name(),
            data.title(),
            "The rows that were not processed, to correct and upload again. " + data.intro(),
            columns,
            List.of());
    return template.withSheets(List.of(sheet));
  }

  private static List<String> rules(BulkImportHandler handler, int maxRows) {
    List<String> rules = new ArrayList<>();
    rules.add(
        "At most "
            + maxRows
            + " rows per file, for one company: the company chosen on screen when uploading.");
    rules.add(
        "Dates as dd-MMM-yyyy (e.g. 15-Jan-2026) or as Excel dates; amounts and numbers without"
            + " thousands separators, or as Excel numbers.");
    rules.add(
        "Keep the header texts and the order of the columns; do not delete columns. A blank cell"
            + " means no value.");
    rules.addAll(handler.rules());
    return rules;
  }

  /**
   * The guide of one column.
   *
   * @param c column
   * @param lists platform lists of values
   * @return guide column
   */
  static GuideColumn column(BulkColumn c, BulkTemplateLists lists) {
    GuideColumn g =
        GuideColumn.of(c.header(), kind(c.type()), c.description())
            .mandatory(c.required())
            .example(c.example());
    if (!c.required() && !c.condition().isEmpty()) {
      g = g.when(c.condition());
    }
    List<Choice> choices = c.choices();
    String allowed = c.allowed();
    if (choices.isEmpty() && !c.lov().isEmpty()) {
      choices = lists.choices(c.lov());
      if (choices.isEmpty() && allowed.isEmpty()) {
        allowed = "Code of the list " + BulkColumn.label(c.lov()) + " (Lists of Values)";
      }
    }
    g = g.choices(choices).allowed(allowed);
    if (!c.format().isEmpty()) {
      g = g.format(c.format());
    }
    return g;
  }

  private static Kind kind(BulkColumn.Type type) {
    return switch (type) {
      case TEXT -> Kind.TEXT;
      case NUMBER -> Kind.NUMBER;
      case DATE -> Kind.DATE;
      case YES_NO -> Kind.YES_NO;
    };
  }
}
