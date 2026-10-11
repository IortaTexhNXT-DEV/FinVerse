package com.iortatechnxt.brokerverse.payables.domain;

import java.math.BigDecimal;

/**
 * Result of posting an approved payment voucher.
 *
 * @param chequeNo cheque number allocated (null for transfers)
 * @param journalBatchNo GL journal batch
 * @param openItemId DEBIT open item recorded for the payment
 * @param baseAmount payment amount in base currency
 */
public record VoucherPosting(
    String chequeNo, String journalBatchNo, Long openItemId, BigDecimal baseAmount) {}
