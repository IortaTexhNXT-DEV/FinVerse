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

/**
 * Downloadable journal upload templates (CSV and XLSX) with two example vouchers in the head-office
 * branch and the base currency of the company.
 */
public final class JournalUploadTemplate {

  private JournalUploadTemplate() {}

  /**
   * Values of the example vouchers.
   *
   * @param valueDate value date
   * @param branchCode branch (the head office of the company), may be blank
   * @param currency currency (the base currency of the company), may be blank
   */
  public record Example(LocalDate valueDate, String branchCode, String currency) {

    /** Blank values instead of null. */
    public Example {
      branchCode = branchCode == null ? "" : branchCode;
      currency = currency == null ? "" : currency;
    }
  }

  /**
   * Example rows in template column order.
   *
   * @param example values of the examples
   * @return rows (without header)
   */
  static List<List<String>> sampleRows(Example example) {
    String date = example.valueDate().toString();
    return List.of(
        List.of(
            "V1",
            example.branchCode(),
            "MANUAL",
            date,
            example.currency(),
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
            example.branchCode(),
            "ACCRUAL",
            date,
            example.currency(),
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
   * @param example values of the examples
   * @return UTF-8 CSV bytes
   */
  public static byte[] csv(Example example) {
    StringBuilder text = new StringBuilder(String.join(",", UploadLine.ALL_COLUMNS)).append("\r\n");
    for (List<String> row : sampleRows(example)) {
      text.append(row.stream().map(JournalUploadTemplate::quote).collect(Collectors.joining(",")))
          .append("\r\n");
    }
    return text.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * XLSX template.
   *
   * @param example values of the examples
   * @return workbook bytes
   */
  public static byte[] xlsx(Example example) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Journals");
      write(sheet.createRow(0), UploadLine.ALL_COLUMNS);
      List<List<String>> rows = sampleRows(example);
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
