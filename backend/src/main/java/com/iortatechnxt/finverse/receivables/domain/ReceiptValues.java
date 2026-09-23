package com.iortatechnxt.finverse.receivables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a new official receipt.
 *
 * @param companyId company
 * @param branchId collecting branch (division)
 * @param receiptNo receipt number
 * @param receiptDate receipt (value) date
 * @param payerType payer type
 * @param partyId sub-ledger party (null for OTHER)
 * @param partyCode party code (null for OTHER)
 * @param payerName payer name
 * @param department department dimension code
 * @param mode payment instrument
 * @param instrumentNo cheque / transfer / card reference
 * @param instrumentDate cheque date
 * @param draweeBank payer's bank
 * @param currency currency
 * @param exchangeRate rate to base currency
 * @param amount amount in currency
 * @param baseAmount amount in base currency
 * @param bankAccountCode GL bank account receiving the money
 * @param incomeAccountCode income account (OTHER receipts)
 * @param allocationMethod allocation method
 * @param narration narration
 * @param pdcId post-dated cheque converted into this receipt
 */
public record ReceiptValues(
    Long companyId,
    Long branchId,
    String receiptNo,
    LocalDate receiptDate,
    PayerType payerType,
    Long partyId,
    String partyCode,
    String payerName,
    String department,
    ReceiptMode mode,
    String instrumentNo,
    LocalDate instrumentDate,
    String draweeBank,
    String currency,
    BigDecimal exchangeRate,
    BigDecimal amount,
    BigDecimal baseAmount,
    String bankAccountCode,
    String incomeAccountCode,
    AllocationMethod allocationMethod,
    String narration,
    Long pdcId) {

  /**
   * Returns a copy with the allocated receipt number, the exchange rate and the base amount.
   *
   * @param no receipt number
   * @param rate exchange rate to base currency
   * @param base amount in base currency
   * @return completed values
   */
  public ReceiptValues numbered(String no, BigDecimal rate, BigDecimal base) {
    return new ReceiptValues(
        companyId,
        branchId,
        no,
        receiptDate,
        payerType,
        partyId,
        partyCode,
        payerName,
        department,
        mode,
        instrumentNo,
        instrumentDate,
        draweeBank,
        currency,
        rate,
        amount,
        base,
        bankAccountCode,
        incomeAccountCode,
        allocationMethod,
        narration,
        pdcId);
  }
}
