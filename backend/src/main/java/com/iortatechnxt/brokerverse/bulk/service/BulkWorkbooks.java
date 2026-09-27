package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.io.ByteArrayOutputStream;
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

/**
 * Excel files of the framework: the upload template, the result report and the error file to
 * correct and upload again.
 */
final class BulkWorkbooks {

  private static final int WIDTH = 22 * 256;
  private static final int WIDE = WIDTH * 3;
  private static final int DESCRIPTION_COLUMN = 3;
  private static final int EXAMPLE_COLUMN = 4;
  private static final int MESSAGES_COLUMN = 2;
  private static final int REFERENCE_COLUMN = 3;
  private static final int OUTCOME_COLUMN = 4;

  private BulkWorkbooks() {}

  /**
   * The template: data sheet with headers and one example row, and an instructions sheet.
   *
   * @param handler handler
   * @return xlsx bytes
   */
  static byte[] template(BulkImportHandler handler) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      CellStyle head = headStyle(wb);
      dataSheet(wb, head, handler.columns());
      instructionsSheet(wb, head, handler);
      return bytes(wb);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void dataSheet(XSSFWorkbook wb, CellStyle head, List<BulkColumn> columns) {
    Sheet data = wb.createSheet("Data");
    Row header = data.createRow(0);
    Row example = data.createRow(1);
    for (int c = 0; c < columns.size(); c++) {
      BulkColumn col = columns.get(c);
      var cell = header.createCell(c);
      cell.setCellValue(col.header());
      cell.setCellStyle(head);
      example.createCell(c).setCellValue(textOf(col.example()));
      data.setColumnWidth(c, WIDTH);
    }
    data.createFreezePane(0, 1);
  }

  private static void instructionsSheet(
      XSSFWorkbook wb, CellStyle head, BulkImportHandler handler) {
    Sheet help = wb.createSheet("Instructions");
    Row h = help.createRow(0);
    String[] titles = {"Column", "Mandatory", "Type", "What to enter", "Example"};
    for (int i = 0; i < titles.length; i++) {
      var cell = h.createCell(i);
      cell.setCellValue(titles[i]);
      cell.setCellStyle(head);
      help.setColumnWidth(i, i == DESCRIPTION_COLUMN ? WIDE : WIDTH);
    }
    List<BulkColumn> columns = handler.columns();
    for (int c = 0; c < columns.size(); c++) {
      BulkColumn col = columns.get(c);
      Row r = help.createRow(c + 1);
      r.createCell(0).setCellValue(col.header());
      r.createCell(1).setCellValue(col.required() ? "Yes" : "No");
      r.createCell(2).setCellValue(typeText(col.type()));
      r.createCell(DESCRIPTION_COLUMN).setCellValue(col.description());
      r.createCell(EXAMPLE_COLUMN).setCellValue(textOf(col.example()));
    }
    int next = columns.size() + 2;
    help.createRow(next)
        .createCell(0)
        .setCellValue(
            "Replace the example row with your data. Keep the headers unchanged. Delete no"
                + " columns.");
    if (!handler.instructions().isBlank()) {
      help.createRow(next + 1).createCell(0).setCellValue(handler.instructions());
    }
  }

  private static String textOf(String value) {
    return value == null ? "" : value;
  }

  /**
   * The result report: every row with its values, status, messages and reference (BRNB.024/039
   * summary and error report).
   *
   * @param job job
   * @param columns template columns
   * @param rows rows
   * @param values row values by row id
   * @param outcomes committed rows per outcome category (BRQID.006)
   * @param uploadedBy display name of the user who uploaded the file
   * @return xlsx bytes
   */
  static byte[] report(
      BulkJob job,
      List<BulkColumn> columns,
      List<BulkRowRecord> rows,
      Map<Long, Map<String, String>> values,
      Map<String, Long> outcomes,
      String uploadedBy) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      CellStyle head = headStyle(wb);
      summarySheet(wb, job, uploadedBy, outcomes);
      rowsSheet(wb, head, columns, rows, values);
      return bytes(wb);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

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
  static byte[] errorFile(
      List<BulkColumn> columns, List<BulkRowRecord> rows, Map<Long, Map<String, String>> values) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      CellStyle head = headStyle(wb);
      Marks marks = new Marks(errorStyle(wb, false), errorStyle(wb, true), dateStyle(wb));
      Sheet sheet = wb.createSheet("Data");
      Row header = sheet.createRow(0);
      for (int c = 0; c < columns.size(); c++) {
        var cell = header.createCell(c);
        cell.setCellValue(columns.get(c).header());
        cell.setCellStyle(head);
        sheet.setColumnWidth(c, WIDTH);
      }
      int errorColumn = columns.size();
      var errorHead = header.createCell(errorColumn);
      errorHead.setCellValue("Error");
      errorHead.setCellStyle(head);
      sheet.setColumnWidth(errorColumn, WIDE);
      int r = 1;
      for (BulkRowRecord row : rows) {
        if (row.getStatus() == BulkRowStatus.COMMITTED) {
          continue;
        }
        errorRow(
            sheet.createRow(r++), columns, row, values.getOrDefault(row.getId(), Map.of()), marks);
      }
      sheet.createFreezePane(0, 1);
      return bytes(wb);
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
    String messages = textOf(row.getMessages());
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

  private static String statusText(BulkJobStatus status) {
    return switch (status) {
      case VALIDATED -> "Validated, waiting for commit";
      case COMPLETED -> "Completed";
      case CANCELLED -> "Cancelled";
    };
  }

  private static String rowStatusText(BulkRowStatus status) {
    String words = status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(words.charAt(0)) + words.substring(1);
  }

  private static void summarySheet(
      XSSFWorkbook wb, BulkJob job, String uploadedBy, Map<String, Long> outcomes) {
    Sheet summary = wb.createSheet("Summary");
    Object[][] facts = {
      {"Upload", job.getJobNo()},
      {"File", job.getFileName()},
      {"Status", statusText(job.getStatus())},
      {"Rows", job.getTotalRows()},
      {"Valid", job.getValidRows()},
      {"Invalid", job.getInvalidRows()},
      {"Committed", job.getCommittedRows()},
      {"Failed at commit", job.getFailedRows()},
      {"Uploaded by", uploadedBy},
      {"Uploaded at", DisplayFormat.dateTime(job.getCreatedAt())},
      {"Reprocessed", job.getReprocessCount()}
    };
    for (int i = 0; i < facts.length; i++) {
      Row r = summary.createRow(i);
      r.createCell(0).setCellValue(String.valueOf(facts[i][0]));
      r.createCell(1).setCellValue(String.valueOf(facts[i][1]));
    }
    int next = facts.length + 1;
    for (Map.Entry<String, Long> outcome : outcomes.entrySet()) {
      Row r = summary.createRow(next++);
      r.createCell(0).setCellValue("Outcome " + outcome.getKey());
      r.createCell(1).setCellValue(String.valueOf(outcome.getValue()));
    }
    summary.setColumnWidth(0, WIDTH);
    summary.setColumnWidth(1, WIDTH * 2);
  }

  private static void rowsSheet(
      XSSFWorkbook wb,
      CellStyle head,
      List<BulkColumn> columns,
      List<BulkRowRecord> rows,
      Map<Long, Map<String, String>> values) {
    Sheet sheet = wb.createSheet("Rows");
    Row header = sheet.createRow(0);
    String[] fixed = {"Row", "Status", "Messages", "Reference", "Outcome"};
    for (int i = 0; i < fixed.length; i++) {
      var cell = header.createCell(i);
      cell.setCellValue(fixed[i]);
      cell.setCellStyle(head);
      sheet.setColumnWidth(i, i == MESSAGES_COLUMN ? WIDE : WIDTH);
    }
    for (int c = 0; c < columns.size(); c++) {
      var cell = header.createCell(fixed.length + c);
      cell.setCellValue(columns.get(c).header());
      cell.setCellStyle(head);
      sheet.setColumnWidth(fixed.length + c, WIDTH);
    }
    int r = 1;
    for (BulkRowRecord row : rows) {
      Row out = sheet.createRow(r++);
      out.createCell(0).setCellValue(row.getRowNo());
      out.createCell(1).setCellValue(rowStatusText(row.getStatus()));
      out.createCell(MESSAGES_COLUMN).setCellValue(textOf(row.getMessages()));
      out.createCell(REFERENCE_COLUMN).setCellValue(textOf(row.getResultRef()));
      out.createCell(OUTCOME_COLUMN).setCellValue(textOf(row.getOutcome()));
      Map<String, String> v = values.getOrDefault(row.getId(), Map.of());
      for (int c = 0; c < columns.size(); c++) {
        out.createCell(fixed.length + c).setCellValue(v.getOrDefault(columns.get(c).header(), ""));
      }
    }
    sheet.createFreezePane(0, 1);
  }

  private static String typeText(BulkColumn.Type type) {
    return switch (type) {
      case TEXT -> "Text";
      case NUMBER -> "Number (e.g. 1500000.00)";
      case DATE -> "Date (yyyy-mm-dd)";
      case YES_NO -> "Y or N";
    };
  }

  private static CellStyle headStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    Font font = wb.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private static byte[] bytes(XSSFWorkbook wb) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    wb.write(out);
    return out.toByteArray();
  }
}
