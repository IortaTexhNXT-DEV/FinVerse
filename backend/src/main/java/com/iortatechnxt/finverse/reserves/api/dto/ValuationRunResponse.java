package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Valuation run header.
 *
 * @param id id
 * @param companyId company
 * @param valuationDate valuation (month-end) date
 * @param periodName valuation month (yyyy-MM)
 * @param status PREVIEW, PENDING_APPROVAL, APPROVED, POSTED or CANCELLED
 * @param baseCurrency currency of all amounts
 * @param previousRunId previous posted run the movements are measured against
 * @param calculatedAt last calculation time
 * @param remarks calculation remarks
 * @param preparedBy creator
 * @param submittedBy maker who submitted
 * @param submittedAt submission time
 * @param approvedBy checker
 * @param approvedAt approval time
 * @param rejectionReason last rejection reason
 * @param postedBy user who posted
 * @param postedAt posting time
 * @param journalCount journals posted
 * @param cancelledBy user who cancelled
 * @param cancelledAt cancellation time
 * @param cancelReason cancellation reason
 * @param cancelDate value date of the reversal journals
 */
public record ValuationRunResponse(
    Long id,
    Long companyId,
    LocalDate valuationDate,
    String periodName,
    String status,
    String baseCurrency,
    Long previousRunId,
    Instant calculatedAt,
    String remarks,
    String preparedBy,
    String submittedBy,
    Instant submittedAt,
    String approvedBy,
    Instant approvedAt,
    String rejectionReason,
    String postedBy,
    Instant postedAt,
    int journalCount,
    String cancelledBy,
    Instant cancelledAt,
    String cancelReason,
    LocalDate cancelDate) {

  /**
   * Maps a run header.
   *
   * @param r run
   * @return response
   */
  public static ValuationRunResponse from(ValuationRun r) {
    return new ValuationRunResponse(
        r.getId(),
        r.getCompanyId(),
        r.getValuationDate(),
        r.getPeriodName(),
        r.getStatus().name(),
        r.getBaseCurrency(),
        r.getPreviousRunId(),
        r.getCalculatedAt(),
        r.getRemarks(),
        r.getCreatedBy(),
        r.getSubmittedBy(),
        r.getSubmittedAt(),
        r.getApprovedBy(),
        r.getApprovedAt(),
        r.getRejectionReason(),
        r.getPostedBy(),
        r.getPostedAt(),
        r.getJournalCount(),
        r.getCancelledBy(),
        r.getCancelledAt(),
        r.getCancelReason(),
        r.getCancelDate());
  }
}
