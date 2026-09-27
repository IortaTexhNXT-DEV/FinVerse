package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;

/**
 * Origin of an invoice, unwrapped into the invoice responses (DATA_MIGRATION_DESIGN 14.1): BIBS or
 * MIGRATED with the source system, legacy reference and migration batch, the ledger context and the
 * legacy invoice number. The screens show a LEGACY badge for MIGRATED invoices.
 *
 * @param origin BIBS or MIGRATED
 * @param sourceSystem legacy source system
 * @param legacyRef legacy policy reference
 * @param migrationBatch loading batch
 * @param ledgerContext NEW or LEGACY
 * @param legacyInvoiceNo number in the source system
 */
public record InvoiceOriginResponse(
    RecordOrigin.Origin origin,
    String sourceSystem,
    String legacyRef,
    String migrationBatch,
    LedgerContext ledgerContext,
    String legacyInvoiceNo) {

  /**
   * Maps an invoice.
   *
   * @param i invoice
   * @return origin
   */
  public static InvoiceOriginResponse from(OpsInvoice i) {
    RecordOrigin o = i.getRecordOrigin() == null ? RecordOrigin.BIBS : i.getRecordOrigin();
    return new InvoiceOriginResponse(
        o.origin(),
        o.sourceSystem(),
        o.legacyRef(),
        o.migrationBatch(),
        i.getLegacy() == null ? LedgerContext.NEW : i.getLegacy().ledgerContext(),
        i.getLegacy() == null ? null : i.getLegacy().legacyInvoiceNo());
  }
}
