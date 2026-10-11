package com.iortatechnxt.brokerverse.budget.service;

import com.iortatechnxt.brokerverse.budget.domain.BudgetLine;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the budget import CSV (format in {@code docs/samples/budget_import_sample.csv}).
 *
 * <p>Header row required (lower case names). Two layouts are accepted:
 *
 * <ul>
 *   <li>{@code account_code,cost_centre,m01,...,m12} – twelve monthly amounts;
 *   <li>{@code account_code,cost_centre,annual} – annual amount spread evenly.
 * </ul>
 *
 * Blank lines and lines starting with {@code #} are ignored. Values must not contain commas.
 */
public final class BudgetCsvParser {

  private static final String SEPARATOR = ",";
  private static final int KEY_COLUMNS = 2;
  private static final int ANNUAL_COLUMNS = KEY_COLUMNS + 1;
  private static final int MONTHLY_COLUMNS = KEY_COLUMNS + BudgetLine.MONTHS;
  private static final int MAX_ERRORS = 20;

  private BudgetCsvParser() {}

  /**
   * Parses CSV text.
   *
   * @param csv file content
   * @return parsed lines
   */
  public static List<ParsedLine> parse(String csv) {
    List<String> rows = csv.lines().map(String::trim).toList();
    int headerIndex = firstDataRow(rows, 0);
    if (headerIndex < 0) {
      throw invalid(List.of("The file is empty"));
    }
    int columns = headerColumns(rows.get(headerIndex));
    List<ParsedLine> lines = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (int i = headerIndex + 1; i < rows.size() && errors.size() < MAX_ERRORS; i++) {
      String row = rows.get(i);
      if (isData(row)) {
        parseRow(row, i + 1, columns, lines, errors);
      }
    }
    if (!errors.isEmpty()) {
      throw invalid(errors);
    }
    return lines;
  }

  private static int headerColumns(String headerRow) {
    String[] header = headerRow.split(SEPARATOR, -1);
    int columns = header.length;
    if (!"account_code".equals(header[0].trim())
        || columns != ANNUAL_COLUMNS && columns != MONTHLY_COLUMNS) {
      throw invalid(
          List.of(
              "Header must be account_code,cost_centre,annual"
                  + " or account_code,cost_centre followed by twelve month columns"));
    }
    return columns;
  }

  private static void parseRow(
      String row, int lineNo, int columns, List<ParsedLine> lines, List<String> errors) {
    String[] cells = row.split(SEPARATOR, -1);
    String prefix = "Line " + lineNo + ": ";
    if (cells.length != columns) {
      errors.add(prefix + "expected " + columns + " values but found " + cells.length);
    } else if (cells[0].isBlank()) {
      errors.add(prefix + "account code is missing");
    } else {
      try {
        lines.add(line(cells, columns));
      } catch (NumberFormatException ex) {
        errors.add(prefix + "amounts must be numbers");
      }
    }
  }

  private static ParsedLine line(String[] cells, int columns) {
    List<BigDecimal> amounts = new ArrayList<>();
    for (int c = KEY_COLUMNS; c < columns; c++) {
      amounts.add(amount(cells[c]));
    }
    List<BigDecimal> months =
        columns == ANNUAL_COLUMNS ? BudgetSpread.even(amounts.get(0)) : amounts;
    String costCenter = cells[1].trim();
    return new ParsedLine(cells[0].trim(), costCenter.isEmpty() ? null : costCenter, months);
  }

  private static BigDecimal amount(String cell) {
    String text = cell.trim();
    return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
  }

  private static int firstDataRow(List<String> rows, int from) {
    for (int i = from; i < rows.size(); i++) {
      if (isData(rows.get(i))) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isData(String row) {
    return !row.isEmpty() && !row.startsWith("#");
  }

  private static BusinessRuleException invalid(List<String> errors) {
    return new BusinessRuleException("BUDGET_IMPORT_INVALID", String.join("; ", errors));
  }

  /**
   * One imported line.
   *
   * @param accountCode account code
   * @param costCenter cost centre or null
   * @param months twelve monthly amounts
   */
  public record ParsedLine(String accountCode, String costCenter, List<BigDecimal> months) {

    /** Canonical constructor copying the months. */
    public ParsedLine {
      months = List.copyOf(months);
    }
  }
}
