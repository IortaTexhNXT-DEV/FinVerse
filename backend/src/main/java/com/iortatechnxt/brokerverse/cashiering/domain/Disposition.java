package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionAction;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A disposition assigned to an unapplied item (CSHID.024): apply to another invoice, DST
 * application, refund, reclass, transfer to another marketing unit or others, with the target
 * fields of its type, the four-eyes approval and the execution result.
 */
@Entity
@Table(name = "csh_disposition")
public class Disposition extends BaseEntity {

  @Column(name = "unapplied_id", nullable = false, updatable = false)
  private Long unappliedId;

  @Column(name = "disposition_type", nullable = false, length = 40)
  private String dispositionType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DispositionAction action;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "target_invoice_no", length = 40)
  private String targetInvoiceNo;

  @Column(name = "target_client_code", length = 30)
  private String targetClientCode;

  @Column(name = "target_unit", length = 40)
  private String targetUnit;

  @Column(name = "previous_client_code", length = 30)
  private String previousClientCode;

  @Column(name = "previous_unit", length = 40)
  private String previousUnit;

  @Column(name = "payee_name", length = 250)
  private String payeeName;

  @Column(length = 250)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DispositionStatus status = DispositionStatus.MONITORING;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "application_id")
  private Long applicationId;

  @Column(name = "disbursement_request_no", length = 30)
  private String disbursementRequestNo;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "reversal_reason", length = 250)
  private String reversalReason;

  @Column(name = "reversal_requested_by", length = 50)
  private String reversalRequestedBy;

  protected Disposition() {}

  /**
   * Creates a disposition in Monitoring.
   *
   * @param unappliedId unapplied item
   * @param rule type and action
   * @param details amount and target fields
   */
  public Disposition(Long unappliedId, DispositionTypeRule rule, DispositionDetails details) {
    this.unappliedId = unappliedId;
    assign(rule, details);
  }

  /**
   * Changes the disposition before it is processed (CSHID.024 "update before final processing").
   *
   * @param rule type and action
   * @param details amount and target fields
   */
  public void update(DispositionTypeRule rule, DispositionDetails details) {
    assign(rule, details);
  }

  private void assign(DispositionTypeRule rule, DispositionDetails details) {
    this.dispositionType = rule.getTypeCode();
    this.action = rule.getAction();
    this.amount = details.amount();
    this.targetInvoiceNo = details.targetInvoiceNo();
    this.targetClientCode = details.targetClientCode();
    this.targetUnit = details.targetUnit();
    this.payeeName = details.payeeName();
    this.remarks = details.remarks();
  }

  /**
   * Sets the status.
   *
   * @param next status
   */
  public void markStatus(DispositionStatus next) {
    this.status = next;
  }

  /**
   * Records the approval.
   *
   * @param by approver
   * @param at time
   */
  public void approve(String by, Instant at) {
    this.approvedBy = by;
    this.approvedAt = at;
    this.status = DispositionStatus.IN_PROCESS;
  }

  /**
   * Records the execution.
   *
   * @param result application, disbursement request, journal and previous owner
   * @param at time
   */
  public void complete(Execution result, Instant at) {
    this.applicationId = result.applicationId();
    this.disbursementRequestNo = result.disbursementRequestNo();
    this.journalBatchNo = result.journalBatchNo();
    this.previousClientCode = result.previousClientCode();
    this.previousUnit = result.previousUnit();
    this.completedAt = at;
    this.status = DispositionStatus.COMPLETED;
  }

  /**
   * Marks the disposition for reversal.
   *
   * @param reason reason
   * @param by requester
   */
  public void requestReversal(String reason, String by) {
    this.reversalReason = reason;
    this.reversalRequestedBy = by;
    this.status = DispositionStatus.FOR_REVERSAL;
  }

  public Long getUnappliedId() {
    return unappliedId;
  }

  public String getDispositionType() {
    return dispositionType;
  }

  public DispositionAction getAction() {
    return action;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getTargetInvoiceNo() {
    return targetInvoiceNo;
  }

  public String getTargetClientCode() {
    return targetClientCode;
  }

  public String getTargetUnit() {
    return targetUnit;
  }

  public String getPreviousClientCode() {
    return previousClientCode;
  }

  public String getPreviousUnit() {
    return previousUnit;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getRemarks() {
    return remarks;
  }

  public DispositionStatus getStatus() {
    return status;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Long getApplicationId() {
    return applicationId;
  }

  public String getDisbursementRequestNo() {
    return disbursementRequestNo;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getReversalReason() {
    return reversalReason;
  }

  public String getReversalRequestedBy() {
    return reversalRequestedBy;
  }

  /**
   * Amount and target fields of a disposition.
   *
   * @param amount amount disposed
   * @param targetInvoiceNo invoice to apply to (apply, DST application)
   * @param targetClientCode client to reclass to
   * @param targetUnit marketing unit to transfer to
   * @param payeeName refund payee
   * @param remarks remarks
   */
  public record DispositionDetails(
      BigDecimal amount,
      String targetInvoiceNo,
      String targetClientCode,
      String targetUnit,
      String payeeName,
      String remarks) {}

  /**
   * Result of an executed disposition.
   *
   * @param applicationId application created, may be null
   * @param disbursementRequestNo refund request, may be null
   * @param journalBatchNo journal, may be null
   * @param previousClientCode client before a reclass, may be null
   * @param previousUnit unit before a transfer, may be null
   */
  public record Execution(
      Long applicationId,
      String disbursementRequestNo,
      String journalBatchNo,
      String previousClientCode,
      String previousUnit) {}
}
