package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads a payment file in BDOI's layout with the profile of its type (FRS.CSH.05.01.06; Appendix
 * D): the text file with or without a line of column names, the header record of the Bills Payment
 * file (record count, total amount and date) and its detail records, the Excel sheet of CLPC with
 * its TOTAL row; maps BDOI's fields to the fields of the system and writes the dates as ISO dates
 * and the amounts without thousands separators.
 */
// A parser of the client's file layouts: many small static readers of one format, not a model
// class.
@SuppressWarnings("PMD.GodClass")
public final class ChannelFileReader {

  private static final Pattern WIDE_SPACE = Pattern.compile("\\s{2,}|\\t");
  private static final String COUNT = "count";
  private static final String TOTAL = "total";
  private static final List<String> AMOUNT_FIELDS =
      List.of("Amount", "Credit", "Paid amount", "Basic commission");

  private ChannelFileReader() {}

  /**
   * Reads a file.
   *
   * @param profile profile of the file type
   * @param fileName name (the extension tells Excel from text for the type that takes both)
   * @param content bytes
   * @param dateFields fields of the system that hold dates
   * @return rows mapped to the fields of the system, with the control figures
   */
  public static Content read(
      ChannelProfile profile, String fileName, byte[] content, Set<String> dateFields) {
    List<List<String>> lines = excel(profile, fileName) ? sheet(content) : text(profile, content);
    List<String> columns = profile.isHeaderLine() && !lines.isEmpty() ? lines.get(0) : List.of();
    List<List<String>> body =
        profile.isHeaderLine() && !lines.isEmpty() ? lines.subList(1, lines.size()) : lines;
    Controls controls = new Controls();
    List<Map<String, String>> rows = new ArrayList<>();
    for (List<String> line : body) {
      if (isDetail(profile, line, controls)) {
        rows.add(map(profile, columns, line, dateFields));
      }
    }
    BigDecimal details = BigDecimal.ZERO;
    String amountField = amountField(profile);
    for (Map<String, String> row : rows) {
      details = details.add(number(row.get(amountField)));
    }
    return new Content(
        new ArrayList<>(profile.fields().keySet()), rows, controls.count, controls.total, details);
  }

  /**
   * Sorts a line: a header record or TOTAL row feeds the controls, a detail record is kept.
   *
   * @return true for a detail record
   */
  private static boolean isDetail(ChannelProfile profile, List<String> line, Controls controls) {
    boolean detail = false;
    if (line.stream().anyMatch(c -> !c.isBlank())) {
      if (isRecord(line, profile.getHeaderRecord())) {
        controls.header(line, profile);
      } else if (isTotalRow(line, profile.getTotalRowLabel())) {
        controls.totalRow(line);
      } else {
        detail = profile.getDetailRecord() == null || isRecord(line, profile.getDetailRecord());
      }
    }
    return detail;
  }

  private static boolean excel(ChannelProfile profile, String fileName) {
    return "EXCEL".equals(profile.getFileFormat())
        || "ANY".equals(profile.getFileFormat()) && asciiLower(fileName).endsWith(".xlsx");
  }

  private static List<List<String>> sheet(byte[] content) {
    List<List<String>> lines = new ArrayList<>();
    DataFormatter formatter = new DataFormatter(Locale.ROOT);
    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      Sheet sheet = book.getSheetAt(0);
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
          Cell cell = row.getCell(c);
          cells.add(cell == null ? "" : formatter.formatCellValue(cell).strip());
        }
        lines.add(cells);
      }
    } catch (IOException | RuntimeException ex) {
      throw new BusinessRuleException(
          "CHANNEL_FILE_UNREADABLE", "The file cannot be read as an Excel workbook", ex);
    }
    return lines;
  }

  private static List<List<String>> text(ChannelProfile profile, byte[] content) {
    String text = new String(content, StandardCharsets.UTF_8).replace("﻿", "");
    List<List<String>> lines = new ArrayList<>();
    String first = text.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
    String delimiter = delimiter(profile.getDelimiter(), first);
    for (String line : text.lines().toList()) {
      String[] parts =
          "SPACES".equals(delimiter)
              ? WIDE_SPACE.split(line.strip())
              : line.split(Pattern.quote(delimiter), -1);
      lines.add(Arrays.stream(parts).map(String::strip).toList());
    }
    return lines;
  }

  /**
   * The separator of a text file: the profile's, or for AUTO the first of pipe, tab, comma and
   * semicolon found in the first line, else columns aligned with spaces (the Trade sample).
   *
   * @param configured separator of the profile
   * @param firstLine first line of the file
   * @return separator, or SPACES
   */
  static String delimiter(String configured, String firstLine) {
    if ("TAB".equals(configured)) {
      return "\t";
    }
    if (!"AUTO".equals(configured)) {
      return configured;
    }
    for (String candidate : List.of("|", "\t", ",", ";")) {
      if (firstLine.contains(candidate)) {
        return candidate;
      }
    }
    return "SPACES";
  }

  private static boolean isRecord(List<String> line, String type) {
    return type != null && !line.isEmpty() && type.equals(line.get(0));
  }

  private static boolean isTotalRow(List<String> line, String label) {
    return label != null
        && line.stream()
            .filter(c -> !c.isBlank())
            .findFirst()
            .map(c -> asciiLower(c).equals(asciiLower(label)))
            .orElse(false);
  }

  private static Map<String, String> map(
      ChannelProfile profile, List<String> columns, List<String> line, Set<String> dateFields) {
    Map<String, String> row = new LinkedHashMap<>();
    profile
        .fields()
        .forEach(
            (field, source) -> {
              String value = cell(columns, line, source);
              row.put(
                  field,
                  dateFields.contains(field) ? isoDate(value, profile.getDateFormat()) : value);
            });
    return row;
  }

  private static String cell(List<String> columns, List<String> line, String source) {
    int index = position(source);
    if (index < 0) {
      for (int i = 0; i < columns.size(); i++) {
        if (asciiLower(columns.get(i)).equals(asciiLower(source))) {
          index = i;
        }
      }
    }
    return index >= 0 && index < line.size() ? line.get(index) : "";
  }

  /**
   * A text with its ASCII capitals in lower case (column names and file extensions of the files).
   *
   * @param text text
   * @return text in lower case
   */
  static String asciiLower(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      out.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
    }
    return out.toString();
  }

  private static int position(String source) {
    return source.chars().allMatch(Character::isDigit) ? Integer.parseInt(source) - 1 : -1;
  }

  /**
   * A date of BDOI's file as an ISO date; a value that is not a date of the format is kept as it
   * is, for the row check to report.
   *
   * @param value value
   * @param format date format of the profile
   * @return ISO date or the value
   */
  static String isoDate(String value, String format) {
    if (value == null || value.isBlank()) {
      return "";
    }
    try {
      return LocalDate.parse(value.strip(), DateTimeFormatter.ofPattern(format, Locale.ROOT))
          .toString();
    } catch (DateTimeParseException ex) {
      return value;
    }
  }

  /**
   * An amount of BDOI's file (leading zeros, thousands separators).
   *
   * @param value value
   * @return amount, zero when blank or not a number
   */
  static BigDecimal number(String value) {
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value.replace(",", "").strip());
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  private static String amountField(ChannelProfile profile) {
    return AMOUNT_FIELDS.stream()
        .filter(profile.fields()::containsKey)
        .findFirst()
        .orElse("Amount");
  }

  /** The control figures met while reading. */
  private static final class Controls {
    private Integer count;
    private BigDecimal total;

    void header(List<String> line, ChannelProfile profile) {
      Map<String, String> controls = profile.controls();
      if (controls.containsKey(COUNT)) {
        count = number(cell(List.of(), line, controls.get(COUNT))).intValue();
      }
      if (controls.containsKey(TOTAL)) {
        total = number(cell(List.of(), line, controls.get(TOTAL)));
      }
    }

    void totalRow(List<String> line) {
      total =
          line.stream()
              .map(ChannelFileReader::number)
              .filter(n -> n.signum() != 0)
              .reduce((a, b) -> b)
              .orElse(BigDecimal.ZERO);
    }
  }

  /**
   * A file read.
   *
   * @param fields fields of the system, in order
   * @param rows detail rows
   * @param headerCount records the header announces, null without a header
   * @param controlTotal total the header or the TOTAL row announces, null without one
   * @param detailTotal total of the detail rows
   */
  public record Content(
      List<String> fields,
      List<Map<String, String>> rows,
      Integer headerCount,
      BigDecimal controlTotal,
      BigDecimal detailTotal) {

    /** Defensive copies. */
    public Content {
      fields = List.copyOf(fields);
      rows = List.copyOf(rows);
    }

    /**
     * The reason the controls fail, null when they hold (FRS.CSH.05.01.06).
     *
     * @return reason or null
     */
    public String controlFailure() {
      if (headerCount != null && headerCount != rows.size()) {
        return "The header gives "
            + headerCount
            + " records but the file holds "
            + rows.size()
            + " detail records";
      }
      if (controlTotal != null && controlTotal.compareTo(detailTotal) != 0) {
        return "The control total "
            + RecordValidation.formatted(controlTotal)
            + " differs from the total of the payments "
            + RecordValidation.formatted(detailTotal);
      }
      return null;
    }
  }
}
