package com.iortatechnxt.brokerverse.underwriting.domain;

import java.math.BigDecimal;

/**
 * Maintainable attributes of an insurance product.
 *
 * @param name product name
 * @param businessLine line of business (BUSINESS_LINE dimension code)
 * @param defaultCommissionRate default intermediary commission %
 * @param uprBasis unearned premium basis
 * @param taxes premium tax rates
 * @param policyFee flat policy fee charged on new and renewed policies
 * @param openCoverAllowed whether marine open covers (and certificates) may be written
 */
public record ProductTerms(
    String name,
    String businessLine,
    BigDecimal defaultCommissionRate,
    UprBasis uprBasis,
    TaxRates taxes,
    BigDecimal policyFee,
    boolean openCoverAllowed) {}
