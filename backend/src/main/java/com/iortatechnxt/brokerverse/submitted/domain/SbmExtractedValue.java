package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * A proposed field value of an extraction with its confidence (BRIDSP-02).
 *
 * @param value value found
 * @param confidence 0 to 1, null when entered by hand
 */
@Embeddable
public record SbmExtractedValue(
    @Column(name = "value", length = 500) String value,
    @Column(name = "confidence", precision = 5, scale = 2) BigDecimal confidence) {}
