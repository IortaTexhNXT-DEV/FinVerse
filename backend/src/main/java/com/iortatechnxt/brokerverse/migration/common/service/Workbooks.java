package com.iortatechnxt.brokerverse.migration.common.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Small workbook builder of the migration exports (code maps, load templates and the template
 * workbook, rejection files, runbook): named sheets with a styled header row and text cells, so
 * dates and codes keep the exact text the layouts require.
 */
public final class Workbooks implements AutoCloseable {

  private static final int WIDTH = 20 * 256;
  private static final int MAX_WIDTH = 60 * 256;
  private static final int CHARACTER = 256;

  private final XSSFWorkbook wb = new XSSFWorkbook();
  private final CellStyle head;

  private Workbooks() {
    head = wb.createCellStyle();
    Font bold = wb.createFont();
    bold.setBold(true);
    bold.setColor(IndexedColors.WHITE.getIndex());
    head.setFont(bold);
    head.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
    head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
  }

  /**
   * A new workbook.
   *
   * @return builder
   */
  public static Workbooks create() {
    return new Workbooks();
  }

  /**
   * Adds a sheet.
   *
   * @param name sheet name
   * @param headers header row
   * @param rows data rows (text)
   * @return this builder
   */
  public Workbooks sheet(String name, List<String> headers, List<List<String>> rows) {
    Sheet sheet = wb.createSheet(name);
    Row h = sheet.createRow(0);
    for (int c = 0; c < headers.size(); c++) {
      Cell cell = h.createCell(c);
      cell.setCellValue(headers.get(c));
      cell.setCellStyle(head);
      sheet.setColumnWidth(
          c, Math.min(MAX_WIDTH, Math.max(WIDTH, headers.get(c).length() * CHARACTER)));
    }
    for (int r = 0; r < rows.size(); r++) {
      Row row = sheet.createRow(r + 1);
      List<String> values = rows.get(r);
      for (int c = 0; c < values.size(); c++) {
        row.createCell(c).setCellValue(values.get(c) == null ? "" : values.get(c));
      }
    }
    sheet.createFreezePane(0, 1);
    return this;
  }

  /**
   * The workbook as XLSX bytes.
   *
   * @return bytes
   */
  public byte[] bytes() {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    try {
      wb.close();
    } catch (IOException e) {
      throw new BusinessRuleException("MIG_WORKBOOK", "The workbook could not be written", e);
    }
  }

  /**
   * A CSV file (UTF-8 without BOM, comma separated, RFC 4180 quotes).
   *
   * @param headers header row
   * @param rows rows
   * @return bytes
   */
  public static byte[] csv(List<String> headers, List<List<String>> rows) {
    StringBuilder out = new StringBuilder();
    line(out, headers);
    rows.forEach(r -> line(out, r));
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void line(StringBuilder out, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(quote(values.get(i)));
    }
    out.append("\r\n");
  }

  private static String quote(String value) {
    if (value == null) {
      return "";
    }
    boolean needs = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0;
    return needs ? '"' + value.replace("\"", "\"\"") + '"' : value;
  }
}
