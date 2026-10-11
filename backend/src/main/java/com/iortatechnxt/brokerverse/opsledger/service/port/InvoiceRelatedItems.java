package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port: records of an Operations module that concern an invoice, shown on the invoice 360 view
 * (RMTID.026, ADJID.024): receipts and applications (cashiering), remittance batches and holds
 * (remittance), recon lines (prodrecon), endorsement requests (adjustment), DP items (commission).
 * Every module implements it as a Spring bean; the view lists all of them.
 */
public interface InvoiceRelatedItems {

  /**
   * Section of the 360 view the items belong to.
   *
   * @return section
   */
  Section section();

  /**
   * The module's records of an invoice.
   *
   * @param invoiceNo invoice number
   * @return items, newest first
   */
  List<RelatedItem> itemsFor(String invoiceNo);

  /** Sections of the invoice 360 view. */
  enum Section {
    /** Receipts, applications, unapplied items (cashiering). */
    RECEIPTS,
    /** Remittance batches, holds, special remittances (remittance). */
    REMITTANCES,
    /** Endorsement requests and slips (adjustment). */
    ADJUSTMENTS,
    /** Production reconciliation lines (prodrecon). */
    RECONCILIATION,
    /** DP items, billings, commission ORs (commission). */
    COMMISSION,
    /** Documents (any module). */
    DOCUMENTS
  }

  /**
   * One related record.
   *
   * @param type record type (e.g. AR, OR, REMITTANCE_BATCH, ENDORSEMENT_REQUEST)
   * @param reference record number
   * @param date record date
   * @param amount amount concerning the invoice, may be null
   * @param status record status
   * @param description short description
   * @param link frontend route of the record, may be null
   */
  record RelatedItem(
      String type,
      String reference,
      LocalDate date,
      BigDecimal amount,
      String status,
      String description,
      String link) {}
}
