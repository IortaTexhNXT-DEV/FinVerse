package com.iortatechnxt.brokerverse.migration.trueup.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A FY2027 opening-balance true-up (object G03; DATA_MIGRATION_DESIGN section 17.7; FR-DM-023 and
 * 024): prepared by the Comptrollership GL lead from a validated G03 batch and the legacy trial
 * balance of the same true-up, approved by the Head of Comptrollership (never the preparer), posted
 * as opening-balance adjustment journals and open-item adjustments, reconciled and signed. It keeps
 * the evidence and is not purged with the staging data.
 */
@Entity
@Table(name = "mig_trueup")
public class MigTrueup extends BaseEntity {

  /** Status of a true-up. */
  public enum Status {
    PREPARED,
    FOR_APPROVAL,
    APPROVED,
    POSTED,
    RECONCILED,
    SIGNED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "trueup_no", nullable = false, length = 2, updatable = false)
  private String trueupNo;

  @Column(nullable = false, length = 20, updatable = false)
  private String reference;

  @Column(name = "as_of", nullable = false)
  private LocalDate asOf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.PREPARED;

  @Column(name = "batch_id")
  private Long batchId;

  @Column(name = "tb_batch_id")
  private Long tbBatchId;

  @Column(name = "journals_posted", nullable = false)
  private int journalsPosted;

  @Column(name = "items_adjusted", nullable = false)
  private int itemsAdjusted;

  @Column(name = "prepared_by", nullable = false, length = 50)
  private String preparedBy;

  @Column(name = "prepared_at", nullable = false)
  private Instant preparedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "recon_run_id")
  private Long reconRunId;

  @Column(name = "signed_by", length = 50)
  private String signedBy;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "period_reopen_log", length = 2000)
  private String periodReopenLog;

  @Column(length = 1000)
  private String remarks;

  protected MigTrueup() {}

  /**
   * A prepared true-up.
   *
   * @param companyId company
   * @param trueupNo 1, 2, 3 or F
   * @param inputs the G03 batch and the legacy trial balance batch
   * @param asOf as-of date of the legacy trial balance
   * @param user Comptrollership GL lead
   * @param when time
   */
  public MigTrueup(
      Long companyId, String trueupNo, Inputs inputs, LocalDate asOf, String user, Instant when) {
    this.companyId = companyId;
    this.trueupNo = trueupNo;
    this.reference = "MIG-TU-" + trueupNo;
    this.batchId = inputs.batchId();
    this.tbBatchId = inputs.tbBatchId();
    this.asOf = asOf;
    this.preparedBy = user;
    this.preparedAt = when;
  }

  /**
   * Submits for approval.
   *
   * @param user preparer
   * @param note remarks
   */
  public void submit(String user, String note) {
    require(Status.PREPARED, "submitted");
    this.preparedBy = user;
    this.remarks = note;
    this.status = Status.FOR_APPROVAL;
  }

  /**
   * Approves (gate G4 of G03) or returns.
   *
   * @param approve approve or return
   * @param user Head of Comptrollership
   * @param note remarks
   * @param when time
   */
  public void decide(boolean approve, String user, String note, Instant when) {
    require(Status.FOR_APPROVAL, "approved");
    if (CurrentUser.sameUser(user, preparedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    if (!approve && (note == null || note.isBlank())) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the return");
    }
    this.remarks = note;
    if (approve) {
      this.approvedBy = user;
      this.approvedAt = when;
      this.status = Status.APPROVED;
    } else {
      this.status = Status.PREPARED;
    }
  }

  /**
   * Posted.
   *
   * @param journals journals posted
   * @param items open items adjusted
   * @param reopenLog periods reopened for the posting
   * @param when time
   */
  public void posted(int journals, int items, String reopenLog, Instant when) {
    require(Status.APPROVED, "posted");
    this.journalsPosted = journals;
    this.itemsAdjusted = items;
    this.periodReopenLog = reopenLog;
    this.postedAt = when;
    this.status = Status.POSTED;
  }

  /**
   * Reconciled.
   *
   * @param runId reconciliation run
   * @param clean no open break
   */
  public void reconciled(Long runId, boolean clean) {
    this.reconRunId = runId;
    if (clean && status == Status.POSTED) {
      this.status = Status.RECONCILED;
    }
  }

  /**
   * Signed (gate G5 of G03) by the Head of Comptrollership.
   *
   * @param user signer
   * @param when time
   */
  public void sign(String user, Instant when) {
    require(Status.RECONCILED, "signed");
    if (CurrentUser.sameUser(user, preparedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.signedBy = user;
    this.signedAt = when;
    this.status = Status.SIGNED;
  }

  private void require(Status expected, String action) {
    if (status != expected) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_STATUS",
          "True-up " + trueupNo + " is " + status + "; it cannot be " + action);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getTrueupNo() {
    return trueupNo;
  }

  public String getReference() {
    return reference;
  }

  public LocalDate getAsOf() {
    return asOf;
  }

  public Status getStatus() {
    return status;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getTbBatchId() {
    return tbBatchId;
  }

  public int getJournalsPosted() {
    return journalsPosted;
  }

  public int getItemsAdjusted() {
    return itemsAdjusted;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public Instant getPreparedAt() {
    return preparedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public Long getReconRunId() {
    return reconRunId;
  }

  public String getSignedBy() {
    return signedBy;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public String getPeriodReopenLog() {
    return periodReopenLog;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Inputs of a true-up.
   *
   * @param batchId validated G03 batch
   * @param tbBatchId G01 batch of the legacy trial balance of this true-up
   */
  public record Inputs(Long batchId, Long tbBatchId) {}
}
