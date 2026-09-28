package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the delimited files uploaded to the remittance feeds (comma, semicolon or tab separated,
 * first line = column names, case-insensitive; optional double quotes). The real layouts of the
 * insurer and Collection files are parked (OQ22, OQ45); the column names are those of the BRD.
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

  private CsvRows() {}

  /**
   * Parses a file.
   *
   * @param content bytes (UTF-8)
   * @param required column names that must be present
   * @return rows by lower-case column name, with their line number
   */
  public static List<Row> parse(byte[] content, Set<String> required) {
    String text = new String(content, StandardCharsets.UTF_8).replace("﻿", "");
    List<String> lines = text.lines().filter(l -> !l.isBlank()).toList();
    if (lines.isEmpty()) {
      throw new BusinessRuleException("FEED_FILE_EMPTY", "The file has no lines");
    }
    char separator = separatorOf(lines.get(0));
    List<String> header = split(lines.get(0), separator).stream().map(CsvRows::key).toList();
    for (String column : required) {
      if (!header.contains(key(column))) {
        throw new BusinessRuleException(
            "FEED_FILE_COLUMNS",
            "The file must have the columns "
                + String.join(", ", required.stream().map(CsvRows::label).sorted().toList()));
      }
    }
    List<Row> rows = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      List<String> cells = split(lines.get(i), separator);
      Map<String, String> values = new LinkedHashMap<>();
      for (int c = 0; c < header.size(); c++) {
        values.put(header.get(c), c < cells.size() ? cells.get(c) : "");
      }
      rows.add(new Row(i + 1, lines.get(i), values));
    }
    return rows;
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
