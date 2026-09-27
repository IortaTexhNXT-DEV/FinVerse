package com.iortatechnxt.brokerverse.opsledger.domain;

/**
 * The ledger an invoice posts to (DATA_MIGRATION_DESIGN 14.2): NEW for invoices booked in BIBS,
 * LEGACY for open legacy invoices and every invoice of their family. Postings of a LEGACY invoice
 * use the {@code LG_} amount components, which the accounting rules route to the legacy control
 * accounts so the legacy positions run off visibly.
 */
public enum LedgerContext {
  /** Invoices booked in BIBS. */
  NEW(""),
  /** Legacy invoices and their family. */
  LEGACY("LG_");

  private final String prefix;

  LedgerContext(String prefix) {
    this.prefix = prefix;
  }

  /**
   * The accounting amount component of this context.
   *
   * @param base component of the NEW context (e.g. PR_BASIC, DTIP, APPLIED)
   * @return the component itself for NEW, {@code LG_} and the component for LEGACY
   */
  public String component(String base) {
    return prefix + base;
  }
}
