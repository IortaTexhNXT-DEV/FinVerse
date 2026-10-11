package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * An item insured at a location of risk (building, contents, stocks...) with its sum insured.
 *
 * @param description item
 * @param sumInsured sum insured
 */
@Embeddable
public record InsuredItem(
    @Column(name = "description", nullable = false, length = 200) String description,
    @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
        BigDecimal sumInsured) {}
