package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The control file of an extract (DATA_MIGRATION_DESIGN section 5.1; workbook layout CONTROL): one
 * row per measure - ROW_COUNT, HASH_TOTAL (with its column), AMOUNT_TOTAL (with column, currency
 * and an optional filter such as {@code component=BASIC}) and SHA256 - with the object, layout,
 * source system, data file name, as-of time and who extracted it.
 *
 * @param object object code
 * @param layout layout code
 * @param sourceSystem source system
 * @param dataFile data file name
 * @param asOf as-of time
 * @param extractedAt extraction time
 * @param extractedBy extracted by
 * @param rowCount declared row count
 * @param hashColumns column(s) of the hash total ({@code +} separated), blank for the layout rule
 * @param hashTotal declared hash total
 * @param sha256 declared SHA-256 of the data file
 * @param amounts declared amount totals
 */
public record ControlFile(
    String object,
    String layout,
    String sourceSystem,
    String dataFile,
    LocalDateTime asOf,
    LocalDateTime extractedAt,
    String extractedBy,
    Integer rowCount,
    String hashColumns,
    String hashTotal,
    String sha256,
    List<AmountTotal> amounts) {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** Defensive copy. */
  public ControlFile {
    amounts = List.copyOf(amounts);
  }

  /**
   * Reads a parsed control file.
   *
   * @param parsed rows of the control file
   * @return control file
   */
  public static ControlFile of(ParsedFile parsed) {
    if (parsed.rows().isEmpty()) {
      throw invalid("The control file has no measures");
    }
    Map<String, String> first = parsed.rows().get(0).values();
    Builder b = new Builder(first);
    for (ParsedFile.RawRow row : parsed.rows()) {
      b.measure(row.values());
    }
    return b.build();
  }

  private static BusinessRuleException invalid(String message) {
    return new BusinessRuleException("MIG_CONTROL_FILE", message);
  }

  private static LocalDateTime stamp(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      String v = value.strip();
      return v.length() == "yyyy-MM-dd".length()
          ? LocalDateTime.parse(v + " 00:00:00", STAMP)
          : LocalDateTime.parse(v, STAMP);
    } catch (DateTimeParseException e) {
      throw invalid("The " + field + " of the control file is not yyyy-MM-dd HH:mm:ss");
    }
  }

  /**
   * A declared amount total.
   *
   * @param column amount column
   * @param currency currency
   * @param filterColumn column of the filter, null when none
   * @param filterValue value of the filter
   * @param value declared total
   */
  public record AmountTotal(
      String column, String currency, String filterColumn, String filterValue, BigDecimal value) {

    /**
     * Label used in messages.
     *
     * @return column, filter and currency
     */
    public String label() {
      return column
          + (filterColumn == null ? "" : " (" + filterColumn + "=" + filterValue + ")")
          + (currency == null ? "" : " in " + currency);
    }
  }

  private static final class Builder {
    private final Map<String, String> head;
    private Integer rowCount;
    private String hashColumns;
    private String hashTotal;
    private String sha256;
    private final List<AmountTotal> amounts = new ArrayList<>();

    Builder(Map<String, String> head) {
      this.head = head;
    }

    void measure(Map<String, String> v) {
      String measure = v.getOrDefault("measure", "").strip().toUpperCase(Locale.ROOT);
      String value = v.getOrDefault("value", "").strip();
      switch (measure) {
        case "ROW_COUNT" -> rowCount = integer(value);
        case "HASH_TOTAL" -> {
          hashColumns = v.get("column_name");
          hashTotal = value;
        }
        case "SHA256" -> sha256 = value.toLowerCase(Locale.ROOT);
        case "AMOUNT_TOTAL" -> amounts.add(amount(v, value));
        default -> throw invalid("Unknown measure " + measure + " in the control file");
      }
    }

    private static AmountTotal amount(Map<String, String> v, String value) {
      String filter = v.get("filter");
      String filterColumn = null;
      String filterValue = null;
      if (filter != null && filter.contains("=")) {
        filterColumn = filter.substring(0, filter.indexOf('=')).strip();
        filterValue = filter.substring(filter.indexOf('=') + 1).strip();
      }
      String currency = v.get("currency");
      return new AmountTotal(
          v.get("column_name"),
          currency == null ? null : currency.strip().toUpperCase(Locale.ROOT),
          filterColumn,
          filterValue,
          decimal(value));
    }

    private static Integer integer(String value) {
      try {
        return Integer.valueOf(value);
      } catch (NumberFormatException e) {
        throw invalid("The row count of the control file is not a number");
      }
    }

    private static BigDecimal decimal(String value) {
      try {
        return new BigDecimal(value);
      } catch (NumberFormatException e) {
        throw invalid("The amount total " + value + " of the control file is not a number");
      }
    }

    ControlFile build() {
      return new ControlFile(
          upper(head.get("object")),
          upper(head.get("layout")),
          upper(head.get("source_system")),
          head.get("data_file"),
          stamp(head.get("as_of"), "as-of time"),
          stamp(head.get("extracted_at"), "extraction time"),
          head.get("extracted_by"),
          rowCount,
          hashColumns,
          hashTotal,
          sha256,
          amounts);
    }

    private static String upper(String s) {
      return s == null ? null : s.strip().toUpperCase(Locale.ROOT);
    }
  }
}
