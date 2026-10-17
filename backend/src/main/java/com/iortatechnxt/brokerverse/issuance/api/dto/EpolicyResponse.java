package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A received e-policy with its extraction, review and dispatch (BRNB.073/074/077/104).
 *
 * @param id id
 * @param accountId account
 * @param arn Account Reference Number
 * @param attachmentId EPOLICY document of the account
 * @param fileName file name
 * @param matchMethod how it was matched to the account
 * @param status RECEIVED, REVIEW, CONFIRMED or REJECTED
 * @param extractedPolicyNumbers policy numbers found
 * @param extractedPeriodFrom period start found
 * @param extractedPeriodTo period end found
 * @param extractedPremium premium found
 * @param extractionNote what could not be read
 * @param policyNumbers confirmed policy numbers
 * @param issueDate issue date
 * @param reviewedBy reviewer
 * @param reviewedAt review time
 * @param rejectReason rejection reason
 * @param dispatchCount sends to the client
 * @param dispatchedAt last send
 * @param dispatchedTo last recipients
 * @param createdAt received at
 * @param createdBy received by
 */
public record EpolicyResponse(
    Long id,
    Long accountId,
    String arn,
    Long attachmentId,
    String fileName,
    String matchMethod,
    String status,
    List<String> extractedPolicyNumbers,
    LocalDate extractedPeriodFrom,
    LocalDate extractedPeriodTo,
    BigDecimal extractedPremium,
    String extractionNote,
    List<String> policyNumbers,
    LocalDate issueDate,
    String reviewedBy,
    Instant reviewedAt,
    String rejectReason,
    int dispatchCount,
    Instant dispatchedAt,
    String dispatchedTo,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps an e-policy.
   *
   * @param e e-policy
   * @return response
   */
  public static EpolicyResponse from(Epolicy e) {
    return new EpolicyResponse(
        e.getId(),
        e.getAccountId(),
        e.getArn(),
        e.getAttachmentId(),
        e.getFileName(),
        e.getMatchMethod().name(),
        e.getStatus().name(),
        e.getExtractedPolicyNumberList(),
        e.getExtractedPeriodFrom(),
        e.getExtractedPeriodTo(),
        e.getExtractedPremium(),
        e.getExtractionNote(),
        e.getPolicyNumberList(),
        e.getIssueDate(),
        e.getReviewedBy(),
        e.getReviewedAt(),
        e.getRejectReason(),
        e.getDispatchCount(),
        e.getDispatchedAt(),
        e.getDispatchedTo(),
        e.getCreatedAt(),
        e.getCreatedBy());
  }
}
