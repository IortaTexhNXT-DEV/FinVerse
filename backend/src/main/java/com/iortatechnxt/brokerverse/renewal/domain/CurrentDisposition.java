package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The current disposition of a candidate: a copy of its latest {@link Disposition} row for the
 * lists and reports (BRD 2.004).
 *
 * @param code disposition
 * @param reasonCode reason for Not for Renewal (list RNW_NONRENEWAL_REASON)
 * @param source who gave it
 * @param remarks remarks
 * @param newInvoiceNo new invoice number (reason Booked to New Invoice)
 */
@Embeddable
public record CurrentDisposition(
    @Enumerated(EnumType.STRING) @Column(name = "disposition", length = 20) RenewalDisposition code,
    @Column(name = "nonrenewal_reason", length = 40) String reasonCode,
    @Enumerated(EnumType.STRING) @Column(name = "disposition_source", length = 20)
        DispositionSource source,
    @Column(name = "disposition_remarks", length = 200) String remarks,
    @Column(name = "new_invoice_no", length = 40) String newInvoiceNo) {

  /** No disposition yet. */
  public static final CurrentDisposition NONE =
      new CurrentDisposition(null, null, null, null, null);

  /**
   * Whether a disposition was given.
   *
   * @return true when set
   */
  public boolean given() {
    return code != null;
  }
}
