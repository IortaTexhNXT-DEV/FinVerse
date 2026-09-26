package com.iortatechnxt.brokerverse.underwriting.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Maker-checker lifecycle shared by policies and endorsements: DRAFT → PENDING_APPROVAL → APPROVED,
 * with rejection back to DRAFT. The approver must differ from both the creator and the submitter.
 */
@Embeddable
public class ApprovalWorkflow {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PolicyStatus status = PolicyStatus.DRAFT;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approval_date")
  private LocalDate approvalDate;

  @Column(name = "rejection_reason", length = 200)
  private String rejectionReason;

  /**
   * Fails unless the document can still be edited.
   *
   * @param document document label for the message
   */
  public void requireEditable(String document) {
    require(PolicyStatus.DRAFT, document, "change");
  }

  /**
   * Maker submits the document for approval.
   *
   * @param document document label
   * @param user maker
   * @param when timestamp
   */
  public void submit(String document, String user, Instant when) {
    require(PolicyStatus.DRAFT, document, "submit");
    this.status = PolicyStatus.PENDING_APPROVAL;
    this.submittedBy = user;
    this.submittedAt = when;
    this.rejectionReason = null;
  }

  /**
   * Checker approves the document.
   *
   * @param document document label
   * @param maker user who created the document
   * @param checker approving user
   * @param when timestamp
   * @param accountingDate approval (accounting) date
   */
  public void approve(
      String document, String maker, String checker, Instant when, LocalDate accountingDate) {
    require(PolicyStatus.PENDING_APPROVAL, document, "approve");
    if (Objects.equals(maker, checker) || Objects.equals(submittedBy, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          document + " cannot be approved by the user who created or submitted it");
    }
    this.status = PolicyStatus.APPROVED;
    this.approvedBy = checker;
    this.approvedAt = when;
    this.approvalDate = accountingDate;
  }

  /**
   * Checker returns the document to the maker.
   *
   * @param document document label
   * @param reason reason
   */
  public void reject(String document, String reason) {
    require(PolicyStatus.PENDING_APPROVAL, document, "reject");
    this.status = PolicyStatus.DRAFT;
    this.rejectionReason = reason;
  }

  /**
   * Discards a draft.
   *
   * @param document document label
   */
  public void discard(String document) {
    require(PolicyStatus.DRAFT, document, "discard");
    this.status = PolicyStatus.CANCELLED;
  }

  /**
   * Marks an approved document as cancelled (policy cancellation).
   *
   * @param document document label
   */
  public void cancelApproved(String document) {
    require(PolicyStatus.APPROVED, document, "cancel");
    this.status = PolicyStatus.CANCELLED;
  }

  /**
   * Fails unless the document is approved.
   *
   * @param document document label
   * @param action attempted action
   */
  public void requireApproved(String document, String action) {
    require(PolicyStatus.APPROVED, document, action);
  }

  private void require(PolicyStatus expected, String document, String action) {
    if (status != expected) {
      throw new BusinessRuleException(
          "INVALID_POLICY_STATUS", "Cannot " + action + " " + document + " in status " + status);
    }
  }

  public PolicyStatus getStatus() {
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
}
