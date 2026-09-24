package com.iortatechnxt.brokerverse.tax.domain;

import java.math.BigDecimal;

/**
 * Accounting result of a remittance. Invariant: payableCleared = creditApplied + amount.
 *
 * @param payableCleared tax payable debited (e.g. output VAT of the quarter)
 * @param creditApplied credits credited (e.g. input VAT and carried-over excess)
 * @param amount cash paid from the bank
 * @param batchNo journal batch, null when nothing was posted (zero return)
 */
public record RemittancePosting(
    BigDecimal payableCleared, BigDecimal creditApplied, BigDecimal amount, String batchNo) {}
