package com.iortatechnxt.brokerverse.common.excel;

import static com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter.CHARACTER;
import static com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter.FIRST_COLUMN;
import static com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter.LINE_POINTS;

import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * The column guide band of a guided sheet (Mandatory, Format, Allowed values, What to enter, one
 * cell above each column) and the header row below it, with the note of every header.
 */
final class GuidedBand {

  private static final int BAND_PADDING = 3;
  private static final int HEADER_PADDING = 4;
  private static final int ALLOWED_LINES = 8;
  private static final int WHAT_LINES = 6;
  private static final int OTHER_LINES = 4;

  private GuidedBand() {}

  /** The four guide rows; returns the header row index. */
  static int band(Sheet sheet, GuidedStyles styles, List<GuideColumn> columns, int from) {
    int r = from;
    for (String label : GuidedTables.BAND_LABELS) {
      Row row = sheet.createRow(r);
      GuidedTemplateWriter.text(row, 0, label, styles.bandLabel());
      int height = 1;
      for (int c = 0; c < columns.size(); c++) {
        GuideColumn col = columns.get(c);
        String value = bandValue(label, col);
        CellStyle style =
            "Mandatory".equals(label) && col.need() != GuideColumn.Need.NO
                ? styles.bandMandatory()
                : styles.band();
        GuidedTemplateWriter.text(row, FIRST_COLUMN + c, value, style);
        int width = sheet.getColumnWidth(FIRST_COLUMN + c) / CHARACTER;
        height = Math.max(height, GuidedTemplateWriter.lines(value, width));
      }
      row.setHeightInPoints(LINE_POINTS * Math.min(height, maxLines(label)) + BAND_PADDING);
      r++;
    }
    return r;
  }

  private static int maxLines(String label) {
    return switch (label) {
      case "Allowed values" -> ALLOWED_LINES;
      case "What to enter" -> WHAT_LINES;
      default -> OTHER_LINES;
    };
  }

  private static String bandValue(String label, GuideColumn col) {
    return switch (label) {
      case "Mandatory" -> col.needText();
      case "Format" -> col.formatText();
      case "Allowed values" -> col.allowedText(GuidedLists.SHOWN_IN_BAND);
      default -> col.whatToEnter();
    };
  }

  static void headerRow(Sheet sheet, GuidedStyles styles, List<GuideColumn> columns, int r) {
    Row row = sheet.createRow(r);
    GuidedTemplateWriter.text(row, 0, GuidedTables.HEADER_CORNER, styles.headerCorner());
    int height = 1;
    for (int c = 0; c < columns.size(); c++) {
      GuideColumn col = columns.get(c);
      Cell cell =
          GuidedTemplateWriter.text(row, FIRST_COLUMN + c, col.shownHeader(), styles.header());
      GuidedChecks.note(sheet, cell, col);
      int width = sheet.getColumnWidth(FIRST_COLUMN + c) / CHARACTER;
      height = Math.max(height, GuidedTemplateWriter.lines(col.shownHeader(), width));
    }
    row.setHeightInPoints((LINE_POINTS + 2) * height + HEADER_PADDING);
  }
}
