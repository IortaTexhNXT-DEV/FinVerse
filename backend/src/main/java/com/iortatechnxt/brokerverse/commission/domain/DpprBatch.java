package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A legacy direct payment PR reversal batch (DATA_MIGRATION_DESIGN 14.4 E; BRID 7.1): legacy
 * invoices the client paid directly to the insurer, whose open premium receivable and due to
 * insurer are reversed once the batch is approved; the maker never approves.
 */
@Entity
@Table(name = "cmr_dppr_batch")
public class DpprBatch extends BaseEntity {

  /** Status of a batch. */
  public enum Status {
    /** Being prepared. */
    DRAFT,
    /** Waiting for the approver. */
    FOR_APPROVAL,
    /** Posted. */
    EXECUTED,
    /** Cancelled. */
    CANCELLED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 20, updatable = false)
  private String batchNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "executed_at")
  private Instant executedAt;

  @Column(name = "posted_count", nullable = false)
  private int postedCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(name = "return_reason", length = 500)
  private String returnReason;

  protected DpprBatch() {}

  /**
   * A new batch in DRAFT.
   *
   * @param companyId company
   * @param batchNo number
   * @param reason reason
   */
  public DpprBatch(Long companyId, String batchNo, String reason) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.reason = reason;
  }

  /**
   * Adds a line amount.
   *
   * @param amount amount
   */
  public void lineAdded(BigDecimal amount) {
    requireStatus(Status.DRAFT);
    total = total.add(amount);
    lineCount++;
  }

  /**
   * Removes a line amount.
   *
   * @param amount amount
   */
  public void lineRemoved(BigDecimal amount) {
    requireStatus(Status.DRAFT);
    total = total.subtract(amount);
    lineCount--;
  }

  /**
   * Submits the batch.
   *
   * @param user requester
   * @param when time
   */
  public void submit(String user, Instant when) {
    requireStatus(Status.DRAFT);
    if (lineCount == 0) {
      throw new BusinessRuleException("CMR_BATCH_EMPTY", "Add at least one line to the batch");
    }
    status = Status.FOR_APPROVAL;
    submittedBy = user;
    submittedAt = when;
    returnReason = null;
  }

  /**
   * Approves the batch.
   *
   * @param user approver
   * @param when time
   */
  public void approve(String user, Instant when) {
    requireStatus(Status.FOR_APPROVAL);
    approvedBy = user;
    approvedAt = when;
  }

  /**
   * Returns the batch.
   *
   * @param why reason
   */
  public void returnToDraft(String why) {
    requireStatus(Status.FOR_APPROVAL);
    status = Status.DRAFT;
    returnReason = why;
  }

  /** Cancels a draft. */
  public void cancel() {
    requireStatus(Status.DRAFT);
    status = Status.CANCELLED;
  }

  /**
   * Records the run.
   *
   * @param posted lines posted
   * @param failed lines refused
   * @param when time
   */
  public void executed(int posted, int failed, Instant when) {
    status = Status.EXECUTED;
    postedCount = posted;
    failedCount = failed;
    executedAt = when;
  }

  /**
   * Refuses an action in another status.
   *
   * @param expected status
   */
  public void requireStatus(Status expected) {
    if (status != expected) {
      throw new BusinessRuleException(
          "CMR_BATCH_STATUS", "Batch " + batchNo + " is " + status + ", not " + expected);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public Status getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public int getLineCount() {
    return lineCount;
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

  public Instant getExecutedAt() {
    return executedAt;
  }

  public int getPostedCount() {
    return postedCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getReturnReason() {
    return returnReason;
  }
}
