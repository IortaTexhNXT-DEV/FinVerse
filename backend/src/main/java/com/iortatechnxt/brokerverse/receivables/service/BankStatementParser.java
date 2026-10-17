package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatement.StatementSummary;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine.ParsedLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses a bank statement in CSV format (documented in docs/samples/BANK_STATEMENT_FORMAT.md).
 *
 * <ul>
 *   <li>First row: header with the columns {@code date, description, reference, debit, credit,
 *       balance} in any order (case insensitive; {@code balance} optional).
 *   <li>Dates: {@code yyyy-MM-dd}, {@code dd/MM/yyyy} or {@code dd-MM-yyyy}.
 *   <li>Amounts: plain decimals, thousands separators allowed inside quotes; debit = withdrawal,
 *       credit = deposit (exactly one of them per line).
 *   <li>Balance: running balance after the line. When present it is checked against the computed
 *       running balance; when absent it is computed from the opening balance.
 * </ul>
 */
public final class BankStatementParser {

  private static final List<String> REQUIRED = List.of("date", "debit", "credit");
  private static final List<DateTimeFormatter> DATE_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE,
          DateTimeFormatter.ofPattern("dd/MM/uuuu"),
          DateTimeFormatter.ofPattern("dd-MM-uuuu"));
  private static final String INVALID = "INVALID_STATEMENT";
  private static final String BALANCE = "balance";
  private static final String ROW = "Row ";
  private static final int MAX_TEXT = 250;
  private static final int MAX_REFERENCE = 60;
  private static final Pattern AMOUNT = Pattern.compile("-?\\d{1,17}(\\.\\d{1,2})?");

  private BankStatementParser() {}

  /**
   * Parses statement text.
   *
   * @param content CSV text
   * @param openingBalance balance before the first line, or null to derive it from the first line
   * @return parsed lines and summary
   */
  public static ParsedStatement parse(String content, BigDecimal openingBalance) {
    List<List<String>> rows = new ArrayList<>();
    for (String raw : content.split("\\r?\\n")) {
      if (!raw.isBlank()) {
        rows.add(splitCsv(raw));
      }
    }
    if (rows.size() < 2) {
      throw new BusinessRuleException(INVALID, "The statement has no lines");
    }
    Map<String, Integer> header = header(rows.get(0));
    List<Row> parsed = new ArrayList<>();
    for (int i = 1; i < rows.size(); i++) {
      parsed.add(row(rows.get(i), header, i + 1));
    }
    BigDecimal opening = openingBalance != null ? openingBalance : derivedOpening(parsed.get(0));
    return build(parsed, Money.round(opening));
  }

  private static ParsedStatement build(List<Row> rows, BigDecimal opening) {
    List<ParsedLine> lines = new ArrayList<>();
    BigDecimal running = opening;
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    LocalDate first = rows.get(0).date();
    LocalDate last = first;
    int lineNo = 0;
    for (Row r : rows) {
      running = running.add(r.credit()).subtract(r.debit());
      if (r.balance() != null && r.balance().compareTo(running) != 0) {
        throw new BusinessRuleException(
            INVALID,
            ROW + r.rowNo() + ": balance " + r.balance() + " does not agree with " + running);
      }
      totalDebit = totalDebit.add(r.debit());
      totalCredit = totalCredit.add(r.credit());
      first = r.date().isBefore(first) ? r.date() : first;
      last = r.date().isAfter(last) ? r.date() : last;
      lineNo++;
      lines.add(
          new ParsedLine(
              lineNo, r.date(), r.description(), r.reference(), r.debit(), r.credit(), running));
    }
    return new ParsedStatement(
        lines,
        new StatementSummary(first, last, opening, running, totalDebit, totalCredit, lineNo));
  }

  private static BigDecimal derivedOpening(Row first) {
    return first.balance() == null
        ? BigDecimal.ZERO
        : first.balance().subtract(first.credit()).add(first.debit());
  }

  private static Map<String, Integer> header(List<String> cells) {
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < cells.size(); i++) {
      index.put(cells.get(i).trim().toLowerCase(Locale.ROOT), i);
    }
    for (String column : REQUIRED) {
      if (!index.containsKey(column)) {
        throw new BusinessRuleException(
            INVALID, "Missing column '" + column + "' (header: date,description,reference,...)");
      }
    }
    return index;
  }

  private static Row row(List<String> cells, Map<String, Integer> header, int rowNo) {
    BigDecimal debit = amount(cell(cells, header, "debit"), rowNo);
    BigDecimal credit = amount(cell(cells, header, "credit"), rowNo);
    if ((debit.signum() == 0) == (credit.signum() == 0)
        || debit.signum() < 0
        || credit.signum() < 0) {
      throw new BusinessRuleException(
          INVALID, ROW + rowNo + ": enter a positive debit or a positive credit");
    }
    String balance = cell(cells, header, BALANCE);
    return new Row(
        rowNo,
        date(cell(cells, header, "date"), rowNo),
        limit(cell(cells, header, "description"), MAX_TEXT),
        limit(cell(cells, header, "reference"), MAX_REFERENCE),
        debit,
        credit,
        balance.isEmpty() ? null : amount(balance, rowNo));
  }

  private static String cell(List<String> cells, Map<String, Integer> header, String column) {
    Integer i = header.get(column);
    return i == null || i >= cells.size() ? "" : cells.get(i).trim();
  }

  private static LocalDate date(String text, int rowNo) {
    for (DateTimeFormatter f : DATE_FORMATS) {
      try {
        return LocalDate.parse(text, f);
      } catch (DateTimeParseException ignored) {
        // try the next accepted format
      }
    }
    throw new BusinessRuleException(INVALID, ROW + rowNo + ": invalid date '" + text + "'");
  }

  private static BigDecimal amount(String text, int rowNo) {
    if (text.isEmpty()) {
      return Money.zero();
    }
    String plain = text.replace(",", "");
    if (!AMOUNT.matcher(plain).matches()) {
      throw new BusinessRuleException(INVALID, ROW + rowNo + ": invalid amount '" + text + "'");
    }
    return Money.round(new BigDecimal(plain));
  }

  private static String limit(String text, int max) {
    if (text.isEmpty()) {
      return null;
    }
    return text.length() > max ? text.substring(0, max) : text;
  }

  /**
   * Splits one CSV row; supports double-quoted fields with commas and doubled quotes.
   *
   * @param line row text
   * @return cells
   */
  static List<String> splitCsv(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    int i = 0;
    while (i < line.length()) {
      char c = line.charAt(i);
      boolean escapedQuote =
          c == '"' && quoted && i + 1 < line.length() && line.charAt(i + 1) == '"';
      if (escapedQuote) {
        current.append('"');
        i++;
      } else if (c == '"') {
        quoted = !quoted;
      } else if (c == ',' && !quoted) {
        cells.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
      i++;
    }
    cells.add(current.toString());
    return cells;
  }

  /**
   * Parsed statement.
   *
   * @param lines lines with running balances
   * @param summary period, balances and totals
   */
  public record ParsedStatement(List<ParsedLine> lines, StatementSummary summary) {}

  private record Row(
      int rowNo,
      LocalDate date,
      String description,
      String reference,
      BigDecimal debit,
      BigDecimal credit,
      BigDecimal balance) {}
}
