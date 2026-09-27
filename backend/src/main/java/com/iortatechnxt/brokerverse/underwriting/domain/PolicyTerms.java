package com.iortatechnxt.brokerverse.underwriting.domain;

import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Header terms of a policy (everything except risks and computed premium).
 *
 * @param branchId issuing branch
 * @param product product
 * @param customer client (policyholder party, billed on the debit note)
 * @param insuredName name of the insured as printed on the policy
 * @param sourceType distribution channel
 * @param intermediary agent or broker (null for direct business)
 * @param issueDate issue date
 * @param periodFrom cover start
 * @param periodTo cover end
 * @param currency policy currency
 * @param businessType direct or with coinsurance
 * @param sharePct company share % (100 for direct business)
 * @param coinsurer coinsurer party (coinsured business only)
 * @param coinsuranceLeader whether the company leads (and bills 100 %)
 * @param discountRate discount % of gross premium
 * @param loadingRate loading % of gross premium
 */
public record PolicyTerms(
    Long branchId,
    Product product,
    Party customer,
    String insuredName,
    SourceType sourceType,
    Party intermediary,
    LocalDate issueDate,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BusinessType businessType,
    BigDecimal sharePct,
    Party coinsurer,
    boolean coinsuranceLeader,
    BigDecimal discountRate,
    BigDecimal loadingRate) {}
