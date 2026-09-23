package com.iortatechnxt.finverse.receivables.api.dto;

import com.iortatechnxt.finverse.receivables.domain.DepositStatus;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Receipt list row.
 *
 * @param id id
 * @param receiptNo receipt number
 * @param receiptDate receipt date
 * @param branchId branch
 * @param payerType payer type
 * @param partyCode party
 * @param payerName payer name
 * @param mode instrument
 * @param instrumentNo cheque / reference number
 * @param instrumentDate cheque date
 * @param currency currency
 * @param amount amount
 * @param appliedAmount applied to debit notes
 * @param unappliedAmount on account
 * @param bankAccountCode bank GL account
 * @param status status
 * @param depositStatus deposit status
 * @param depositedOn deposit date
 * @param createdBy maker
 * @param approvedBy checker
 */
public record ReceiptSummaryResponse(
    Long id,
    String receiptNo,
    LocalDate receiptDate,
    Long branchId,
    PayerType payerType,
    String partyCode,
    String payerName,
    ReceiptMode mode,
    String instrumentNo,
    LocalDate instrumentDate,
    String currency,
    BigDecimal amount,
    BigDecimal appliedAmount,
    BigDecimal unappliedAmount,
    String bankAccountCode,
    ReceiptStatus status,
    DepositStatus depositStatus,
    LocalDate depositedOn,
    String createdBy,
    String approvedBy) {

  /**
   * Maps an entity.
   *
   * @param r receipt
   * @return row
   */
  public static ReceiptSummaryResponse from(Receipt r) {
    return new ReceiptSummaryResponse(
        r.getId(),
        r.getReceiptNo(),
        r.getReceiptDate(),
        r.getBranchId(),
        r.getPayerType(),
        r.getPartyCode(),
        r.getPayerName(),
        r.getMode(),
        r.getInstrumentNo(),
        r.getInstrumentDate(),
        r.getCurrency(),
        r.getAmount(),
        r.getAppliedAmount(),
        r.unapplied(),
        r.getBankAccountCode(),
        r.getStatus(),
        r.getDepositStatus(),
        r.getDepositedOn(),
        r.getCreatedBy(),
        r.getApprovedBy());
  }
}
