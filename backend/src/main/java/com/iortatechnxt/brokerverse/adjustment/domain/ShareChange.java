package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Recompute of one insurer share (ADJID.014/027): premium, commission and VAT changes; a negative
 * premium change is the insurer's refund premium.
 *
 * @param insurerCode insurer
 * @param sharePct share in percent
 * @param lead lead insurer
 * @param premiumDelta gross premium change
 * @param commissionDelta commission change
 * @param vatDelta VAT on commission change
 */
@Embeddable
public record ShareChange(
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Column(name = "share_pct", nullable = false, precision = 9, scale = 4) BigDecimal sharePct,
    @Column(nullable = false) boolean lead,
    @Column(name = "premium_delta", nullable = false, precision = 19, scale = 2)
        BigDecimal premiumDelta,
    @Column(name = "commission_delta", nullable = false, precision = 19, scale = 2)
        BigDecimal commissionDelta,
    @Column(name = "vat_delta", nullable = false, precision = 19, scale = 2) BigDecimal vatDelta) {}
