package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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
 * The result report of an upload and the shared cell helpers of the framework's workbooks (the
 * template is a guided template, see {@link BulkTemplates}; the error file {@link BulkErrorFile}).
 */
final class BulkWorkbooks {

  static final int WIDTH = 22 * 256;
  static final int WIDE = WIDTH * 3;
  private static final int MESSAGES_COLUMN = 2;
  private static final int REFERENCE_COLUMN = 3;
  private static final int OUTCOME_COLUMN = 4;

  private BulkWorkbooks() {}

  static String textOf(String value) {
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

  private static String statusText(BulkJobStatus status) {
    return switch (status) {
      case VALIDATED -> "Validated, waiting for commit";
      case SUBMITTED -> "Submitted, waiting for approval";
      case REJECTED -> "Rejected";
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

  static CellStyle headStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    Font font = wb.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  static byte[] bytes(XSSFWorkbook wb) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    wb.write(out);
    return out.toByteArray();
  }
}
