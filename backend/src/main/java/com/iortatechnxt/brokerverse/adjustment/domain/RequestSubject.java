package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The booked invoice an endorsement request is raised on, copied from the Operations ledger when
 * the request is created (ADJID.001/020: the ARN and the invoice number stay on the request).
 *
 * @param invoiceNo invoice number
 * @param arn Account Reference Number
 * @param policyNo policy number, may be null
 * @param clientCode client code
 * @param assuredName assured name
 * @param insurerCode lead insurer
 * @param currency currency
 * @param segment market segment
 * @param aoUsername requesting Account Officer
 * @param productLine product line (risk type)
 */
@Embeddable
public record RequestSubject(
    @Column(name = "invoice_no", nullable = false, length = 40, updatable = false) String invoiceNo,
    @Column(nullable = false, length = 30, updatable = false) String arn,
    @Column(name = "policy_no", length = 60, updatable = false) String policyNo,
    @Column(name = "client_code", nullable = false, length = 30, updatable = false)
        String clientCode,
    @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
        String assuredName,
    @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
        String insurerCode,
    @Column(nullable = false, length = 3, updatable = false) String currency,
    @Column(length = 40, updatable = false) String segment,
    @Column(name = "ao_username", length = 50, updatable = false) String aoUsername,
    @Column(name = "product_line", length = 30, updatable = false) String productLine) {}
