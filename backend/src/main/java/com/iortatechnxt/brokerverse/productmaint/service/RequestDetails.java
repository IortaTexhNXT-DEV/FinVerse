package com.iortatechnxt.brokerverse.productmaint.service;

import java.math.BigDecimal;

/**
 * The fields of BDOI's Annex E (Package Request Fields) that the request form does not hold
 * elsewhere: business origin, Account Officer and Unit Head, Insured's Name, the estimated number
 * of policies and total basic premium, the cover wording, premium, and the incentive.
 *
 * @param businessOrigin business origin (list PKG_BUSINESS_ORIGIN)
 * @param accountOfficer Account Officer (user ID)
 * @param unitHead Unit Head (user ID)
 * @param insuredName Insured's Name
 * @param estimatedPolicies estimated number of policies to be issued (at least PKG_MIN_POLICIES)
 * @param estimatedPremium estimated total basic premium (at least PKG_MIN_PREMIUM)
 * @param typeOfCover type of cover (Property: All-Risk, F/L Only, Named Perils)
 * @param descriptionOfCover description of cover
 * @param cover cover or undertaking
 * @param extensions extensions of cover
 * @param warranties warranties and clauses
 * @param otherInstructions other instructions
 * @param maximumLimits maximum limits
 * @param premium premium
 * @param incentiveEligible Incentive Eligible (default No)
 * @param incentiveAmount incentive amount (greater than zero)
 * @param incentiveRate incentive commission rate (above 0 % up to 100 %)
 */
public record RequestDetails(
    String businessOrigin,
    String accountOfficer,
    String unitHead,
    String insuredName,
    Integer estimatedPolicies,
    BigDecimal estimatedPremium,
    String typeOfCover,
    String descriptionOfCover,
    String cover,
    String extensions,
    String warranties,
    String otherInstructions,
    String maximumLimits,
    BigDecimal premium,
    Boolean incentiveEligible,
    BigDecimal incentiveAmount,
    BigDecimal incentiveRate) {}
