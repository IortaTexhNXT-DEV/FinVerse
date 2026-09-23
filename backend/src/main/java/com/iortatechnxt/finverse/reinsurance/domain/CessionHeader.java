package com.iortatechnxt.finverse.reinsurance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Identification and totals of the premium transaction a cession allocates.
 *
 * @param companyId company
 * @param branchId branch of the policy
 * @param policyId policy id
 * @param policyNo policy number
 * @param endorsementNo 0 for the original issue, else the endorsement number
 * @param documentNo policy or endorsement document number
 * @param kind NEW or the endorsement type
 * @param businessLine line of business
 * @param productCode product code
 * @param uwYear underwriting year of the policy
 * @param treatyYear underwriting year of the treaty programme applied
 * @param issueDate document issue date
 * @param effectiveDate cover start or endorsement effective date
 * @param riDate reinsurance accounting date (approval date of the transaction)
 * @param currency policy currency
 * @param exchangeRate rate to the base currency
 * @param sharePct company share of the policy (coinsurance), %
 * @param ourSi company sum insured of the transaction
 * @param ourPremium company net premium of the transaction
 */
public record CessionHeader(
    Long companyId,
    Long branchId,
    Long policyId,
    String policyNo,
    int endorsementNo,
    String documentNo,
    String kind,
    String businessLine,
    String productCode,
    int uwYear,
    int treatyYear,
    LocalDate issueDate,
    LocalDate effectiveDate,
    LocalDate riDate,
    String currency,
    BigDecimal exchangeRate,
    BigDecimal sharePct,
    BigDecimal ourSi,
    BigDecimal ourPremium) {}
