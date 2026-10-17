package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * The account an ACSL case is about (ACSL 2.5.1-2.5.3): the invoice and its family root, the
 * insurer and client, the AR of the payment and the amount concerned.
 *
 * @param invoiceNo invoice
 * @param rootInvoiceNo root invoice of the family (ACSL 2.16.0)
 * @param insurerCode insurer
 * @param clientCode client
 * @param arNo acknowledgement receipt of the payment
 * @param currency currency
 * @param amount amount concerned
 */
@Embeddable
public record CaseSubject(
    @Column(name = "invoice_no", length = 40) String invoiceNo,
    @Column(name = "root_invoice_no", length = 40) String rootInvoiceNo,
    @Column(name = "insurer_code", length = 30) String insurerCode,
    @Column(name = "client_code", length = 30) String clientCode,
    @Column(name = "ar_no", length = 40) String arNo,
    @Column(length = 3) String currency,
    @Column(precision = 19, scale = 2) BigDecimal amount) {

  /** No account. */
  public static final CaseSubject NONE = new CaseSubject(null, null, null, null, null, null, null);
}
