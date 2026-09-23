package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.payables.domain.PettyCashDisbursement;
import com.iortatechnxt.finverse.payables.domain.PettyCashStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petty cash disbursement voucher view.
 *
 * @param id id
 * @param fundId fund
 * @param documentNo document number
 * @param date disbursement date
 * @param payee payee
 * @param expenseAccountCode expense account
 * @param costCenter cost centre
 * @param description description
 * @param receiptRef receipt reference
 * @param amount amount
 * @param status status
 * @param statusReason rejection reason
 * @param createdBy maker
 * @param approvedBy checker
 * @param journalBatchNo journal
 * @param reimbursementId reimbursement claim
 */
public record DisbursementResponse(
    Long id,
    Long fundId,
    String documentNo,
    LocalDate date,
    String payee,
    String expenseAccountCode,
    String costCenter,
    String description,
    String receiptRef,
    BigDecimal amount,
    PettyCashStatus status,
    String statusReason,
    String createdBy,
    String approvedBy,
    String journalBatchNo,
    Long reimbursementId) {

  /**
   * Maps an entity.
   *
   * @param d voucher
   * @return response
   */
  public static DisbursementResponse from(PettyCashDisbursement d) {
    return new DisbursementResponse(
        d.getId(),
        d.getFundId(),
        d.getDocumentNo(),
        d.getDisbursementDate(),
        d.getPayee(),
        d.getExpenseAccountCode(),
        d.getCostCenter(),
        d.getDescription(),
        d.getReceiptRef(),
        d.getAmount(),
        d.getStatus(),
        d.getStatusReason(),
        d.getCreatedBy(),
        d.getApprovedBy(),
        d.getJournalBatchNo(),
        d.getReimbursementId());
  }
}
