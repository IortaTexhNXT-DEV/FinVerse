package com.iortatechnxt.brokerverse.receivables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a post-dated cheque received.
 *
 * @param companyId company
 * @param branchId receiving branch (division)
 * @param pdcNo register number
 * @param receivedDate date the cheque was received
 * @param partyId payer party
 * @param partyCode payer party code
 * @param payerName payer name
 * @param department department dimension code
 * @param chequeNo cheque number
 * @param chequeDate cheque (due) date
 * @param draweeBank payer's bank
 * @param currency currency
 * @param exchangeRate rate to base currency on the received date
 * @param amount amount in currency
 * @param baseAmount amount in base currency
 * @param bankAccountCode GL bank account the cheque will be deposited in
 * @param debitItemId linked debit note (optional)
 * @param narration narration
 */
public record PdcValues(
    Long companyId,
    Long branchId,
    String pdcNo,
    LocalDate receivedDate,
    Long partyId,
    String partyCode,
    String payerName,
    String department,
    String chequeNo,
    LocalDate chequeDate,
    String draweeBank,
    String currency,
    BigDecimal exchangeRate,
    BigDecimal amount,
    BigDecimal baseAmount,
    String bankAccountCode,
    Long debitItemId,
    String narration) {}
