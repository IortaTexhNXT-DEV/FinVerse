package com.iortatechnxt.brokerverse.reinsurance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Editable terms of a treaty.
 *
 * @param name treaty name
 * @param treatyType quota share, surplus or excess of loss
 * @param businessLine line of business covered
 * @param uwYear underwriting year of the programme
 * @param periodFrom treaty period start
 * @param periodTo treaty period end
 * @param currency statement currency (the company base currency)
 * @param quotaSharePct quota share % (quota share only)
 * @param treatyLimit maximum sum insured per risk of the quota share layer, null = unlimited
 * @param retentionLimit one line: the company's gross retention per risk (surplus only)
 * @param lines number of lines of the surplus treaty
 * @param levyPct premium tax / levy withheld on premium ceded, %
 * @param reserveInterestPct yearly interest paid on reserves withheld, %
 * @param lossReservePct share of the reinsurers' outstanding losses withheld as loss reserve, %
 */
public record TreatyTerms(
    String name,
    TreatyType treatyType,
    String businessLine,
    int uwYear,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal quotaSharePct,
    BigDecimal treatyLimit,
    BigDecimal retentionLimit,
    Integer lines,
    BigDecimal levyPct,
    BigDecimal reserveInterestPct,
    BigDecimal lossReservePct) {}
