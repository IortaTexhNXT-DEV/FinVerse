package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptAllocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Receipt with its allocations (detail and print view).
 *
 * @param summary list fields
 * @param department department
 * @param draweeBank payer's bank
 * @param exchangeRate rate to base currency
 * @param baseAmount amount in base currency
 * @param incomeAccountCode income account (other receipts)
 * @param allocationMethod allocation method
 * @param narration narration
 * @param approvedAt approval time
 * @param journalBatchNo journal of the approval
 * @param reversalDate cancellation / bounce / rejection date
 * @param reversalReason reason
 * @param reversedBy user
 * @param depositSlipId deposit slip
 * @param pdcId post-dated cheque behind the receipt
 * @param allocations allocations to debit notes
 */
public record ReceiptResponse(
    ReceiptSummaryResponse summary,
    String department,
    String draweeBank,
    BigDecimal exchangeRate,
    BigDecimal baseAmount,
    String incomeAccountCode,
    AllocationMethod allocationMethod,
    String narration,
    Instant approvedAt,
    String journalBatchNo,
    LocalDate reversalDate,
    String reversalReason,
    String reversedBy,
    Long depositSlipId,
    Long pdcId,
    List<Allocation> allocations) {

  /**
   * Maps an entity whose allocations are loaded.
   *
   * @param r receipt
   * @return response
   */
  public static ReceiptResponse from(Receipt r) {
    return new ReceiptResponse(
        ReceiptSummaryResponse.from(r),
        r.getDepartment(),
        r.getDraweeBank(),
        r.getExchangeRate(),
        r.getBaseAmount(),
        r.getIncomeAccountCode(),
        r.getAllocationMethod(),
        r.getNarration(),
        r.getApprovedAt(),
        r.getJournalBatchNo(),
        r.getReversalDate(),
        r.getReversalReason(),
        r.getReversedBy(),
        r.getDepositSlipId(),
        r.getPdcId(),
        r.getAllocations().stream().map(Allocation::from).toList());
  }

  /**
   * Allocation of the receipt to a debit note.
   *
   * @param id id
   * @param debitItemId debit open item
   * @param documentNo debit note number
   * @param amount amount applied
   * @param matched whether the sub-ledger match exists
   * @param appliedOn application date
   */
  public record Allocation(
      Long id,
      Long debitItemId,
      String documentNo,
      BigDecimal amount,
      boolean matched,
      LocalDate appliedOn) {

    static Allocation from(ReceiptAllocation a) {
      return new Allocation(
          a.getId(),
          a.getDebitItemId(),
          a.getDocumentNo(),
          a.getAmount(),
          a.getMatchId() != null,
          a.getAppliedOn());
    }
  }
}
