package com.iortatechnxt.brokerverse.finreport.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One posted ledger entry, read for ledgers and activity analysis.
 *
 * @param accountId account
 * @param branchId branch (division)
 * @param valueDate document date
 * @param batchNo voucher number
 * @param journalType journal type name
 * @param currency transaction currency
 * @param debitFc debit in transaction currency
 * @param creditFc credit in transaction currency
 * @param debitBase debit in base currency
 * @param creditBase credit in base currency
 * @param costCenter cost centre (department)
 * @param businessLine line of business (activity)
 * @param partyCode sub-ledger party
 * @param reference reference
 * @param narration narration
 */
public record LedgerLine(
    Long accountId,
    Long branchId,
    LocalDate valueDate,
    String batchNo,
    String journalType,
    String currency,
    BigDecimal debitFc,
    BigDecimal creditFc,
    BigDecimal debitBase,
    BigDecimal creditBase,
    String costCenter,
    String businessLine,
    String partyCode,
    String reference,
    String narration) {

  /**
   * Transaction code (voucher number prefix).
   *
   * @return transaction code
   */
  public String transactionCode() {
    int dash = batchNo.indexOf('-');
    return dash > 0 ? batchNo.substring(0, dash) : batchNo;
  }
}
