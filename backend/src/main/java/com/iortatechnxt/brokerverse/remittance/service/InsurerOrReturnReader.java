package com.iortatechnxt.brokerverse.remittance.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Reads the extract an insurer returns with its official receipts (FRS BRD-02 FRID.RMT.008.01, ICD
 * IF-RMT-03): the Excel extract of IF-RMT-01 with the columns Date of Official Receipt, Official
 * Receipt Number and OR Amount filled, or the text extract of IF-RMT-02 with fields 23 to 25
 * filled. The batch is read from the column Batch Number (Excel) or from the file name (text); the
 * text file must agree with its header control totals. Accounts without an OR number are not
 * returned yet and are skipped.
 */
@SuppressWarnings("PMD.GodClass") // the two returned layouts of IF-RMT-03 in one reader
final class InsurerOrReturnReader {

  private static final String INVOICE = "Invoice Number";
  private static final String BATCH = "Batch Number";
  private static final String OR_DATE = "Date of Official Receipt";
  private static final String OR_NO = "Official Receipt Number";
  private static final String OR_AMOUNT = "OR Amount";
  private static final int MAX_HEADER_ROW = 30;
  private static final int SNIFF_BYTES = 8;
  private static final int SETTLEMENT_DIGITS = 8;
  private static final Pattern DELIMITER = Pattern.compile("|", Pattern.LITERAL);
  private static final DateTimeFormatter US_DATE =
      DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH);

  /** Fields of the D record (1-based in the ICD). */
  private static final int D_INVOICE = 1;

  private static final int D_PAID_AR = 10;
  private static final int D_NET_DUE = 15;
  private static final int D_OR_DATE = 22;
  private static final int D_OR_NO = 23;
  private static final int D_OR_AMOUNT = 24;
  private static final int H_COUNT = 4;
  private static final int H_PAID_AR = 5;
  private static final int H_NET_DUE = 10;

  private InsurerOrReturnReader() {}

  /**
   * Whether a file is a returned extract and not the BIBS upload template: an Excel 97-2003 file, a
   * text file starting with an H record, or a workbook whose header row has the column Batch Number
   * of the extract.
   *
   * @param fileName file name
   * @param content file
   * @return true for a returned extract
   */
  static boolean isReturnedExtract(String fileName, byte[] content) {
    String name = fileName == null ? "" : fileName.strip();
    if (hasExtension(name, ".xls")) {
      return true;
    }
    if (hasExtension(name, ".txt")) {
      return new String(content, 0, Math.min(content.length, SNIFF_BYTES), StandardCharsets.UTF_8)
          .replace("\uFEFF", "")
          .startsWith("H|");
    }
    return hasExtension(name, ".xlsx") && excel(content).refusal() == null;
  }

  private static boolean hasExtension(String name, String extension) {
    return name.length() >= extension.length()
        && name.regionMatches(
            true, name.length() - extension.length(), extension, 0, extension.length());
  }

  /**
   * Reads the OR lines of a returned extract.
   *
   * @param fileName file name
   * @param content file
   * @return lines, or the reason the whole file is refused
   */
  static Result read(String fileName, byte[] content) {
    return hasExtension(fileName.strip(), ".txt") ? text(fileName, content) : excel(content);
  }

  private static Result excel(byte[] content) {
    try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(content))) {
      Sheet sheet = wb.getSheetAt(0);
      DataFormatter formatter = new DataFormatter(Locale.US);
      int headerRow = headerRow(sheet, formatter);
      if (headerRow < 0) {
        return Result.refused("The file has no header row with " + INVOICE);
      }
      Map<String, Integer> columns = new HashMap<>();
      for (Cell c : sheet.getRow(headerRow)) {
        columns.put(formatter.formatCellValue(c).strip(), c.getColumnIndex());
      }
      for (String needed : List.of(BATCH, OR_DATE, OR_NO, OR_AMOUNT)) {
        if (!columns.containsKey(needed)) {
          return Result.refused("The file has no column " + needed);
        }
      }
      return Result.of(accounts(sheet, headerRow, columns, formatter));
    } catch (IOException | RuntimeException ex) {
      return Result.refused("The file cannot be read as an Excel extract");
    }
  }

  private static int headerRow(Sheet sheet, DataFormatter formatter) {
    for (int r = 0; r <= Math.min(MAX_HEADER_ROW, sheet.getLastRowNum()); r++) {
      Row row = sheet.getRow(r);
      if (row != null && INVOICE.equals(cellText(row, 0, formatter))) {
        return r;
      }
    }
    return -1;
  }

  private static List<Line> accounts(
      Sheet sheet, int headerRow, Map<String, Integer> columns, DataFormatter formatter) {
    List<Line> lines = new ArrayList<>();
    for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      String invoice = row == null ? "" : cellText(row, 0, formatter);
      if (invoice.isEmpty()) {
        break;
      }
      String orNo = cellText(row, columns.get(OR_NO), formatter);
      if (!orNo.isEmpty()) {
        lines.add(
            new Line(
                r + 1,
                cellText(row, columns.get(BATCH), formatter),
                invoice,
                orNo,
                cellDate(row.getCell(columns.get(OR_DATE)), formatter),
                cellText(row, columns.get(OR_AMOUNT), formatter)));
      }
    }
    return lines;
  }

  private static String cellText(Row row, int column, DataFormatter formatter) {
    Cell cell = row.getCell(column);
    return cell == null ? "" : formatter.formatCellValue(cell).strip();
  }

  private static String cellDate(Cell cell, DataFormatter formatter) {
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
        && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().format(US_DATE);
    }
    return formatter.formatCellValue(cell).strip();
  }

  private static Result text(String fileName, byte[] content) {
    List<String[]> records = records(content);
    if (records.isEmpty() || !"H".equals(records.get(0)[0])) {
      return Result.refused("The text file does not start with an H record");
    }
    List<String[]> details = records.subList(1, records.size());
    String reason = controlTotals(records.get(0), details);
    return reason == null
        ? Result.of(textLines(baseName(fileName), details))
        : Result.refused(reason);
  }

  private static List<String[]> records(byte[] content) {
    String body = new String(content, StandardCharsets.UTF_8).replace("\uFEFF", "");
    List<String[]> records = new ArrayList<>();
    for (String line : body.split("\\R")) {
      if (!line.isBlank()) {
        records.add(DELIMITER.split(line, -1));
      }
    }
    return records;
  }

  private static List<Line> textLines(String settlementRef, List<String[]> details) {
    List<Line> lines = new ArrayList<>();
    for (int i = 0; i < details.size(); i++) {
      String[] d = details.get(i);
      if (d.length > D_OR_AMOUNT && !d[D_OR_NO].isBlank()) {
        lines.add(
            new Line(
                i + 2,
                settlementRef,
                d[D_INVOICE].strip(),
                d[D_OR_NO].strip(),
                d[D_OR_DATE].strip(),
                d[D_OR_AMOUNT].strip()));
      }
    }
    return lines;
  }

  private static String controlTotals(String[] h, List<String[]> details) {
    try {
      if (h.length <= H_NET_DUE || Integer.parseInt(h[H_COUNT].strip()) != details.size()) {
        return "The number of D records does not agree with the header";
      }
      if (details.stream().anyMatch(d -> d.length <= D_NET_DUE || !"D".equals(d[0]))) {
        return "A record after the header is not a D record";
      }
      BigDecimal paid = sum(details, D_PAID_AR);
      BigDecimal net = sum(details, D_NET_DUE);
      boolean agree =
          paid.compareTo(new BigDecimal(h[H_PAID_AR].strip())) == 0
              && net.compareTo(new BigDecimal(h[H_NET_DUE].strip())) == 0;
      return agree ? null : "The control totals of the header do not agree with the D records";
    } catch (NumberFormatException ex) {
      return "The header or a D record holds a value that is not a number";
    }
  }

  private static BigDecimal sum(List<String[]> details, int field) {
    return details.stream()
        .map(d -> new BigDecimal(d[field].strip()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Name of a text extract without its extension: prefix + YYMMDD + insurer code + 8-digit
   * settlement number.
   *
   * @param fileName file name
   * @return base name
   */
  static String baseName(String fileName) {
    String base = fileName.strip();
    int dot = base.lastIndexOf('.');
    return dot < 0 ? base : base.substring(0, dot);
  }

  /**
   * The settlement number at the end of the base name of a text extract.
   *
   * @param baseName base name
   * @return last 8 characters, or the name when shorter
   */
  static String settlementNoOf(String baseName) {
    return baseName.length() <= SETTLEMENT_DIGITS
        ? baseName
        : baseName.substring(baseName.length() - SETTLEMENT_DIGITS);
  }

  /**
   * An amount as the insurer typed it: thousands separators and brackets for negatives allowed.
   *
   * @param text amount
   * @return amount with 2 decimals
   * @throws NumberFormatException when not a number
   */
  static BigDecimal amount(String text) {
    String t = text.strip().replace(",", "");
    boolean negative = t.startsWith("(") && t.endsWith(")");
    BigDecimal value = new BigDecimal(negative ? t.substring(1, t.length() - 1) : t);
    return (negative ? value.negate() : value).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * A date as M/D/YYYY (the extract) or ISO.
   *
   * @param text date
   * @return date
   * @throws DateTimeParseException when not a date
   */
  static LocalDate date(String text) {
    String t = text.strip();
    return t.contains("/") ? LocalDate.parse(t, US_DATE) : LocalDate.parse(t);
  }

  /**
   * An OR line of a returned extract.
   *
   * @param lineNo row or line of the file
   * @param batchRef insurer code and settlement number, or the base name of a text file
   * @param invoiceNo invoice
   * @param orNo OR number
   * @param orDate OR date as typed
   * @param orAmount OR amount as typed
   */
  record Line(
      int lineNo, String batchRef, String invoiceNo, String orNo, String orDate, String orAmount) {}

  /**
   * The lines of a file, or why it is refused.
   *
   * @param lines lines
   * @param refusal reason, null when read
   */
  record Result(List<Line> lines, String refusal) {

    /** Defensive copy. */
    Result {
      lines = List.copyOf(lines);
    }

    static Result of(List<Line> lines) {
      return new Result(lines, null);
    }

    static Result refused(String reason) {
      return new Result(List.of(), reason);
    }
  }
}
