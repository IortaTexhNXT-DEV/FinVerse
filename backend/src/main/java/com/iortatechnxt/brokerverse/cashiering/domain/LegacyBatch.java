package com.iortatechnxt.brokerverse.cashiering.domain;

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
 * A Cashiering batch of the legacy context (DATA_MIGRATION_DESIGN 14.4 D and F): the
 * reclassification of old unapplied payments to other income, approved by the Cashiering team lead
 * and then by top management, or the reversal of legacy PR 2307 balances against the insurer,
 * approved by the team lead. Lines post once the last approval is given.
 */
@Entity
@Table(name = "csh_legacy_batch")
public class LegacyBatch extends BaseEntity {

  /** Kind of batch. */
  public enum Kind {
    /** Unapplied payments to other income (BRID 5.5). */
    INCOME_RECLASS,
    /** Legacy PR 2307 against the insurer (BRID 7.2). */
    PR2307_REVERSAL
  }

  /** Status of a batch. */
  public enum Status {
    /** Being prepared by the requester. */
    DRAFT,
    /** Waiting for the team lead. */
    FOR_APPROVAL,
    /** Waiting for top management (income reclassification). */
    FOR_TOP_MANAGEMENT,
    /** Posted. */
    EXECUTED,
    /** Cancelled by the requester. */
    CANCELLED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 20, updatable = false)
  private String batchNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Kind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "first_approved_by", length = 50)
  private String firstApprovedBy;

  @Column(name = "first_approved_at")
  private Instant firstApprovedAt;

  @Column(name = "final_approved_by", length = 50)
  private String finalApprovedBy;

  @Column(name = "final_approved_at")
  private Instant finalApprovedAt;

  @Column(name = "executed_at")
  private Instant executedAt;

  @Column(name = "posted_count", nullable = false)
  private int postedCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(name = "return_reason", length = 500)
  private String returnReason;

  protected LegacyBatch() {}

  /**
   * A new batch in DRAFT.
   *
   * @param companyId company
   * @param batchNo number
   * @param kind kind
   * @param reason reason
   * @param currency currency of the lines
   */
  public LegacyBatch(Long companyId, String batchNo, Kind kind, String reason, String currency) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.kind = kind;
    this.reason = reason;
    this.currency = currency;
  }

  /**
   * Adds the amount of a line.
   *
   * @param amount line amount
   */
  public void lineAdded(BigDecimal amount) {
    requireStatus(Status.DRAFT);
    total = total.add(amount);
    lineCount++;
  }

  /**
   * Removes the amount of a line.
   *
   * @param amount line amount
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
      throw new BusinessRuleException("CASH_BATCH_EMPTY", "Add at least one line to the batch");
    }
    status = Status.FOR_APPROVAL;
    submittedBy = user;
    submittedAt = when;
    returnReason = null;
  }

  /**
   * Records the team lead's approval; an income reclassification then waits for top management.
   *
   * @param user approver
   * @param when time
   * @return true when the batch is ready to post
   */
  public boolean approveFirst(String user, Instant when) {
    requireStatus(Status.FOR_APPROVAL);
    firstApprovedBy = user;
    firstApprovedAt = when;
    if (kind == Kind.INCOME_RECLASS) {
      status = Status.FOR_TOP_MANAGEMENT;
      return false;
    }
    finalApprovedBy = user;
    finalApprovedAt = when;
    return true;
  }

  /**
   * Records top management's approval.
   *
   * @param user approver
   * @param when time
   */
  public void approveFinal(String user, Instant when) {
    requireStatus(Status.FOR_TOP_MANAGEMENT);
    finalApprovedBy = user;
    finalApprovedAt = when;
  }

  /**
   * Returns the batch to the requester.
   *
   * @param why reason
   */
  public void returnToDraft(String why) {
    if (status != Status.FOR_APPROVAL && status != Status.FOR_TOP_MANAGEMENT) {
      throw new BusinessRuleException(
          "CASH_BATCH_STATUS", "Batch " + batchNo + " is not waiting for an approval");
    }
    status = Status.DRAFT;
    returnReason = why;
    firstApprovedBy = null;
    firstApprovedAt = null;
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
   * @param failed lines failed
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
   * @param expected expected status
   */
  public void requireStatus(Status expected) {
    if (status != expected) {
      throw new BusinessRuleException(
          "CASH_BATCH_STATUS", "Batch " + batchNo + " is " + status + ", not " + expected);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public Kind getKind() {
    return kind;
  }

  public Status getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public String getCurrency() {
    return currency;
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

  public String getFirstApprovedBy() {
    return firstApprovedBy;
  }

  public String getFinalApprovedBy() {
    return finalApprovedBy;
  }

  public Instant getFinalApprovedAt() {
    return finalApprovedAt;
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
