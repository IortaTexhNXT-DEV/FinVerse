package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reimbursement claim view.
 *
 * @param id id
 * @param fundId fund
 * @param documentNo document number
 * @param claimDate claim date
 * @param bankAccountId bank account
 * @param amount amount
 * @param narration narration
 * @param status status
 * @param statusReason rejection reason
 * @param createdBy maker
 * @param approvedBy checker
 * @param journalBatchNo journal
 */
public record ReimbursementResponse(
    Long id,
    Long fundId,
    String documentNo,
    LocalDate claimDate,
    Long bankAccountId,
    BigDecimal amount,
    String narration,
    PettyCashStatus status,
    String statusReason,
    String createdBy,
    String approvedBy,
    String journalBatchNo) {

  /**
   * Maps an entity.
   *
   * @param r claim
   * @return response
   */
  public static ReimbursementResponse from(PettyCashReimbursement r) {
    return new ReimbursementResponse(
        r.getId(),
        r.getFundId(),
        r.getDocumentNo(),
        r.getClaimDate(),
        r.getBankAccountId(),
        r.getAmount(),
        r.getNarration(),
        r.getStatus(),
        r.getStatusReason(),
        r.getCreatedBy(),
        r.getApprovedBy(),
        r.getJournalBatchNo());
  }
}
