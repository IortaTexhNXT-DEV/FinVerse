package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.DepositSlip;
import com.iortatechnxt.brokerverse.receivables.domain.DepositSlipStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Deposit slip.
 *
 * @param id id
 * @param slipNo slip number
 * @param slipDate slip date
 * @param branchId branch
 * @param bankAccountCode bank GL account
 * @param currency currency
 * @param totalAmount total
 * @param receiptCount number of receipts
 * @param status status
 * @param depositedOn deposit date
 * @param depositedBy confirming user
 * @param createdBy preparer
 * @param receipts receipts on the slip (detail view only)
 */
public record DepositSlipResponse(
    Long id,
    String slipNo,
    LocalDate slipDate,
    Long branchId,
    String bankAccountCode,
    String currency,
    BigDecimal totalAmount,
    int receiptCount,
    DepositSlipStatus status,
    LocalDate depositedOn,
    String depositedBy,
    String createdBy,
    List<ReceiptSummaryResponse> receipts) {

  /**
   * Maps a slip with its receipts.
   *
   * @param s slip
   * @param receipts receipts (empty for list views)
   * @return response
   */
  public static DepositSlipResponse from(DepositSlip s, List<ReceiptSummaryResponse> receipts) {
    return new DepositSlipResponse(
        s.getId(),
        s.getSlipNo(),
        s.getSlipDate(),
        s.getBranchId(),
        s.getBankAccountCode(),
        s.getCurrency(),
        s.getTotalAmount(),
        s.getReceiptCount(),
        s.getStatus(),
        s.getDepositedOn(),
        s.getDepositedBy(),
        s.getCreatedBy(),
        List.copyOf(receipts));
  }
}
