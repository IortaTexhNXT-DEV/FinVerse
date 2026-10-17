package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The ledger context of an invoice and, for an open legacy invoice, its number in the source system
 * (DATA_MIGRATION_DESIGN 14.1): columns {@code ledger_context} and {@code legacy_invoice_no} of
 * {@code ops_invoice}.
 *
 * @param ledgerContext NEW, or LEGACY for a legacy invoice and its family
 * @param legacyInvoiceNo invoice number in the source system, null for BIBS invoices
 */
@Embeddable
public record LegacyInvoiceRef(
    @Enumerated(EnumType.STRING) @Column(name = "ledger_context", nullable = false, length = 10)
        LedgerContext ledgerContext,
    @Column(name = "legacy_invoice_no", length = 40) String legacyInvoiceNo) {

  /** An invoice booked in BIBS. */
  public static final LegacyInvoiceRef NEW = new LegacyInvoiceRef(LedgerContext.NEW, null);

  /** A null context reads as NEW. */
  public LegacyInvoiceRef {
    ledgerContext = ledgerContext == null ? LedgerContext.NEW : ledgerContext;
  }

  /**
   * An open legacy invoice.
   *
   * @param legacyInvoiceNo number in the source system
   * @return reference
   */
  public static LegacyInvoiceRef legacy(String legacyInvoiceNo) {
    return new LegacyInvoiceRef(LedgerContext.LEGACY, legacyInvoiceNo);
  }

  /**
   * Whether the invoice posts to the legacy control accounts.
   *
   * @return true for LEGACY
   */
  public boolean isLegacy() {
    return ledgerContext == LedgerContext.LEGACY;
  }
}
