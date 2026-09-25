package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.AliasType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One record of a list file in the SCR_WATCHLIST template (SNSRP-201, 202): its columns, and the
 * conversion of a row to entry values with the FRS messages ("Line n: name is missing", "Line n:
 * birth date is not a valid date").
 *
 * @param line line in the file (header = 1)
 * @param reference reference of the record in the source
 * @param values entry values
 * @param remarks remarks of the record, may be {@code null}
 */
public record ListRecord(int line, String reference, EntryValues values, String remarks) {

  /** Column: reference in the source list. */
  public static final String REFERENCE = "Reference";

  /** Column: INDIVIDUAL or ENTITY. */
  public static final String ENTITY_TYPE = "Entity Type";

  /** Column: name as listed. */
  public static final String PRIMARY_NAME = "Primary Name";

  /** Column: first name. */
  public static final String FIRST_NAME = "First Name";

  /** Column: last name. */
  public static final String LAST_NAME = "Last Name";

  /** Column: aliases separated by semicolons. */
  public static final String ALIASES = "Aliases";

  /** Column: birth date. */
  public static final String BIRTH_DATE = "Birth Date";

  /** Column: nationality. */
  public static final String NATIONALITY = "Nationality";

  /** Column: ID numbers. */
  public static final String ID_NUMBERS = "ID Numbers";

  /** Column: listing date. */
  public static final String LISTED_ON = "Listed On";

  /** Column: list type (blank = the source's). */
  public static final String LIST_TYPE = "List Type";

  /** Column: remarks. */
  public static final String REMARKS = "Remarks";

  private static final String LINE = "Line ";
  private static final List<DateTimeFormatter> DATES =
      List.of(
          DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
          DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
          DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));

  /**
   * The template columns (bulk handler SCR_WATCHLIST and the upload check).
   *
   * @return columns in order
   */
  public static List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(REFERENCE, "Reference of the record in the source list", "UNSC-0001"),
        BulkColumn.required(ENTITY_TYPE, "INDIVIDUAL or ENTITY", "INDIVIDUAL"),
        BulkColumn.required(PRIMARY_NAME, "Name as listed", "Dela Cruz, Juan Invented"),
        BulkColumn.optional(FIRST_NAME, "First name (individuals)", "Juan"),
        BulkColumn.optional(LAST_NAME, "Last name (individuals)", "Dela Cruz"),
        BulkColumn.optional(ALIASES, "Aliases separated by semicolons", "Juanito DC; J. D. Cruz"),
        BulkColumn.optional(BIRTH_DATE, "Birth date yyyy-MM-dd", "1970-02-28"),
        BulkColumn.optional(NATIONALITY, "Nationality", "Filipino"),
        BulkColumn.optional(ID_NUMBERS, "Identification numbers", "P1234567"),
        BulkColumn.optional(LISTED_ON, "Listing date yyyy-MM-dd", "2026-01-15"),
        BulkColumn.optional(LIST_TYPE, "SANCTION, PEP, INTERNAL; blank = the source's", ""),
        BulkColumn.optional(REMARKS, "Remarks", "Advisory 2026-01"));
  }

  /**
   * The mandatory headers of a list file.
   *
   * @return headers
   */
  public static List<String> requiredHeaders() {
    return List.of(REFERENCE, ENTITY_TYPE, PRIMARY_NAME);
  }

  /**
   * Converts a row; returns the error messages instead when the row is invalid.
   *
   * @param line line number
   * @param row values by header (blank cells absent)
   * @param defaultListType the source's list type
   * @param errors receives the messages of an invalid row
   * @return the record, {@code null} when invalid
   */
  public static ListRecord read(
      int line, Map<String, String> row, String defaultListType, List<String> errors) {
    String reference = text(row, REFERENCE);
    String name = text(row, PRIMARY_NAME);
    if (reference == null) {
      errors.add(LINE + line + ": reference is missing");
    }
    if (name == null) {
      errors.add(LINE + line + ": name is missing");
    }
    SubjectType type = entityType(text(row, ENTITY_TYPE), line, errors);
    LocalDate birth = date(row, BIRTH_DATE, "birth date", line, errors);
    LocalDate listed = date(row, LISTED_ON, "listing date", line, errors);
    if (!errors.isEmpty()) {
      return null;
    }
    String listType = text(row, LIST_TYPE);
    EntryValues values =
        new EntryValues(
            listType == null ? defaultListType : listType.toUpperCase(Locale.ROOT),
            type,
            name,
            text(row, FIRST_NAME),
            text(row, LAST_NAME),
            birth,
            text(row, NATIONALITY),
            text(row, ID_NUMBERS),
            listed,
            null,
            aliases(text(row, ALIASES)));
    return new ListRecord(line, reference, values, text(row, REMARKS));
  }

  private static SubjectType entityType(String value, int line, List<String> errors) {
    if (value == null) {
      return SubjectType.INDIVIDUAL;
    }
    try {
      return SubjectType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      errors.add(LINE + line + ": entity type must be INDIVIDUAL or ENTITY");
      return null;
    }
  }

  private static LocalDate date(
      Map<String, String> row, String header, String label, int line, List<String> errors) {
    String value = text(row, header);
    if (value == null) {
      return null;
    }
    for (DateTimeFormatter format : DATES) {
      try {
        return LocalDate.parse(value, format);
      } catch (DateTimeParseException ignored) {
        // try the next accepted format
      }
    }
    errors.add(LINE + line + ": " + label + " is not a valid date");
    return null;
  }

  private static List<EntryValues.Alias> aliases(String text) {
    if (text == null) {
      return List.of();
    }
    List<EntryValues.Alias> out = new ArrayList<>();
    Arrays.stream(text.split(";"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .forEach(s -> out.add(new EntryValues.Alias(s, AliasType.AKA)));
    return out;
  }

  private static String text(Map<String, String> row, String header) {
    String value = row.get(header);
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * The raw record as one line (for the error log), in template column order.
   *
   * @param row values by header
   * @return text
   */
  public static String raw(Map<String, String> row) {
    List<String> cells = new ArrayList<>();
    for (BulkColumn column : columns()) {
      cells.add(row.getOrDefault(column.header(), ""));
    }
    return String.join(",", cells);
  }
}
