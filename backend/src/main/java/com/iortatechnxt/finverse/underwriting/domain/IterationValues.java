package com.iortatechnxt.finverse.underwriting.domain;

import java.math.BigDecimal;

/**
 * Figures of one quotation iteration, at 100 %.
 *
 * @param sumInsured sum insured
 * @param grossPremium gross premium
 * @param discount discount amount
 * @param loading loading amount
 * @param charges other charges (taxes, fees) quoted
 * @param remarks negotiation remarks
 */
public record IterationValues(
    BigDecimal sumInsured,
    BigDecimal grossPremium,
    BigDecimal discount,
    BigDecimal loading,
    BigDecimal charges,
    String remarks) {}
