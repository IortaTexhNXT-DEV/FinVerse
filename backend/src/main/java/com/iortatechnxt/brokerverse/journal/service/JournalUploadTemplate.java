package com.iortatechnxt.brokerverse.journal.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Downloadable journal upload templates (CSV and XLSX) with two sample vouchers. */
public final class JournalUploadTemplate {

  private JournalUploadTemplate() {}

  /**
   * Sample rows in template column order.
   *
   * @param valueDate value date of the samples
   * @return rows (without header)
   */
  static List<List<String>> sampleRows(LocalDate valueDate) {
    String date = valueDate.toString();
    return List.of(
        List.of(
            "V1",
            "HO",
            "MANUAL",
            date,
            "PHP",
            "Office rent for the month",
            "INV-2026-001",
            "5603",
            "15000.00",
            "",
            "",
            "",
            "FIN",
            "",
            "",
            "",
            "Rent - head office"),
        List.of("V1", "", "", "", "", "", "", "1111", "", "15000.00", "", "", "", "", "", "", ""),
        List.of(
            "V2",
            "HO",
            "ACCRUAL",
            date,
            "PHP",
            "Accrued professional fees",
            "",
            "5605",
            "8000.00",
            "",
            "",
            "",
            "FIN",
            "",
            "",
            "",
            ""),
        List.of("V2", "", "", "", "", "", "", "2502", "", "8000.00", "", "", "", "", "", "", ""));
  }

  /**
   * CSV template.
   *
   * @param valueDate value date of the samples
   * @return UTF-8 CSV bytes
   */
  public static byte[] csv(LocalDate valueDate) {
    StringBuilder text = new StringBuilder(String.join(",", UploadLine.ALL_COLUMNS)).append("\r\n");
    for (List<String> row : sampleRows(valueDate)) {
      text.append(row.stream().map(JournalUploadTemplate::quote).collect(Collectors.joining(",")))
          .append("\r\n");
    }
    return text.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * XLSX template.
   *
   * @param valueDate value date of the samples
   * @return workbook bytes
   */
  public static byte[] xlsx(LocalDate valueDate) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Journals");
      write(sheet.createRow(0), UploadLine.ALL_COLUMNS);
      List<List<String>> rows = sampleRows(valueDate);
      for (int i = 0; i < rows.size(); i++) {
        write(sheet.createRow(i + 1), rows.get(i));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void write(Row row, List<String> values) {
    for (int c = 0; c < values.size(); c++) {
      row.createCell(c).setCellValue(values.get(c));
    }
  }

  private static String quote(String value) {
    return value.contains(",") || value.contains("\"")
        ? "\"" + value.replace("\"", "\"\"") + "\""
        : value;
  }
}
