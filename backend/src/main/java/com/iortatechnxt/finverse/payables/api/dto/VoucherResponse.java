package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.payables.domain.PaymentCategory;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.VoucherAllocation;
import com.iortatechnxt.finverse.payables.domain.VoucherStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Payment voucher view ({@code allocations} empty in list results).
 *
 * @param id id
 * @param voucherNo voucher number
 * @param companyId company
 * @param branchId branch
 * @param partyCode payee
 * @param payeeName payee name
 * @param category category
 * @param paymentMode mode
 * @param bankAccountId bank account
 * @param voucherDate payment date
 * @param chequeNo cheque number
 * @param chequeDate cheque date
 * @param currency currency
 * @param amount amount
 * @param baseAmount base currency amount
 * @param department department
 * @param narration narration
 * @param status status
 * @param statusReason last rejection / cancellation / void reason
 * @param createdBy maker
 * @param submittedBy submitter
 * @param approvedBy checker
 * @param approvedAt approval time
 * @param journalBatchNo GL journal
 * @param presentedOn cheque presentation date
 * @param voidedOn void date
 * @param voidBatchNo reversal journal
 * @param allocations items paid
 */
public record VoucherResponse(
    Long id,
    String voucherNo,
    Long companyId,
    Long branchId,
    String partyCode,
    String payeeName,
    PaymentCategory category,
    PaymentMode paymentMode,
    Long bankAccountId,
    LocalDate voucherDate,
    String chequeNo,
    LocalDate chequeDate,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String department,
    String narration,
    VoucherStatus status,
    String statusReason,
    String createdBy,
    String submittedBy,
    String approvedBy,
    Instant approvedAt,
    String journalBatchNo,
    LocalDate presentedOn,
    LocalDate voidedOn,
    String voidBatchNo,
    List<AllocationResponse> allocations) {

  /**
   * Maps a voucher with its allocations.
   *
   * @param v voucher (allocations loaded)
   * @return response
   */
  public static VoucherResponse from(PaymentVoucher v) {
    return of(v, v.getAllocations().stream().map(AllocationResponse::from).toList());
  }

  /**
   * Maps a voucher without allocations (lists).
   *
   * @param v voucher
   * @return response
   */
  public static VoucherResponse summary(PaymentVoucher v) {
    return of(v, List.of());
  }

  private static VoucherResponse of(PaymentVoucher v, List<AllocationResponse> allocations) {
    return new VoucherResponse(
        v.getId(),
        v.getVoucherNo(),
        v.getCompanyId(),
        v.getBranchId(),
        v.getPartyCode(),
        v.getPayeeName(),
        v.getCategory(),
        v.getPaymentMode(),
        v.getBankAccountId(),
        v.getVoucherDate(),
        v.getChequeNo(),
        v.getChequeDate(),
        v.getCurrency(),
        v.getAmount(),
        v.getBaseAmount(),
        v.getDepartment(),
        v.getNarration(),
        v.getStatus(),
        v.getStatusReason(),
        v.getCreatedBy(),
        v.getSubmittedBy(),
        v.getApprovedBy(),
        v.getApprovedAt(),
        v.getJournalBatchNo(),
        v.getPresentedOn(),
        v.getVoidedOn(),
        v.getVoidBatchNo(),
        allocations);
  }

  /**
   * Item paid by the voucher.
   *
   * @param openItemId open item
   * @param documentType document type
   * @param documentNo document number
   * @param documentDate document date
   * @param dueDate due date
   * @param amount amount paid
   */
  public record AllocationResponse(
      Long openItemId,
      String documentType,
      String documentNo,
      LocalDate documentDate,
      LocalDate dueDate,
      BigDecimal amount) {

    /**
     * Maps an allocation.
     *
     * @param a allocation
     * @return response
     */
    public static AllocationResponse from(VoucherAllocation a) {
      return new AllocationResponse(
          a.getOpenItemId(),
          a.getDocumentType(),
          a.getDocumentNo(),
          a.getDocumentDate(),
          a.getDueDate(),
          a.getAmount());
    }
  }
}
