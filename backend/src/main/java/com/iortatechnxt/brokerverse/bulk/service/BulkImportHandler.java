package com.iortatechnxt.brokerverse.bulk.service;

import java.util.List;
import java.util.Locale;

/**
 * Port for one kind of bulk upload (bulk quotations, accounts, client updates, booking...).
 * Implement it as a Spring bean in the owning module's {@code service} package; the framework
 * provides template, parsing, review, commit and reports.
 *
 * <p>{@link #commit} runs once per valid row in its own transaction: throw a {@code
 * BusinessRuleException} to fail that row only.
 */
public interface BulkImportHandler {

  /**
   * Unique handler code (UPPER_SNAKE_CASE), used in URLs.
   *
   * @return code
   */
  String code();

  /**
   * Screen title, e.g. "Bulk account creation".
   *
   * @return title
   */
  String title();

  /**
   * Permission needed to use the handler (in addition to {@code BULK_PROCESS}).
   *
   * @return permission name
   */
  String permission();

  /**
   * Template columns in order.
   *
   * @return columns
   */
  List<BulkColumn> columns();

  /**
   * Extra instructions shown on the template's instructions sheet.
   *
   * @return text, may be empty
   */
  default String instructions() {
    return "";
  }

  /**
   * Normalises one value before validation (BRNB.064 "sanitise the list"). The default trims and
   * collapses inner whitespace; override to upper-case plate numbers, strip dashes, etc.
   *
   * @param header column
   * @param value raw value (never null)
   * @return clean value; blank means "no value"
   */
  default String sanitize(String header, String value) {
    return value.trim().replaceAll("\\s+", " ");
  }

  /**
   * Key identifying duplicates inside one file (e.g. plate number); rows sharing a key after the
   * first are rejected. Null disables the check for the row.
   *
   * @param row row
   * @return key or null
   */
  default String duplicateKey(BulkRow row) {
    return null;
  }

  /**
   * Validates a row against business rules (existing records, lists of values...).
   *
   * @param row row
   * @param context run context
   * @return error messages; empty when valid
   */
  List<String> validate(BulkRow row, BulkContext context);

  /**
   * Creates or updates the business record(s) of a valid row.
   *
   * @param row row
   * @param context run context
   * @return reference of the record created or updated (shown in the report)
   */
  String commit(BulkRow row, BulkContext context);

  /**
   * Upper-cases and removes spaces (helper for identifiers such as plate or engine numbers).
   *
   * @param value value
   * @return normalised identifier
   */
  static String identifier(String value) {
    return value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }
}
