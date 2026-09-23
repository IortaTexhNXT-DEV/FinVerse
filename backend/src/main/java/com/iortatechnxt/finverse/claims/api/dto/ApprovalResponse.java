package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.ClaimApproval;
import com.iortatechnxt.finverse.claims.domain.DocumentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Maker-checker state of a claim document.
 *
 * @param status status
 * @param submittedBy maker
 * @param submittedAt entry time
 * @param decidedBy checker who approved or rejected
 * @param decidedAt decision time
 * @param approvalDate accounting date
 * @param rejectionReason rejection reason
 * @param authorityAmount base-currency amount subject to the checker's limit
 */
public record ApprovalResponse(
    DocumentStatus status,
    String submittedBy,
    Instant submittedAt,
    String decidedBy,
    Instant decidedAt,
    LocalDate approvalDate,
    String rejectionReason,
    BigDecimal authorityAmount) {

  /**
   * Maps an approval state.
   *
   * @param a approval
   * @return response
   */
  public static ApprovalResponse from(ClaimApproval a) {
    return new ApprovalResponse(
        a.getStatus(),
        a.getSubmittedBy(),
        a.getSubmittedAt(),
        a.getApprovedBy(),
        a.getApprovedAt(),
        a.getApprovalDate(),
        a.getRejectionReason(),
        a.getAuthorityAmount());
  }
}
