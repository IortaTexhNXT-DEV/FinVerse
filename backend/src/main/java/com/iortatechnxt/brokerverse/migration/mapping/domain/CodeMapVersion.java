package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A version of a code map set (gate G2; FR-DM-011): DRAFT while the Data Steward edits it,
 * SUBMITTED to the business owner, APPROVED (one per set; the previous approved version becomes
 * SUPERSEDED). The approver is never the submitter. A batch records the version it was validated
 * with.
 */
@Entity
@Table(name = "mig_code_map_version")
public class CodeMapVersion extends BaseEntity {

  @Column(name = "set_code", nullable = false, length = 60, updatable = false)
  private String setCode;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MapVersionStatus status = MapVersionStatus.DRAFT;

  @Column(length = 1000)
  private String comment;

  @Column(name = "return_reason", length = 1000)
  private String returnReason;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  protected CodeMapVersion() {}

  /**
   * A new DRAFT version.
   *
   * @param setCode set
   * @param versionNo number
   * @param comment comment
   */
  public CodeMapVersion(String setCode, int versionNo, String comment) {
    this.setCode = setCode;
    this.versionNo = versionNo;
    this.comment = comment;
  }

  /** Refuses a change unless the version is a draft. */
  public void requireDraft() {
    if (status != MapVersionStatus.DRAFT) {
      throw new BusinessRuleException(
          "MIG_MAP_NOT_DRAFT", "Version " + versionNo + " of " + setCode + " is no longer a draft");
    }
  }

  /**
   * Submits the draft.
   *
   * @param user Data Steward
   * @param when time
   */
  public void submit(String user, Instant when) {
    requireDraft();
    this.status = MapVersionStatus.SUBMITTED;
    this.submittedBy = user;
    this.submittedAt = when;
    this.returnReason = null;
  }

  /**
   * Approves the submitted version.
   *
   * @param user business owner
   * @param when time
   */
  public void approve(String user, Instant when) {
    requireSubmitted(user);
    this.status = MapVersionStatus.APPROVED;
    this.approvedBy = user;
    this.approvedAt = when;
  }

  /**
   * Returns the submitted version to DRAFT.
   *
   * @param user business owner
   * @param reason reason
   */
  public void returnWith(String user, String reason) {
    requireSubmitted(user);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the return");
    }
    this.status = MapVersionStatus.DRAFT;
    this.returnReason = reason.strip();
  }

  /** A later version was approved. */
  public void supersede() {
    this.status = MapVersionStatus.SUPERSEDED;
  }

  private void requireSubmitted(String user) {
    if (status != MapVersionStatus.SUBMITTED) {
      throw new BusinessRuleException(
          "MIG_MAP_NOT_SUBMITTED", "Version " + versionNo + " of " + setCode + " is not submitted");
    }
    if (CurrentUser.sameUser(user, submittedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
  }

  /**
   * Label used in lists and the batch evidence.
   *
   * @return set and version
   */
  public String label() {
    return setCode + " v" + versionNo;
  }

  public String getSetCode() {
    return setCode;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public MapVersionStatus getStatus() {
    return status;
  }

  public String getComment() {
    return comment;
  }

  public String getReturnReason() {
    return returnReason;
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
}
