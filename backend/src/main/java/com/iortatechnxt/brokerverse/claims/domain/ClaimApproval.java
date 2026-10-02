package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Maker-checker state of a claim document: PENDING_APPROVAL → APPROVED or REJECTED. The checker
 * must differ from the maker; the {@code authorityAmount} (base currency) is compared with the
 * checker's authorization limit by the service.
 */
@Embeddable
public class ClaimApproval {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DocumentStatus status = DocumentStatus.PENDING_APPROVAL;

  @Column(name = "submitted_by", nullable = false, length = 50)
  private String submittedBy;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approval_date")
  private LocalDate approvalDate;

  @Column(name = "rejection_reason", length = 200)
  private String rejectionReason;

  @Column(name = "authority_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal authorityAmount;

  protected ClaimApproval() {}

  /**
   * Starts the approval of a document entered by a maker.
   *
   * @param maker maker
   * @param when entry time
   * @param authorityAmount base-currency amount subject to the checker's limit
   */
  public ClaimApproval(String maker, Instant when, BigDecimal authorityAmount) {
    this.submittedBy = maker;
    this.submittedAt = when;
    this.authorityAmount = authorityAmount;
  }

  /**
   * An approval completed at once by the system on behalf of a checker (reserve releases on close,
   * final settlement or repudiation, which are part of that checker's decision).
   *
   * @param checker user whose decision triggered the document
   * @param when time
   * @param date accounting date
   * @return approved state
   */
  public static ClaimApproval approvedBy(String checker, Instant when, LocalDate date) {
    ClaimApproval a = new ClaimApproval(checker, when, BigDecimal.ZERO);
    a.status = DocumentStatus.APPROVED;
    a.approvedBy = checker;
    a.approvedAt = when;
    a.approvalDate = date;
    return a;
  }

  /**
   * Fails unless the document is pending.
   *
   * @param document document label
   * @param action attempted action
   */
  public void requirePending(String document, String action) {
    if (status != DocumentStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          "INVALID_DOCUMENT_STATUS", "Cannot " + action + " " + document + " in status " + status);
    }
  }

  /**
   * Checker approves.
   *
   * @param document document label
   * @param checker approving user
   * @param when time
   * @param date accounting date
   */
  public void approve(String document, String checker, Instant when, LocalDate date) {
    requirePending(document, "approve");
    if (Objects.equals(submittedBy, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", document + " cannot be approved by the user who entered it");
    }
    this.status = DocumentStatus.APPROVED;
    this.approvedBy = checker;
    this.approvedAt = when;
    this.approvalDate = date;
  }

  /**
   * Checker rejects (final).
   *
   * @param document document label
   * @param checker rejecting user
   * @param when time
   * @param reason reason
   */
  public void reject(String document, String checker, Instant when, String reason) {
    requirePending(document, "reject");
    this.status = DocumentStatus.REJECTED;
    this.approvedBy = checker;
    this.approvedAt = when;
    this.rejectionReason = reason;
  }

  /**
   * Updates the amount the checker's limit applies to (recomputed at approval).
   *
   * @param amount base-currency amount
   */
  public void setAuthorityAmount(BigDecimal amount) {
    this.authorityAmount = amount;
  }

  public DocumentStatus getStatus() {
    return status;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public LocalDate getApprovalDate() {
    return approvalDate;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public BigDecimal getAuthorityAmount() {
    return authorityAmount;
  }
}
