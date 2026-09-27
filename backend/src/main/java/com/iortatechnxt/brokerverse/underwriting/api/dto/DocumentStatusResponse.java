package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.underwriting.domain.ApprovalWorkflow;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.PostingRefs;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Workflow and accounting references of a policy or endorsement.
 *
 * @param status status
 * @param createdBy maker
 * @param createdAt creation time
 * @param submittedBy submitter
 * @param submittedAt submission time
 * @param approvedBy approver
 * @param approvedAt approval time
 * @param approvalDate accounting date
 * @param rejectionReason reason of the last rejection
 * @param debitNoteNo client debit (or credit) note
 * @param creditNoteNo intermediary credit (or debit) note
 * @param premiumBatchNo premium journal
 * @param commissionBatchNo commission journal
 * @param exchangeRate rate to base currency
 */
public record DocumentStatusResponse(
    PolicyStatus status,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    String approvedBy,
    Instant approvedAt,
    LocalDate approvalDate,
    String rejectionReason,
    String debitNoteNo,
    String creditNoteNo,
    String premiumBatchNo,
    String commissionBatchNo,
    BigDecimal exchangeRate) {

  /**
   * Maps workflow and references.
   *
   * @param w workflow
   * @param r references
   * @param createdBy maker
   * @param createdAt creation time
   * @return response
   */
  public static DocumentStatusResponse of(
      ApprovalWorkflow w, PostingRefs r, String createdBy, Instant createdAt) {
    return new DocumentStatusResponse(
        w.getStatus(),
        createdBy,
        createdAt,
        w.getSubmittedBy(),
        w.getSubmittedAt(),
        w.getApprovedBy(),
        w.getApprovedAt(),
        w.getApprovalDate(),
        w.getRejectionReason(),
        r.getDebitNoteNo(),
        r.getCreditNoteNo(),
        r.getPremiumBatchNo(),
        r.getCommissionBatchNo(),
        r.getExchangeRate());
  }
}
