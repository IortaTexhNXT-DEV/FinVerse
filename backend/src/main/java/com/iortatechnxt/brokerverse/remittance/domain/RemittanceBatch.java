package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A remittance batch {@code RMB-<insurer>-<yyyy>} of one insurer, remittance type and currency
 * (RMTID.007/008): its lines, the totals of the lines not excluded (RMTID.002), the stage of the
 * Process Remittance workflow (RMTID.009-011/019/036), the Disbursement request and DV number, the
 * commission and incentive ORs (CSHID.007, RMTID.023) and the schedule sent to the insurer
 * (MKTID.001).
 */
@Entity
@Table(name = "rem_batch")
public class RemittanceBatch extends BaseEntity {

  private static final String STAGE_ERROR = "REMIT_BATCH_STAGE";
  private static final String BATCH = "Batch ";
  private static final int MAX_MESSAGE = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 40, updatable = false)
  private String batchNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "remittance_type", nullable = false, length = 20, updatable = false)
  private RemittanceType remittanceType;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Column(name = "special_request_no", length = 30, updatable = false)
  private String specialRequestNo;

  @Column(length = 50)
  private String processor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BatchStage stage = BatchStage.REVIEW_IN_PROCESS;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Embedded private RemittanceAmounts totals = RemittanceAmounts.NONE;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "return_reason", length = 250)
  private String returnReason;

  @Column(name = "disbursement_request_no", length = 30)
  private String disbursementRequestNo;

  @Column(name = "disbursement_status", length = 20)
  private String disbursementStatus;

  @Column(name = "dv_no", length = 40)
  private String dvNo;

  @Column(name = "disbursed_amount", precision = 19, scale = 2)
  private BigDecimal disbursedAmount;

  @Column(name = "commission_or_no", length = 40)
  private String commissionOrNo;

  @Column(name = "commission_or_status", length = 20)
  private String commissionOrStatus;

  @Column(name = "incentive_or_no", length = 40)
  private String incentiveOrNo;

  @Column(name = "incentive_or_status", length = 20)
  private String incentiveOrStatus;

  @Column(name = "or_message", length = MAX_MESSAGE)
  private String orMessage;

  @Column(name = "schedule_sent_at")
  private Instant scheduleSentAt;

  @Column(name = "schedule_sent_by", length = 50)
  private String scheduleSentBy;

  @Column(name = "extract_file_id")
  private Long extractFileId;

  @Embedded private BatchSettlement settlement = new BatchSettlement();

  @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<BatchLine> lines = new ArrayList<>();

  protected RemittanceBatch() {}

  /**
   * A new batch in review.
   *
   * @param header company, number, insurer, type, currency and origin
   * @param processor processor who works it
   */
  public RemittanceBatch(Header header, String processor) {
    this.companyId = header.companyId();
    this.batchNo = header.batchNo();
    this.insurerCode = header.insurerCode();
    this.remittanceType = header.type();
    this.currency = header.currency();
    this.runId = header.runId();
    this.specialRequestNo = header.specialRequestNo();
    this.processor = processor;
  }

  /**
   * Adds an extracted line.
   *
   * @param line line
   */
  public void add(BatchLine line) {
    line.attach(this);
    lines.add(line);
    recompute();
  }

  /**
   * A line.
   *
   * @param invoiceNo invoice
   * @return line
   */
  public BatchLine line(String invoiceNo) {
    return lines.stream()
        .filter(l -> l.getInvoiceNo().equals(invoiceNo))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Line of " + batchNo, invoiceNo));
  }

  /**
   * The lines not excluded.
   *
   * @return lines to remit
   */
  public List<BatchLine> included() {
    return lines.stream().filter(l -> !l.isExcluded()).toList();
  }

  /**
   * Excludes a line (RMTID.002 addendum).
   *
   * @param invoiceNo invoice
   * @param exclusion reason, comment, user and time
   * @return the line
   */
  public BatchLine exclude(String invoiceNo, Exclusion exclusion) {
    requireEditable();
    BatchLine line = line(invoiceNo);
    line.exclude(exclusion.reason(), exclusion.comment(), exclusion.by(), exclusion.at());
    recompute();
    return line;
  }

  /**
   * Restores an excluded line before submission (RMTID.002 addendum).
   *
   * @param invoiceNo invoice
   * @param by user
   * @param at time
   * @return the line
   */
  public BatchLine restore(String invoiceNo, String by, Instant at) {
    requireEditable();
    BatchLine line = line(invoiceNo);
    line.restore(by, at);
    recompute();
    return line;
  }

  private void requireEditable() {
    if (!stage.isEditable()) {
      throw new BusinessRuleException(
          STAGE_ERROR, BATCH + batchNo + " is " + stage + ": lines can no longer change");
    }
  }

  private void recompute() {
    List<BatchLine> kept = included();
    lineCount = kept.size();
    totals =
        kept.stream()
            .map(BatchLine::getAmounts)
            .reduce(RemittanceAmounts.NONE, RemittanceAmounts::plus);
  }

  /**
   * Records the submission for approval (RMTID.010).
   *
   * @param by processor
   * @param at time
   */
  public void submitted(String by, Instant at) {
    if (lineCount == 0) {
      throw new BusinessRuleException(
          "REMIT_BATCH_EMPTY", BATCH + batchNo + " has no invoice left to remit");
    }
    submittedBy = by;
    submittedAt = at;
  }

  /**
   * Records the approval (RMTID.010); the approver may not be the submitter (four eyes).
   *
   * @param by approver
   * @param at time
   */
  public void approved(String by, Instant at) {
    if (CurrentUser.sameUser(by, submittedBy) || CurrentUser.sameUser(by, processor)) {
      throw new BusinessRuleException(
          "REMIT_FOUR_EYES", BATCH + batchNo + " must be approved by another user");
    }
    approvedBy = by;
    approvedAt = at;
  }

  /**
   * Records the return reason (RMTID.029).
   *
   * @param reason reason
   */
  public void returned(String reason) {
    if (!stage.isOpen()) {
      throw new BusinessRuleException(STAGE_ERROR, BATCH + batchNo + " can no longer return");
    }
    returnReason = reason;
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param newStage stage
   */
  public void markStage(BatchStage newStage) {
    this.stage = newStage;
  }

  /**
   * Records the payment request sent to Disbursement.
   *
   * @param requestNo request number
   * @param status its status
   * @param amount amount requested
   */
  public void sentToDisbursement(String requestNo, String status, BigDecimal amount) {
    this.disbursementRequestNo = requestNo;
    this.disbursementStatus = status;
    this.disbursedAmount = amount;
  }

  /**
   * Records a Disbursement status change (RMTID.019/034).
   *
   * @param status new status
   * @param dv DV number, may be null
   */
  public void disbursement(String status, String dv) {
    this.disbursementStatus = status;
    if (dv != null) {
      this.dvNo = dv;
    }
  }

  /**
   * Records the commission OR (CSHID.007).
   *
   * @param status issued or deferred
   * @param number OR number, may be null
   */
  public void commissionReceipt(String status, String number) {
    this.commissionOrStatus = status;
    this.commissionOrNo = number;
  }

  /**
   * Records the incentive OR (RMTID.023).
   *
   * @param status issued or deferred
   * @param number OR number, may be null
   */
  public void incentiveReceipt(String status, String number) {
    this.incentiveOrStatus = status;
    this.incentiveOrNo = number;
  }

  /**
   * Keeps the outcome message of the OR requests.
   *
   * @param message message
   */
  public void receiptMessage(String message) {
    this.orMessage =
        message == null || message.length() <= MAX_MESSAGE
            ? message
            : message.substring(0, MAX_MESSAGE);
  }

  /**
   * Records that the schedule was sent to the insurer (MKTID.001, sent once).
   *
   * @param by user
   * @param at time
   */
  public void scheduleSent(String by, Instant at) {
    if (scheduleSentAt != null) {
      throw new BusinessRuleException(
          "REMIT_SCHEDULE_ALREADY_SENT", "The schedule of " + batchNo + " was already sent");
    }
    scheduleSentBy = by;
    scheduleSentAt = at;
  }

  /**
   * Links the extract file stored for the shared drive (RMTID.001, OQ17).
   *
   * @param fileId extract repository id
   */
  public void extractFile(Long fileId) {
    this.extractFileId = fileId;
  }

  /**
   * Re-assigns the processor (RMTID.009).
   *
   * @param username processor
   */
  public void assignProcessor(String username) {
    this.processor = username;
  }

  /**
   * Records the deductions applied in this send cycle (ACSL 2.9.2).
   *
   * @param amount total applied, at most the amount payable
   */
  public void deductionsApplied(BigDecimal amount) {
    settlement.applied(amount);
  }

  /**
   * Links the early-incentive service invoice (DIS 3.29.1).
   *
   * @param siNo service invoice number
   * @param wtax withholding tax on it
   */
  public void earlyIncentiveInvoice(String siNo, BigDecimal wtax) {
    settlement.serviceInvoice(siNo, wtax);
  }

  /**
   * Records that the batch's DV was cancelled (DIS 2.20.0): the next payment request and postings
   * use a new cycle reference; the deductions of the cycle are released.
   *
   * @param dv cancelled DV, may be null
   * @param reason cancellation reason
   * @param at time
   */
  public void dvCancelled(String dv, String reason, Instant at) {
    settlement.cancelled(dv == null ? dvNo : dv, reason, at, MAX_MESSAGE);
    this.dvNo = null;
    this.disbursementStatus = "CANCELLED";
  }

  /**
   * The reference of the payment request and postings of the current cycle.
   *
   * @return batch number, or {@code <batch>/R<cycle>} after a cancelled DV
   */
  public String cycleReference() {
    return settlement.reference(batchNo);
  }

  /**
   * What Disbursement pays the insurer now.
   *
   * @return amount payable less the deductions applied
   */
  public BigDecimal amountDue() {
    return totals.payable().subtract(settlement.getDeductionAmount());
  }

  public BatchSettlement getSettlement() {
    return settlement;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public RemittanceType getRemittanceType() {
    return remittanceType;
  }

  public String getCurrency() {
    return currency;
  }

  public Long getRunId() {
    return runId;
  }

  public String getSpecialRequestNo() {
    return specialRequestNo;
  }

  public String getProcessor() {
    return processor;
  }

  public BatchStage getStage() {
    return stage;
  }

  public int getLineCount() {
    return lineCount;
  }

  public RemittanceAmounts getTotals() {
    return totals;
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

  public String getReturnReason() {
    return returnReason;
  }

  public String getDisbursementRequestNo() {
    return disbursementRequestNo;
  }

  public String getDisbursementStatus() {
    return disbursementStatus;
  }

  public String getDvNo() {
    return dvNo;
  }

  public BigDecimal getDisbursedAmount() {
    return disbursedAmount;
  }

  public String getCommissionOrNo() {
    return commissionOrNo;
  }

  public String getCommissionOrStatus() {
    return commissionOrStatus;
  }

  public String getIncentiveOrNo() {
    return incentiveOrNo;
  }

  public String getIncentiveOrStatus() {
    return incentiveOrStatus;
  }

  public String getOrMessage() {
    return orMessage;
  }

  public Instant getScheduleSentAt() {
    return scheduleSentAt;
  }

  public String getScheduleSentBy() {
    return scheduleSentBy;
  }

  public Long getExtractFileId() {
    return extractFileId;
  }

  public List<BatchLine> getLines() {
    return Collections.unmodifiableList(lines);
  }

  /**
   * Identity of a new batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @param insurerCode insurer
   * @param type remittance type
   * @param currency currency
   * @param runId extraction run
   * @param specialRequestNo special remittance request, may be null
   */
  public record Header(
      Long companyId,
      String batchNo,
      String insurerCode,
      RemittanceType type,
      String currency,
      Long runId,
      String specialRequestNo) {}

  /**
   * An exclusion.
   *
   * @param reason reason code
   * @param comment comment
   * @param by user
   * @param at time
   */
  public record Exclusion(String reason, String comment, String by, Instant at) {}
}
