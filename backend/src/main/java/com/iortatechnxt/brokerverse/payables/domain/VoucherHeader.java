package com.iortatechnxt.brokerverse.payables.domain;

import java.time.LocalDate;

/**
 * Header values of a payment voucher.
 *
 * @param companyId company
 * @param branchId paying branch
 * @param partyId payee party id
 * @param partyCode payee party code
 * @param payeeName name printed on the cheque / transfer
 * @param category what is paid (decides the accounting event)
 * @param mode payment mode
 * @param bankAccountId paying bank account
 * @param voucherDate payment (accounting) date
 * @param chequeDate cheque date for PDCs (defaults to the voucher date for other modes)
 * @param currency payment currency
 * @param department department dimension (analysis for PDC and voucher reports)
 * @param narration narration
 */
public record VoucherHeader(
    Long companyId,
    Long branchId,
    Long partyId,
    String partyCode,
    String payeeName,
    PaymentCategory category,
    PaymentMode mode,
    Long bankAccountId,
    LocalDate voucherDate,
    LocalDate chequeDate,
    String currency,
    String department,
    String narration) {}
