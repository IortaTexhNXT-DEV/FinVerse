package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.common.excel.SheetLogo;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/** Column widths and the logo of the Excel export of a report (see {@link XlsxReportRenderer}). */
final class XlsxReportLayout {

  private static final float LOGO_ROW_POINTS = 30f;
  private static final double LOGO_POINTS = 22;
  private static final int UNITS_PER_CHAR = 256;
  // Width of a character of the default font in points (7 pixels at 96 dpi).
  private static final double POINTS_PER_CHAR = 5.25;

  private XlsxReportLayout() {}

  /**
   * Sets every column as wide as its heading and longest value (as shown) plus a margin; the label
   * column, when there is one, as wide as its labels.
   *
   * @return the width of the sheet's columns in points, for the logo
   */
  static double columnWidths(Sheet sheet, ReportResult result, boolean labels) {
    List<ReportColumn> cols = result.columns();
    int first = labels ? 1 : 0;
    int[] chars = new int[cols.size() + first];
    for (int i = 0; i < cols.size(); i++) {
      chars[i + first] = SheetColumnWidths.heading(cols.get(i).label());
    }
    for (ReportRow row : result.rows()) {
      if (labels && row.kind() != RowKind.GROUP_HEADER && row.kind() != RowKind.SECTION) {
        chars[0] = Math.max(chars[0], LabelLayout.label(row).length());
      }
      for (int i = 0; i < cols.size(); i++) {
        ReportColumn c = cols.get(i);
        String shown = CellFormatter.format(row.cells().get(c.key()), c.type());
        chars[i + first] = Math.max(chars[i + first], shown.length());
      }
    }
    double points = 0;
    for (int i = 0; i < chars.length; i++) {
      int units = SheetColumnWidths.units(chars[i]);
      sheet.setColumnWidth(i, units);
      points += units / (double) UNITS_PER_CHAR * POINTS_PER_CHAR;
    }
    return points;
  }

  /**
   * The BDO Insure logo in the first row, above the company, anchored over as many columns as its
   * width needs (the first column may be narrower than the logo).
   */
  static void logo(SXSSFWorkbook wb, Sheet sheet, double sheetPoints) {
    Row row = sheet.createRow(0);
    row.setHeightInPoints(LOGO_ROW_POINTS);
    double height = Math.min(LOGO_POINTS, sheetPoints / BrandAssets.LOGO_RATIO);
    SheetLogo.place(wb, sheet, height);
  }
}
