package com.iortatechnxt.brokerverse.underwriting.domain;

import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Header terms of a quotation.
 *
 * @param branchId branch
 * @param product product
 * @param customer prospective client
 * @param insuredName insured name
 * @param sourceType distribution channel
 * @param intermediary agent or broker (null for direct)
 * @param issueDate quotation date
 * @param validityDays days the offer remains valid
 * @param periodFrom proposed cover start
 * @param periodTo proposed cover end
 * @param currency currency
 * @param sharePct company share %
 * @param commissionRate brokerage / commission %
 */
public record QuotationTerms(
    Long branchId,
    Product product,
    Party customer,
    String insuredName,
    SourceType sourceType,
    Party intermediary,
    LocalDate issueDate,
    int validityDays,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal sharePct,
    BigDecimal commissionRate) {}
