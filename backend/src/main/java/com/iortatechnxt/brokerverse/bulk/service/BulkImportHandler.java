package com.iortatechnxt.brokerverse.bulk.service;

import java.util.List;
import java.util.Locale;

/**
 * Port for one kind of bulk upload (bulk quotations, accounts, client updates, booking...).
 * Implement it as a Spring bean in the owning module's {@code service} package; the framework
 * provides template, parsing, review, commit and reports.
 *
 * <p>{@link #commit} (or {@link #process}) runs once per valid row in its own transaction: throw a
 * {@code BusinessRuleException} to fail that row only.
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
   * Creates or updates the business record(s) of a valid row. Implement this, or {@link #process}
   * when the handler sorts its rows into outcome categories.
   *
   * @param row row
   * @param context run context
   * @return reference of the record created or updated (shown in the report)
   */
  default String commit(BulkRow row, BulkContext context) {
    throw new IllegalStateException(code() + " implements neither commit nor process");
  }

  /**
   * Commits a valid row and tells its outcome category (BRQID.006). The default commits the row
   * with {@link #commit} and returns no category.
   *
   * @param row row
   * @param context run context
   * @return reference and category
   */
  default BulkOutcome process(BulkRow row, BulkContext context) {
    return BulkOutcome.of(commit(row, context));
  }

  /**
   * The outcome categories {@link #process} returns, in report order (e.g. APPLIED, UNAPPLIED,
   * PREBOOKED, EXCESS). Empty for handlers that do not categorise.
   *
   * @return categories
   */
  default List<String> outcomeCategories() {
    return List.of();
  }

  /**
   * Layout of plain-text (.txt) files of this handler (CSHID.008). The default is a delimited file
   * with a header line whose separator is detected.
   *
   * @return layout
   */
  default TextLayout textLayout() {
    return TextLayout.AUTO;
  }

  /**
   * Whether a file identical (same SHA-256) to an earlier upload of this handler that was not
   * cancelled is refused (CSHID.008, PRCID.010: the same payment or insurer file twice).
   *
   * @return true to refuse duplicate files
   */
  default boolean blocksDuplicateFiles() {
    return false;
  }

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
