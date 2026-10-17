package com.iortatechnxt.brokerverse.journal.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import java.math.BigDecimal;

/**
 * Resolved values of a journal line (accounts looked up, amounts converted).
 *
 * @param account GL account
 * @param branchId posting branch of the line
 * @param side debit or credit
 * @param currency line currency
 * @param amount amount in line currency (positive)
 * @param exchangeRate rate to base
 * @param baseAmount base currency amount (positive)
 * @param costCenter cost centre code
 * @param businessLine line of business code
 * @param partyCode sub-ledger party (policyholder, intermediary, reinsurer...)
 * @param reference transaction reference (policy, claim, cheque no...)
 * @param narration line narration
 */
public record JournalLineSpec(
    GlAccount account,
    Long branchId,
    BalanceSide side,
    String currency,
    BigDecimal amount,
    BigDecimal exchangeRate,
    BigDecimal baseAmount,
    String costCenter,
    String businessLine,
    String partyCode,
    String reference,
    String narration) {}
