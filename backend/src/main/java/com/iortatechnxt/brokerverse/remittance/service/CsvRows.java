package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.excel.GuidedTables;
import com.iortatechnxt.brokerverse.common.excel.WorkbookTables;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the files uploaded to the remittance feeds: the guided Excel template (header row below its
 * column guide, example rows skipped), or a delimited file (comma, semicolon or tab separated,
 * first line = column names, optional double quotes). Column names are matched whatever their
 * spelling ("Invoice No.", "invoiceNo"; see {@link #key}). The real layouts of the insurer and
 * Collection files are parked (OQ22, OQ45); the column names are those of the BRD.
 */
public final class CsvRows {

  /** The headers of the upload templates (Holds, Special Remittance, Insurer OR). */
  private static final Map<String, String> LABELS =
      Map.of(
          "invoiceno", "Invoice No.",
          "reasoncode", "Reason Code",
          "holduntil", "Hold Until",
          "remarks", "Remarks",
          "conditioncode", "Condition Code",
          "batchno", "Batch No.",
          "orno", "OR No.",
          "ordate", "OR Date",
          "oramount", "OR Amount");

  private static final char QUOTE = '"';
  private static final byte[] ZIP = {'P', 'K', 3, 4};

  private CsvRows() {}

  /**
   * Parses a file.
   *
   * @param content bytes (UTF-8 text, or an Excel workbook)
   * @param required column names that must be present
   * @return rows by lower-case column name, with their line number
   */
  public static List<Row> parse(byte[] content, Set<String> required) {
    boolean workbook = isWorkbook(content);
    List<String> lines =
        workbook
            ? List.of()
            : new String(content, StandardCharsets.UTF_8).replace("\uFEFF", "").lines().toList();
    List<List<String>> table = workbook ? workbook(content, required) : text(lines);
    if (table.stream().allMatch(CsvRows::blank)) {
      throw new BusinessRuleException("FEED_FILE_EMPTY", "The file has no lines");
    }
    int headerRow = GuidedTables.headerRow(table, required, CsvRows::key);
    List<String> header = header(table.get(headerRow), required);
    List<Row> rows = new ArrayList<>();
    for (int i = headerRow + 1; i < table.size(); i++) {
      List<String> cells = table.get(i);
      if (!blank(cells) && !GuidedTables.isExample(cells)) {
        rows.add(
            new Row(i + 1, workbook ? raw(header, cells) : lines.get(i), values(header, cells)));
      }
    }
    return rows;
  }

  /** The column keys of the file's header; refuses a file without the required columns. */
  private static List<String> header(List<String> row, Set<String> required) {
    List<String> header = GuidedTables.headers(row).stream().map(CsvRows::key).toList();
    for (String column : required) {
      if (!header.contains(key(column))) {
        throw new BusinessRuleException(
            "FEED_FILE_COLUMNS",
            "The file must have the columns "
                + String.join(", ", required.stream().map(CsvRows::label).sorted().toList()));
      }
    }
    return header;
  }

  /**
   * The column a header names, whatever its spelling: "Invoice No.", "invoice no" and "invoiceNo"
   * are the same column; a note in brackets ("Hold Until (dd-MMM-yyyy)") is ignored.
   *
   * @param header header text or column name
   * @return lower-case letters and digits
   */
  static String key(String header) {
    return header
        .replaceAll("\\([^)]*\\)", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]", "");
  }

  /**
   * The header users read for a column of the upload templates.
   *
   * @param column column name
   * @return business header, the column name when unknown
   */
  public static String label(String column) {
    return LABELS.getOrDefault(key(column), column);
  }

  private static Map<String, String> values(List<String> header, List<String> cells) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int c = 0; c < header.size(); c++) {
      if (!header.get(c).isEmpty()) {
        values.put(header.get(c), c < cells.size() ? cells.get(c).strip() : "");
      }
    }
    return values;
  }

  private static boolean isWorkbook(byte[] content) {
    return content.length > ZIP.length && Arrays.equals(Arrays.copyOf(content, ZIP.length), ZIP);
  }

  private static List<List<String>> workbook(byte[] content, Set<String> required) {
    try {
      return WorkbookTables.read(content, required);
    } catch (IOException | RuntimeException e) {
      throw new BusinessRuleException(
          "FEED_FILE_UNREADABLE", "The file is not a readable Excel workbook", e);
    }
  }

  /** The cells of the lines of a delimited text; the separator is the first line's. */
  private static List<List<String>> text(List<String> lines) {
    char separator = separatorOf(lines.stream().filter(l -> !l.isBlank()).findFirst().orElse(""));
    return lines.stream().map(l -> split(l, separator)).toList();
  }

  /** The raw line of a row: its values comma separated (payload of the flow-in record). */
  private static String raw(List<String> header, List<String> cells) {
    int from = header.isEmpty() || !header.get(0).isEmpty() ? 0 : 1;
    return String.join(",", cells.subList(Math.min(from, cells.size()), cells.size()));
  }

  private static boolean blank(List<String> cells) {
    return cells.stream().allMatch(String::isBlank);
  }

  private static char separatorOf(String header) {
    if (header.indexOf('\t') >= 0) {
      return '\t';
    }
    return header.indexOf(';') >= 0 ? ';' : ',';
  }

  private static List<String> split(String line, char separator) {
    List<String> cells = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false;
    for (char ch : line.toCharArray()) {
      if (ch == QUOTE) {
        quoted = !quoted;
      } else if (ch == separator && !quoted) {
        cells.add(cell.toString().strip());
        cell.setLength(0);
      } else {
        cell.append(ch);
      }
    }
    cells.add(cell.toString().strip());
    return cells;
  }

  /**
   * One data line.
   *
   * @param lineNo line number in the file
   * @param raw raw line (payload of the flow-in record)
   * @param values values by column key (see {@link CsvRows#key})
   */
  public record Row(int lineNo, String raw, Map<String, String> values) {

    /** Defensive copy. */
    public Row {
      values = Map.copyOf(values);
    }

    /**
     * A value.
     *
     * @param column column name
     * @return value, empty when missing
     */
    public String get(String column) {
      return values.getOrDefault(key(column), "");
    }

    /**
     * A mandatory value.
     *
     * @param column column name
     * @return value
     */
    public String require(String column) {
      String value = get(column);
      if (value.isBlank()) {
        throw new BusinessRuleException(
            "FEED_VALUE_MISSING", "Line " + lineNo + ": " + label(column) + " is missing");
      }
      return value;
    }
  }
}
