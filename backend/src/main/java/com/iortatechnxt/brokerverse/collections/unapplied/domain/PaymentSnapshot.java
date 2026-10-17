package com.iortatechnxt.brokerverse.collections.unapplied.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The fields of the "For Application To Invoice" file (BRCLXN.041, p.60) taken from the unapplied
 * payment when the request is made: payment date, payment file name, transaction no., paid amount,
 * payment type, payor, reference no. and the assured of the target invoice.
 *
 * @param paymentDate payment date
 * @param paymentFileName payment file (upload batch), may be null
 * @param transactionNo transaction no., may be null
 * @param paidAmount paid amount
 * @param currency currency
 * @param paymentType payment type
 * @param payorName payor
 * @param referenceNo payor's reference, may be null
 * @param assuredName assured of the target invoice, may be null
 */
@Embeddable
public record PaymentSnapshot(
    @Column(name = "payment_date") LocalDate paymentDate,
    @Column(name = "payment_file_name", length = 120) String paymentFileName,
    @Column(name = "transaction_no", length = 40) String transactionNo,
    @Column(name = "paid_amount", precision = 19, scale = 2) BigDecimal paidAmount,
    @Column(length = 3) String currency,
    @Column(name = "payment_type", length = 20) String paymentType,
    @Column(name = "payor_name", length = 250) String payorName,
    @Column(name = "reference_no", length = 80) String referenceNo,
    @Column(name = "assured_name", length = 250) String assuredName) {

  /** No payment fields (item unknown). */
  public static final PaymentSnapshot EMPTY =
      new PaymentSnapshot(null, null, null, null, null, null, null, null, null);
}
