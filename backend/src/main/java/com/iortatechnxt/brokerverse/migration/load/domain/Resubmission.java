package com.iortatechnxt.brokerverse.migration.load.domain;

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
 * A resubmission of corrected rejected rows (workflow MIG_RESUBMISSION; DATA_MIGRATION_DESIGN
 * section 15.1): the maker's corrected file of a batch, approved or returned by a checker who is
 * never the maker; an approved resubmission is loaded as a RERUN batch of the parent.
 */
@Entity
@Table(name = "mig_resubmission")
public class Resubmission extends BaseEntity {

  /** Status of a resubmission. */
  public enum Status {
    PREPARED,
    APPROVED,
    RETURNED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "resubmission_no", nullable = false, length = 20, updatable = false)
  private String resubmissionNo;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "parent_batch_id", nullable = false, updatable = false)
  private Long parentBatchId;

  @Column(name = "extract_id", nullable = false, updatable = false)
  private Long extractId;

  @Column(name = "rerun_batch_id")
  private Long rerunBatchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.PREPARED;

  @Column(name = "corrected_rows", nullable = false)
  private int correctedRows;

  @Column(name = "prepared_by", nullable = false, length = 50, updatable = false)
  private String preparedBy;

  @Column(name = "prepared_at", nullable = false, updatable = false)
  private Instant preparedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 1000)
  private String decisionNote;

  protected Resubmission() {}

  /**
   * A prepared resubmission.
   *
   * @param companyId company
   * @param resubmissionNo number
   * @param parent parent batch
   * @param extractId corrected extract
   * @param rows corrected rows
   * @param maker maker (the operator uploading on the maker's behalf is recorded as maker)
   * @param when time
   */
  public Resubmission(
      Long companyId,
      String resubmissionNo,
      MigBatch parent,
      Long extractId,
      int rows,
      String maker,
      Instant when) {
    this.companyId = companyId;
    this.resubmissionNo = resubmissionNo;
    this.objectCode = parent.getObjectCode();
    this.parentBatchId = parent.getId();
    this.extractId = extractId;
    this.correctedRows = rows;
    this.preparedBy = maker;
    this.preparedAt = when;
  }

  /**
   * The checker decided.
   *
   * @param approve approve or return
   * @param user checker
   * @param note note (mandatory on a return)
   * @param when time
   */
  public void decide(boolean approve, String user, String note, Instant when) {
    requireDecidableBy(user);
    if (!approve && (note == null || note.isBlank())) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the return");
    }
    this.status = approve ? Status.APPROVED : Status.RETURNED;
    this.decidedBy = user;
    this.decidedAt = when;
    this.decisionNote = note;
  }

  private void requireDecidableBy(String user) {
    if (status != Status.PREPARED) {
      throw new BusinessRuleException(
          "MIG_RESUBMISSION_DECIDED", "Resubmission " + resubmissionNo + " is already decided");
    }
    if (CurrentUser.sameUser(user, preparedBy) || CurrentUser.sameUser(user, getCreatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
  }

  public void setRerunBatchId(Long rerunBatchId) {
    this.rerunBatchId = rerunBatchId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getResubmissionNo() {
    return resubmissionNo;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public Long getParentBatchId() {
    return parentBatchId;
  }

  public Long getExtractId() {
    return extractId;
  }

  public Long getRerunBatchId() {
    return rerunBatchId;
  }

  public Status getStatus() {
    return status;
  }

  public int getCorrectedRows() {
    return correctedRows;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public Instant getPreparedAt() {
    return preparedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionNote() {
    return decisionNote;
  }
}
