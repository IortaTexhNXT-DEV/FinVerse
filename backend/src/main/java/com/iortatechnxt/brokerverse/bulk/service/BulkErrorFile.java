package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** The error file of an upload: the rows that were not processed, to correct and upload again. */
final class BulkErrorFile {

  private BulkErrorFile() {}

  /**
   * The error file: the rows that were not processed, in the template layout (same headers, so the
   * corrected file can be uploaded again) with an "Error" column last. The cells a message names
   * are highlighted, and the Error cell of every rejected row.
   *
   * @param columns template columns
   * @param rows rows of the job
   * @param values row values by row id
   * @return xlsx bytes
   */
  static byte[] write(
      List<BulkColumn> columns, List<BulkRowRecord> rows, Map<Long, Map<String, String>> values) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      CellStyle head = BulkWorkbooks.headStyle(wb);
      Marks marks = new Marks(errorStyle(wb, false), errorStyle(wb, true), dateStyle(wb));
      Sheet sheet = wb.createSheet("Data");
      Row header = sheet.createRow(0);
      for (int c = 0; c < columns.size(); c++) {
        var cell = header.createCell(c);
        cell.setCellValue(columns.get(c).header());
        cell.setCellStyle(head);
        sheet.setColumnWidth(c, BulkWorkbooks.WIDTH);
      }
      int errorColumn = columns.size();
      var errorHead = header.createCell(errorColumn);
      errorHead.setCellValue("Error");
      errorHead.setCellStyle(head);
      sheet.setColumnWidth(errorColumn, BulkWorkbooks.WIDE);
      int r = 1;
      for (BulkRowRecord row : rows) {
        if (row.getStatus() == BulkRowStatus.COMMITTED) {
          continue;
        }
        errorRow(
            sheet.createRow(r++), columns, row, values.getOrDefault(row.getId(), Map.of()), marks);
      }
      sheet.createFreezePane(0, 1);
      return BulkWorkbooks.bytes(wb);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Styles of the error file: an offending cell, an offending date cell, and a date cell (dates are
   * real date cells shown as dd-MMM-yyyy; the upload reads them back as dates).
   */
  private record Marks(CellStyle marked, CellStyle markedDate, CellStyle date) {}

  /** One rejected row of the error file: its values, then its messages in the Error column. */
  private static void errorRow(
      Row out, List<BulkColumn> columns, BulkRowRecord row, Map<String, String> v, Marks marks) {
    String messages = BulkWorkbooks.textOf(row.getMessages());
    String lower = messages.toLowerCase(Locale.ROOT);
    for (int c = 0; c < columns.size(); c++) {
      String name = columns.get(c).header();
      var cell = out.createCell(c);
      boolean marked = !messages.isEmpty() && lower.contains(name.toLowerCase(Locale.ROOT));
      LocalDate date = columns.get(c).type() == BulkColumn.Type.DATE ? dateOf(v.get(name)) : null;
      if (date == null) {
        cell.setCellValue(v.getOrDefault(name, ""));
        if (marked) {
          cell.setCellStyle(marks.marked());
        }
      } else {
        cell.setCellValue(date);
        cell.setCellStyle(marked ? marks.markedDate() : marks.date());
      }
    }
    var error = out.createCell(columns.size());
    error.setCellValue(messages);
    if (!messages.isEmpty()) {
      error.setCellStyle(marks.marked());
    }
  }

  /** The date of a value entered as yyyy-MM-dd, or null when it is blank or not a date. */
  private static LocalDate dateOf(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.strip());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static CellStyle dateStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    style.setDataFormat(wb.createDataFormat().getFormat(DisplayFormat.SHEET_DATE_FORMAT));
    return style;
  }

  private static CellStyle errorStyle(XSSFWorkbook wb, boolean date) {
    CellStyle style = wb.createCellStyle();
    if (date) {
      style.setDataFormat(wb.createDataFormat().getFormat(DisplayFormat.SHEET_DATE_FORMAT));
    }
    Font font = wb.createFont();
    font.setColor(IndexedColors.DARK_RED.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }
}
