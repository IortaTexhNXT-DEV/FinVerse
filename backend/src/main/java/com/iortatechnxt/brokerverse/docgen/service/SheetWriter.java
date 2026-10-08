package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.common.util.Money;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes the spreadsheets of the document composer: a branded heading row, frozen panes, real date
 * and number cells shown as people read them (17-Oct-2026, 80,000,000.00), and columns as wide as
 * their content so that a date or amount never runs into the next column.
 */
final class SheetWriter {

  private SheetWriter() {}

  /**
   * Renders a workbook with one sheet per spec, in order.
   *
   * @param specs sheets (at least one)
   * @return xlsx bytes
   */
  static byte[] write(List<SheetSpec> specs) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      XSSFCellStyle head = wb.createCellStyle();
      org.apache.poi.ss.usermodel.Font font = wb.createFont();
      font.setBold(true);
      font.setColor(IndexedColors.WHITE.getIndex());
      head.setFont(font);
      head.setFillForegroundColor(brand());
      head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      head.setWrapText(true);
      head.setVerticalAlignment(VerticalAlignment.TOP);
      CellStyle date = wb.createCellStyle();
      date.setDataFormat(
          wb.getCreationHelper().createDataFormat().getFormat(DisplayFormat.SHEET_DATE_FORMAT));
      CellStyle amount = wb.createCellStyle();
      amount.setDataFormat(
          wb.getCreationHelper().createDataFormat().getFormat(DisplayFormat.SHEET_AMOUNT_FORMAT));
      CellStyles styles = new CellStyles(head, date, amount);
      for (SheetSpec spec : specs) {
        writeSheet(wb.createSheet(spec.sheetName()), spec, styles);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Cell styles of a workbook: heading row, dates and amounts. */
  private record CellStyles(CellStyle head, CellStyle date, CellStyle amount) {}

  private static void writeSheet(Sheet sheet, SheetSpec spec, CellStyles styles) {
    Row header = sheet.createRow(0);
    for (int c = 0; c < spec.headers().size(); c++) {
      var cell = header.createCell(c);
      cell.setCellValue(spec.headers().get(c));
      cell.setCellStyle(styles.head());
    }
    int r = 1;
    for (List<Object> values : spec.rows()) {
      Row row = sheet.createRow(r++);
      for (int c = 0; c < values.size(); c++) {
        write(row.createCell(c), values.get(c), styles);
      }
    }
    sheet.createFreezePane(0, 1);
    SheetColumnWidths.fit(sheet, 0, spec.headers().size());
  }

  /** Header Blue of the brand. */
  private static XSSFColor brand() {
    java.awt.Color c = BrandAssets.color(BrandAssets.HEADER);
    return new XSSFColor(
        new byte[] {(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()},
        new DefaultIndexedColorMap());
  }

  private static void write(
      org.apache.poi.ss.usermodel.Cell cell, Object value, CellStyles styles) {
    switch (value) {
      case null -> cell.setBlank();
      case BigDecimal n -> {
        cell.setCellValue(n.doubleValue());
        if (n.scale() == Money.SCALE) {
          cell.setCellStyle(styles.amount());
        }
      }
      case Number n -> cell.setCellValue(n.doubleValue());
      case LocalDate d -> {
        cell.setCellValue(d);
        cell.setCellStyle(styles.date());
      }
      case Boolean b -> cell.setCellValue(Boolean.TRUE.equals(b) ? "Y" : "N");
      default -> cell.setCellValue(value.toString());
    }
  }
}
