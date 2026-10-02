package com.iortatechnxt.brokerverse.ledger.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account statement line with running balance.
 *
 * @param valueDate value date
 * @param batchId journal batch id (drill-down)
 * @param batchNo batch number
 * @param journalType journal type
 * @param narration narration
 * @param reference reference
 * @param currency currency
 * @param amountFc signed transaction currency amount (debit positive)
 * @param debit debit (base)
 * @param credit credit (base)
 * @param runningBalance running balance (base, debit positive)
 */
public record StatementLineResponse(
    LocalDate valueDate,
    Long batchId,
    String batchNo,
    String journalType,
    String narration,
    String reference,
    String currency,
    BigDecimal amountFc,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal runningBalance) {}
