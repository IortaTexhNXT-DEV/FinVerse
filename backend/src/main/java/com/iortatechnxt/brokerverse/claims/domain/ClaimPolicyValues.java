package com.iortatechnxt.brokerverse.claims.domain;

import java.math.BigDecimal;

/**
 * Values of the policy facts of a new claim (see {@link ClaimPolicy}).
 *
 * @param policyId policy id
 * @param policyNo policy number
 * @param productCode product code
 * @param productName product name
 * @param businessLine line of business (class)
 * @param customerCode policyholder code
 * @param customerName policyholder name
 * @param insuredName insured name
 * @param intermediaryCode agent / broker code, null for direct business
 * @param uwYear underwriting year
 * @param sharePct company share %
 * @param coinsuranceLeader whether the company leads the coinsurance
 * @param coinsurerCode coinsurer code, null when not coinsured
 * @param riskId insured risk, null when not specified
 * @param riskDescription risk description
 * @param sumInsured sum insured of the risk (or policy) at 100 %
 */
public record ClaimPolicyValues(
    Long policyId,
    String policyNo,
    String productCode,
    String productName,
    String businessLine,
    String customerCode,
    String customerName,
    String insuredName,
    String intermediaryCode,
    int uwYear,
    BigDecimal sharePct,
    boolean coinsuranceLeader,
    String coinsurerCode,
    Long riskId,
    String riskDescription,
    BigDecimal sumInsured) {}
