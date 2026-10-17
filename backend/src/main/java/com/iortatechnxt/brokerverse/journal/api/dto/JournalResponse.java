package com.iortatechnxt.brokerverse.journal.api.dto;

import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Journal batch view (header, audit fields and optionally lines).
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param batchNo batch number
 * @param journalType type
 * @param status status
 * @param transactionDate entry date
 * @param valueDate value date
 * @param currency currency
 * @param narration narration
 * @param reference reference
 * @param sourceModule source module
 * @param sourceReference source reference
 * @param reversalOfId reversed batch
 * @param reversedById reversing batch
 * @param totalDebit total debit (base)
 * @param totalCredit total credit (base)
 * @param createdBy inputter
 * @param createdAt input time
 * @param submittedBy submitter
 * @param submittedAt submission time
 * @param authorizedBy authorizer
 * @param authorizedAt authorization time
 * @param rejectedBy rejecting user
 * @param rejectionReason rejection reason
 * @param postedAt posting time
 * @param lines lines (empty in list views)
 * @param assignedTo user the entry is assigned to for posting (FRBS 2.5.1)
 * @param assignedBy assigning user
 * @param reverseOn automatic reversal date (FRBS 2.8.1)
 * @param correctsBatchId journal corrected by this one (ACSL 2.9.1)
 * @param relatedInvoiceNo related invoice
 * @param rootInvoiceNo root of the invoice family
 */
public record JournalResponse(
    Long id,
    Long companyId,
    Long branchId,
    String batchNo,
    JournalType journalType,
    JournalStatus status,
    LocalDate transactionDate,
    LocalDate valueDate,
    String currency,
    String narration,
    String reference,
    String sourceModule,
    String sourceReference,
    Long reversalOfId,
    Long reversedById,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    String authorizedBy,
    Instant authorizedAt,
    String rejectedBy,
    String rejectionReason,
    Instant postedAt,
    List<JournalLineResponse> lines,
    String assignedTo,
    String assignedBy,
    LocalDate reverseOn,
    Long correctsBatchId,
    String relatedInvoiceNo,
    String rootInvoiceNo) {

  /**
   * Maps an entity including lines.
   *
   * @param b batch
   * @return response
   */
  public static JournalResponse withLines(JournalBatch b) {
    return map(b, b.getLines().stream().map(JournalLineResponse::from).toList());
  }

  /**
   * Maps an entity without lines (list views).
   *
   * @param b batch
   * @return response
   */
  public static JournalResponse summary(JournalBatch b) {
    return map(b, List.of());
  }

  private static JournalResponse map(JournalBatch b, List<JournalLineResponse> lines) {
    return new JournalResponse(
        b.getId(),
        b.getCompanyId(),
        b.getBranchId(),
        b.getBatchNo(),
        b.getJournalType(),
        b.getStatus(),
        b.getTransactionDate(),
        b.getValueDate(),
        b.getCurrency(),
        b.getNarration(),
        b.getReference(),
        b.getSourceModule(),
        b.getSourceReference(),
        b.getReversalOfId(),
        b.getReversedById(),
        b.getTotalDebit(),
        b.getTotalCredit(),
        b.getCreatedBy(),
        b.getCreatedAt(),
        b.getSubmittedBy(),
        b.getSubmittedAt(),
        b.getAuthorizedBy(),
        b.getAuthorizedAt(),
        b.getRejectedBy(),
        b.getRejectionReason(),
        b.getPostedAt(),
        lines,
        b.getAssignedTo(),
        b.getAssignedBy(),
        b.getReverseOn(),
        b.getCorrectsBatchId(),
        b.getRelatedInvoiceNo(),
        b.getRootInvoiceNo());
  }
}
