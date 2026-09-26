package com.iortatechnxt.brokerverse.underwriting.domain;

import java.math.BigDecimal;

/**
 * Inputs of the premium computation. Amounts are at 100 % (whole risk); negative gross premium
 * means return premium (refund, cancellation).
 *
 * @param sumInsured sum insured at 100 %
 * @param grossPremium gross premium at 100 %
 * @param discountRate discount % of gross
 * @param loadingRate loading % of gross
 * @param sharePct company share % (100 when not coinsured)
 * @param coinsuranceLeader true when the company leads the coinsurance and bills the client 100 %
 * @param taxes product tax rates, applied on the company's net premium
 * @param policyFee flat policy fee (new and renewed policies only)
 * @param commissionRate intermediary commission % of the company's net premium
 * @param withholdingRate withholding tax % on commission
 */
public record PremiumInput(
    BigDecimal sumInsured,
    BigDecimal grossPremium,
    BigDecimal discountRate,
    BigDecimal loadingRate,
    BigDecimal sharePct,
    boolean coinsuranceLeader,
    TaxRates taxes,
    BigDecimal policyFee,
    BigDecimal commissionRate,
    BigDecimal withholdingRate) {}
