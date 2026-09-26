package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * What the posting of an endorsement request produced (ADJID.011-014).
 *
 * @param batchNo validation batch ({@code VB-<yyyy>})
 * @param endorsementNo booking endorsement number ({@code EN-<yyyy>}), null for internal requests
 * @param newInvoiceNo endorsement or return invoice booked, null when none
 * @param serviceInvoices service invoices issued or credited, comma separated
 * @param arInsurerAmount return premium set up as AR Insurer (remitted decrease), null when none
 * @param excessAmount excess moved to an unapplied item by the re-application, null when none
 * @param unappliedRef unapplied item of the excess, null when none
 */
@Embeddable
public record PostingOutcome(
    @Column(name = "batch_no", length = 40) String batchNo,
    @Column(name = "endorsement_no", length = 40) String endorsementNo,
    @Column(name = "new_invoice_no", length = 40) String newInvoiceNo,
    @Column(name = "service_invoices", length = 500) String serviceInvoices,
    @Column(name = "ar_insurer_amount", precision = 19, scale = 2) BigDecimal arInsurerAmount,
    @Column(name = "excess_amount", precision = 19, scale = 2) BigDecimal excessAmount,
    @Column(name = "unapplied_ref", length = 60) String unappliedRef) {

  /** Nothing posted. */
  public static final PostingOutcome NONE =
      new PostingOutcome(null, null, null, null, null, null, null);

  /**
   * The same outcome with the re-application result.
   *
   * @param excess excess moved to an unapplied item
   * @param unapplied unapplied item reference
   * @return outcome
   */
  public PostingOutcome withReapplication(BigDecimal excess, String unapplied) {
    return new PostingOutcome(
        batchNo, endorsementNo, newInvoiceNo, serviceInvoices, arInsurerAmount, excess, unapplied);
  }

  /**
   * The same outcome in a posting batch.
   *
   * @param batch validation batch number
   * @return outcome
   */
  public PostingOutcome inBatch(String batch) {
    return new PostingOutcome(
        batch,
        endorsementNo,
        newInvoiceNo,
        serviceInvoices,
        arInsurerAmount,
        excessAmount,
        unappliedRef);
  }
}
