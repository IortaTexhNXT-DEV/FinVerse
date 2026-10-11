package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Header fields of a request form (Appendix D): segment, reference, requesting unit, RFP type,
 * purpose and currency.
 *
 * @param segment market segment
 * @param referenceText reference written on the form (e.g. "2025_331 Refund")
 * @param requestingUnit requesting unit
 * @param rfpType type of a request for payment (LOV {@code PRQ_RFP_TYPE}), cash advances only
 * @param purpose purpose or remarks
 * @param currency currency
 */
@Embeddable
public record RequestContent(
    @Column(length = 40) String segment,
    @Column(name = "reference_text", length = 80) String referenceText,
    @Column(name = "requesting_unit", length = 60) String requestingUnit,
    @Column(name = "rfp_type", length = 30) String rfpType,
    @Column(nullable = false, length = 500) String purpose,
    @Column(nullable = false, length = 3) String currency) {}
