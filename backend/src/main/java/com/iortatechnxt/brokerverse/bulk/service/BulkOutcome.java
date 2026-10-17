package com.iortatechnxt.brokerverse.bulk.service;

/**
 * Result of committing one row: the record created or updated and, for handlers that sort their
 * rows into business categories, the category (BRQID.006: e.g. APPLIED, UNAPPLIED, PREBOOKED,
 * EXCESS for payment files). The categories a handler uses are listed by {@link
 * BulkImportHandler#outcomeCategories()}.
 *
 * @param reference reference of the record created or updated (shown in the report)
 * @param category outcome category, null when the handler does not categorise
 */
public record BulkOutcome(String reference, String category) {

  /**
   * An uncategorised outcome.
   *
   * @param reference record created or updated
   * @return outcome
   */
  public static BulkOutcome of(String reference) {
    return new BulkOutcome(reference, null);
  }
}
