package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * One parsed data row of a journal upload file: the voucher header values it carries, the journal
 * line and the row-level validation errors.
 *
 * @param rowNumber 1-based row number in the file (the header is row 1)
 * @param voucherKey voucher key grouping rows into one journal
 * @param header header values (blank when the row inherits them from the voucher's first row)
 * @param line journal line (null when the row has errors)
 * @param errors row-level errors
 */
public record UploadLine(
    int rowNumber,
    String voucherKey,
    Map<String, String> header,
    JournalLineRequest line,
    List<String> errors) {

  /** Voucher key column. */
  public static final String VOUCHER_KEY = "voucher_key";

  /** Branch code column. */
  public static final String BRANCH_CODE = "branch_code";

  /** Journal type column. */
  public static final String JOURNAL_TYPE = "journal_type";

  /** Value date column. */
  public static final String VALUE_DATE = "value_date";

  /** Header currency column. */
  public static final String CURRENCY = "currency";

  /** Voucher narration column. */
  public static final String NARRATION = "narration";

  /** Voucher reference column. */
  public static final String REFERENCE = "reference";

  /** Account code column. */
  public static final String ACCOUNT_CODE = "account_code";

  /** Debit amount column. */
  public static final String DEBIT = "debit";

  /** Credit amount column. */
  public static final String CREDIT = "credit";

  private static final String LINE_CURRENCY = "line_currency";
  private static final String EXCHANGE_RATE = "exchange_rate";

  /** Columns every file must contain. */
  public static final List<String> REQUIRED_COLUMNS =
      List.of(
          VOUCHER_KEY, BRANCH_CODE, VALUE_DATE, CURRENCY, NARRATION, ACCOUNT_CODE, DEBIT, CREDIT);

  /** Header columns that must be identical on all rows of a voucher. */
  public static final List<String> HEADER_COLUMNS =
      List.of(BRANCH_CODE, JOURNAL_TYPE, VALUE_DATE, CURRENCY, NARRATION, REFERENCE);

  /** All columns in template order. */
  public static final List<String> ALL_COLUMNS =
      List.of(
          VOUCHER_KEY,
          BRANCH_CODE,
          JOURNAL_TYPE,
          VALUE_DATE,
          CURRENCY,
          NARRATION,
          REFERENCE,
          ACCOUNT_CODE,
          DEBIT,
          CREDIT,
          LINE_CURRENCY,
          EXCHANGE_RATE,
          "cost_center",
          "business_line",
          "party_code",
          "line_reference",
          "line_narration");

  private static final Pattern AMOUNT = Pattern.compile("\\d{1,17}(\\.\\d{1,2})?");
  private static final Pattern RATE = Pattern.compile("\\d{1,11}(\\.\\d{1,8})?");
  private static final Pattern CCY = Pattern.compile("[A-Z]{3}");

  /** Canonical constructor copying collections. */
  public UploadLine {
    header = Map.copyOf(header);
    errors = List.copyOf(errors);
  }

  /**
   * Whether the row passed row-level validation.
   *
   * @return true when valid
   */
  public boolean valid() {
    return errors.isEmpty();
  }

  /**
   * Parses and validates one data row.
   *
   * @param rowNumber row number
   * @param v cell values by column name (missing columns = blank)
   * @return parsed row
   */
  public static UploadLine parse(int rowNumber, Map<String, String> v) {
    List<String> errors = new ArrayList<>();
    String key = v.getOrDefault(VOUCHER_KEY, "");
    if (key.isBlank()) {
      errors.add("voucher_key is required");
    }
    checkHeader(v, errors);
    JournalLineRequest line = parseLine(v, errors);
    return new UploadLine(rowNumber, key, headerValues(v), errors.isEmpty() ? line : null, errors);
  }

  private static Map<String, String> headerValues(Map<String, String> v) {
    Map<String, String> result = new HashMap<>();
    HEADER_COLUMNS.forEach(c -> result.put(c, v.getOrDefault(c, "")));
    return result;
  }

  private static void checkHeader(Map<String, String> v, List<String> errors) {
    String date = v.getOrDefault(VALUE_DATE, "");
    if (!date.isBlank() && !isIsoDate(date)) {
      errors.add("value_date must be an ISO date (YYYY-MM-DD)");
    }
    String type = v.getOrDefault(JOURNAL_TYPE, "");
    if (!type.isBlank() && !List.of("MANUAL", "ADJUSTMENT", "ACCRUAL").contains(upper(type))) {
      errors.add("journal_type must be MANUAL, ADJUSTMENT or ACCRUAL");
    }
    checkCurrency(v.getOrDefault(CURRENCY, ""), CURRENCY, errors);
  }

  private static JournalLineRequest parseLine(Map<String, String> v, List<String> errors) {
    String account = v.getOrDefault(ACCOUNT_CODE, "");
    if (account.isBlank()) {
      errors.add("account_code is required");
    }
    BigDecimal debit = amount(v.getOrDefault(DEBIT, ""), DEBIT, errors);
    BigDecimal credit = amount(v.getOrDefault(CREDIT, ""), CREDIT, errors);
    boolean isDebit = debit.signum() > 0;
    if (isDebit == credit.signum() > 0) {
      errors.add("enter either a debit or a credit amount greater than zero");
    }
    String lineCurrency = v.getOrDefault(LINE_CURRENCY, "");
    checkCurrency(lineCurrency, LINE_CURRENCY, errors);
    BigDecimal rate = rate(v.getOrDefault(EXCHANGE_RATE, ""), errors);
    return new JournalLineRequest(
        account,
        isDebit ? BalanceSide.DEBIT : BalanceSide.CREDIT,
        isDebit ? debit : credit,
        blankToNull(upper(lineCurrency)),
        rate,
        null,
        blankToNull(v.get("cost_center")),
        blankToNull(v.get("business_line")),
        blankToNull(v.get("party_code")),
        blankToNull(v.get("line_reference")),
        blankToNull(v.get("line_narration")));
  }

  private static boolean isIsoDate(String text) {
    try {
      return LocalDate.parse(text) != null;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private static BigDecimal rate(String text, List<String> errors) {
    if (text.isBlank()) {
      return null;
    }
    if (!RATE.matcher(text).matches() || new BigDecimal(text).signum() <= 0) {
      errors.add("exchange_rate must be a positive number");
      return null;
    }
    return new BigDecimal(text);
  }

  private static BigDecimal amount(String text, String column, List<String> errors) {
    String clean = text.replace(",", "");
    if (clean.isBlank()) {
      return BigDecimal.ZERO;
    }
    if (!AMOUNT.matcher(clean).matches()) {
      errors.add(column + " must be a positive amount with at most 2 decimals");
      return BigDecimal.ZERO;
    }
    return new BigDecimal(clean);
  }

  private static void checkCurrency(String value, String column, List<String> errors) {
    if (!value.isBlank() && !CCY.matcher(upper(value)).matches()) {
      errors.add(column + " must be a 3-letter ISO code");
    }
  }

  /**
   * Upper-cases a value (ISO codes, enum names).
   *
   * @param value value
   * @return upper-case value
   */
  static String upper(String value) {
    return value.strip().toUpperCase(Locale.ROOT);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Parsed journal type (MANUAL when blank).
   *
   * @param value cell value
   * @return type
   */
  static JournalType journalType(String value) {
    return value.isBlank() ? JournalType.MANUAL : JournalType.valueOf(upper(value));
  }
}
