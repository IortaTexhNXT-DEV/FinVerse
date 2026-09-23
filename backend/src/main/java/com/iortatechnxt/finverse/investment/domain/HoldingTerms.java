package com.iortatechnxt.finverse.investment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contractual terms of a holding.
 *
 * @param faceValue face (par) value; number of shares x par for equities
 * @param purchasePrice clean consideration paid (excluding purchased accrued interest)
 * @param purchasedInterest accrued interest bought with the security
 * @param tradeDate trade date
 * @param settlementDate settlement (value) date
 * @param maturityDate maturity date (null for equities)
 * @param couponRate coupon rate, % per annum
 * @param couponFrequency coupon frequency
 * @param dayCount day count convention
 * @param amortizationMethod premium / discount amortization method
 */
public record HoldingTerms(
    BigDecimal faceValue,
    BigDecimal purchasePrice,
    BigDecimal purchasedInterest,
    LocalDate tradeDate,
    LocalDate settlementDate,
    LocalDate maturityDate,
    BigDecimal couponRate,
    CouponFrequency couponFrequency,
    DayCountConvention dayCount,
    AmortizationMethod amortizationMethod) {}
