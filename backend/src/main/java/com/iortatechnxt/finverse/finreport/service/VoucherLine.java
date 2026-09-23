package com.iortatechnxt.finverse.finreport.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One journal (voucher) line with its header, read for voucher listings, day books and prints.
 *
 * @param batchId journal id
 * @param batchNo voucher number
 * @param journalType journal type name (transaction code source)
 * @param status journal status name
 * @param valueDate document date
 * @param narration voucher narration
 * @param reference voucher reference
 * @param sourceModule originating module or null for manual journals
 * @param createdBy entered by
 * @param createdAt entered at
 * @param submittedBy submitted by
 * @param authorizedBy authorised / approved by
 * @param authorizedAt authorised at
 * @param lineNo line number
 * @param accountId account
 * @param branchId line branch (division)
 * @param debit whether the line is a debit
 * @param currency line currency
 * @param amount amount in line currency
 * @param baseAmount amount in base currency
 * @param costCenter cost centre (department)
 * @param businessLine line of business (activity)
 * @param partyCode sub-ledger party
 * @param lineReference line reference
 * @param lineNarration line narration
 */
public record VoucherLine(
    Long batchId,
    String batchNo,
    String journalType,
    String status,
    LocalDate valueDate,
    String narration,
    String reference,
    String sourceModule,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    String authorizedBy,
    Instant authorizedAt,
    int lineNo,
    Long accountId,
    Long branchId,
    boolean debit,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String costCenter,
    String businessLine,
    String partyCode,
    String lineReference,
    String lineNarration) {

  /**
   * Base amount on the debit side.
   *
   * @return base amount or zero
   */
  public BigDecimal debitBase() {
    return debit ? baseAmount : BigDecimal.ZERO;
  }

  /**
   * Base amount on the credit side.
   *
   * @return base amount or zero
   */
  public BigDecimal creditBase() {
    return debit ? BigDecimal.ZERO : baseAmount;
  }

  /**
   * Transaction code: the voucher number prefix (e.g. "JV" of JV-HO-2026-000001).
   *
   * @return transaction code
   */
  public String transactionCode() {
    int dash = batchNo.indexOf('-');
    return dash > 0 ? batchNo.substring(0, dash) : batchNo;
  }
}
