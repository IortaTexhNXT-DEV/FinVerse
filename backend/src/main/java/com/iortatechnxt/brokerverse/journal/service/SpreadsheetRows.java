package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads tabular upload files (CSV per RFC 4180, or the first sheet of an XLSX workbook) into rows
 * of text cells. Dates in XLSX cells are returned as ISO dates, numbers in plain notation.
 */
public final class SpreadsheetRows {

  private static final char QUOTE = '"';
  private static final char COMMA = ',';
  private static final char BOM = '\uFEFF';

  private SpreadsheetRows() {}

  /**
   * Parses a file by its extension.
   *
   * @param fileName file name (.csv or .xlsx)
   * @param content bytes
   * @param maxRows largest accepted number of rows (header included)
   * @return rows of cells
   */
  public static List<List<String>> read(String fileName, byte[] content, int maxRows) {
    String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    List<List<String>> rows;
    if (name.endsWith(".csv")) {
      rows = parseCsv(new String(content, StandardCharsets.UTF_8));
    } else if (name.endsWith(".xlsx")) {
      rows = parseXlsx(content);
    } else {
      throw new BusinessRuleException("UNSUPPORTED_FILE", "Upload a .csv or .xlsx file");
    }
    if (rows.size() > maxRows) {
      throw new BusinessRuleException(
          "TOO_MANY_ROWS", "The file has more than " + (maxRows - 1) + " data rows");
    }
    return rows;
  }

  /**
   * Parses CSV text. Quoted fields may contain commas, quotes ("") and line breaks; blank lines are
   * skipped.
   *
   * @param text CSV text
   * @return rows of cells
   */
  public static List<List<String>> parseCsv(String text) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    int i = !text.isEmpty() && text.charAt(0) == BOM ? 1 : 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == QUOTE) {
        i = readQuoted(text, i + 1, cell);
      } else if (c == COMMA) {
        row.add(take(cell));
        i++;
      } else if (c == '\n' || c == '\r') {
        endRow(rows, row, cell);
        i = afterLineBreak(text, i);
      } else {
        cell.append(c);
        i++;
      }
    }
    endRow(rows, row, cell);
    return rows;
  }

  /** Appends a quoted section (without its quotes) and returns the index after it. */
  private static int readQuoted(String text, int from, StringBuilder cell) {
    int i = from;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c != QUOTE) {
        cell.append(c);
        i++;
      } else if (i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
        cell.append(QUOTE);
        i += 2;
      } else {
        return i + 1;
      }
    }
    return i;
  }

  private static int afterLineBreak(String text, int i) {
    boolean crlf = text.charAt(i) == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n';
    return crlf ? i + 2 : i + 1;
  }

  private static String take(StringBuilder cell) {
    String value = cell.toString().strip();
    cell.setLength(0);
    return value;
  }

  private static void endRow(List<List<String>> rows, List<String> row, StringBuilder cell) {
    row.add(take(cell));
    if (row.stream().anyMatch(v -> !v.isEmpty())) {
      rows.add(List.copyOf(row));
    }
    row.clear();
  }

  /**
   * Parses the first sheet of an XLSX workbook.
   *
   * @param content workbook bytes
   * @return rows of cells (blank rows skipped)
   */
  public static List<List<String>> parseXlsx(byte[] content) {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
          cells.add(text(row.getCell(c)));
        }
        if (cells.stream().anyMatch(s -> !s.isBlank())) {
          rows.add(cells);
        }
      }
      return rows;
    } catch (IOException | RuntimeException e) {
      BusinessRuleException unreadable =
          new BusinessRuleException("UNREADABLE_FILE", "The file is not a readable XLSX workbook");
      unreadable.initCause(e);
      throw unreadable;
    }
  }

  private static String text(Cell cell) {
    if (cell == null) {
      return "";
    }
    CellType type =
        cell.getCellType() == CellType.FORMULA
            ? cell.getCachedFormulaResultType()
            : cell.getCellType();
    return switch (type) {
      case STRING -> cell.getStringCellValue().strip();
      case NUMERIC ->
          DateUtil.isCellDateFormatted(cell)
              ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
              : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> "";
    };
  }
}
