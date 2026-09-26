package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout.Column;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes the AMLC extraction file from the STR layout (SNSRP-105, 706; FR-SS-017, 071): one row per
 * STR with the layout's columns in order, each an STR field or a fixed value, with the BIBS codes
 * mapped to AMLC codes. CSV (header row, delimiter, quoting) and fixed-width files are written;
 * XLSX and XML layouts wait for the AMLC format (SQ09). Pure function.
 */
public final class StrFileWriter {

  private static final String QUOTE = "\"";
  private static final String LEFT = "LEFT";
  private static final String LINE = "\r\n";

  private StrFileWriter() {}

  /**
   * Writes the file.
   *
   * @param layout the STR layout
   * @param rows one map of field code to value per STR
   * @return the file bytes in the layout's encoding
   */
  public static byte[] write(StrLayout layout, List<Map<String, String>> rows) {
    Charset charset = charset(layout.encoding());
    if (layout.format() == StrFormat.CSV) {
      return csv(layout, rows).getBytes(charset);
    }
    if (layout.format() == StrFormat.FIXED) {
      return fixed(layout, rows).getBytes(charset);
    }
    throw new BusinessRuleException(
        "SCR_STR_FORMAT_PARKED",
        "STR layout format "
            + layout.format()
            + " waits for the AMLC format (SQ09); use CSV or FIXED");
  }

  /**
   * The file extension of a layout.
   *
   * @param layout the layout
   * @return csv or txt
   */
  public static String extension(StrLayout layout) {
    return layout.format() == StrFormat.CSV ? "csv" : "txt";
  }

  private static String csv(StrLayout layout, List<Map<String, String>> rows) {
    String delimiter =
        layout.delimiter() == null || layout.delimiter().isEmpty() ? "," : layout.delimiter();
    StringBuilder out = new StringBuilder();
    out.append(
            layout.columns().stream()
                .map(c -> quote(c.header() == null ? c.fieldCode() : c.header(), delimiter))
                .collect(Collectors.joining(delimiter)))
        .append(LINE);
    for (Map<String, String> row : rows) {
      out.append(
              layout.columns().stream()
                  .map(c -> quote(value(c, row), delimiter))
                  .collect(Collectors.joining(delimiter)))
          .append(LINE);
    }
    return out.toString();
  }

  private static String fixed(StrLayout layout, List<Map<String, String>> rows) {
    StringBuilder out = new StringBuilder();
    for (Map<String, String> row : rows) {
      for (Column c : layout.columns()) {
        out.append(pad(value(c, row), c));
      }
      out.append(LINE);
    }
    return out.toString();
  }

  /**
   * The value of a column for an STR: the fixed value, or the field mapped by the code map.
   *
   * @param column the column
   * @param row the STR values
   * @return the value, empty when absent
   */
  static String value(Column column, Map<String, String> row) {
    if (column.fixedValue() != null) {
      return column.fixedValue();
    }
    String value = row.getOrDefault(column.fieldCode(), "");
    return column.codeMap().getOrDefault(value, value == null ? "" : value);
  }

  private static String pad(String value, Column column) {
    if (column.length() == null) {
      return value;
    }
    int width = column.length();
    if (value.length() >= width) {
      return value.substring(0, width);
    }
    String blanks = " ".repeat(width - value.length());
    return LEFT.equals(column.pad()) ? blanks + value : value + blanks;
  }

  private static String quote(String value, String delimiter) {
    String text = value == null ? "" : value;
    if (text.contains(delimiter) || text.contains(QUOTE) || text.contains("\n")) {
      return QUOTE + text.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
    }
    return text;
  }

  private static Charset charset(String encoding) {
    try {
      return encoding == null || encoding.isBlank()
          ? StandardCharsets.UTF_8
          : Charset.forName(encoding);
    } catch (IllegalArgumentException ex) {
      return StandardCharsets.UTF_8;
    }
  }
}
