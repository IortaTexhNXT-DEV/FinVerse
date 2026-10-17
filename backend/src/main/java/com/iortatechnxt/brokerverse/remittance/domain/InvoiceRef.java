package com.iortatechnxt.brokerverse.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The invoice a hold or special remittance request concerns (MKTID.003/009), copied from the
 * Operations ledger as plain values (no foreign keys to the V8xx tables).
 *
 * @param invoiceNo invoice
 * @param arn ARN
 * @param insurerCode insurer
 * @param clientCode client
 * @param assuredName assured
 */
@Embeddable
public record InvoiceRef(
    @Column(name = "invoice_no", nullable = false, length = 40, updatable = false) String invoiceNo,
    @Column(nullable = false, length = 30, updatable = false) String arn,
    @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
        String insurerCode,
    @Column(name = "client_code", nullable = false, length = 30, updatable = false)
        String clientCode,
    @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
        String assuredName) {}
