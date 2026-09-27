package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * An insurer's share in an invoice (co-insurance, ADJID.027); 100% for a single insurer.
 *
 * @param insurerCode insurer party code
 * @param sharePct share in percent
 * @param lead whether the insurer leads (the invoice's insurer)
 */
@Embeddable
public record OpsInvoiceShare(
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Column(name = "share_pct", nullable = false, precision = 9, scale = 4) BigDecimal sharePct,
    @Column(name = "lead", nullable = false) boolean lead) {}
