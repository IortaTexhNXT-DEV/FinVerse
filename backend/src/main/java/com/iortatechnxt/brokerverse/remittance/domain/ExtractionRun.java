package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One remittance extraction run (RMTID.001/003/004/005): trigger, scope (insurer, type or invoice),
 * business date, counts per tag and the batches created. Every run is logged, also when it fails
 * (REMIT_EXTRACTION_FAILED).
 */
@Entity
@Table(name = "rem_extraction_run")
public class ExtractionRun extends BaseEntity {

  private static final int MAX_MESSAGE = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private ExtractionTrigger trigger;

  @Column(name = "insurer_code", length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "remittance_type", length = 20, updatable = false)
  private RemittanceType remittanceType;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RunStatus status = RunStatus.RUNNING;

  @Column(name = "examined_count", nullable = false)
  private int examinedCount;

  @Column(name = "extracted_count", nullable = false)
  private int extractedCount;

  @Column(name = "not_due_count", nullable = false)
  private int notDueCount;

  @Column(name = "due_count", nullable = false)
  private int dueCount;

  @Column(name = "batch_count", nullable = false)
  private int batchCount;

  @Column(length = MAX_MESSAGE)
  private String message;

  protected ExtractionRun() {}

  /**
   * A run that starts.
   *
   * @param companyId company
   * @param runNo run number
   * @param scope trigger, insurer, type and invoice
   * @param businessDate business date
   * @param at start time
   */
  public ExtractionRun(
      Long companyId, String runNo, Scope scope, LocalDate businessDate, Instant at) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.trigger = scope.trigger();
    this.insurerCode = scope.insurerCode();
    this.remittanceType = scope.type();
    this.invoiceNo = scope.invoiceNo();
    this.businessDate = businessDate;
    this.startedAt = at;
  }

  /**
   * Counts one examined invoice under its tag.
   *
   * @param tag tag given
   */
  public void count(ExtractionTag tag) {
    examinedCount++;
    switch (tag) {
      case EXTRACTED -> extractedCount++;
      case UNEXTRACTED_DUE -> dueCount++;
      default -> notDueCount++;
    }
  }

  /** Counts a batch created. */
  public void batchCreated() {
    batchCount++;
  }

  /**
   * Ends the run.
   *
   * @param outcome succeeded or failed
   * @param text summary or error
   * @param at end time
   */
  public void finish(RunStatus outcome, String text, Instant at) {
    this.status = outcome;
    this.message =
        text == null || text.length() <= MAX_MESSAGE ? text : text.substring(0, MAX_MESSAGE);
    this.endedAt = at;
  }

  /**
   * Summary of the counts.
   *
   * @return text
   */
  public String summary() {
    return examinedCount
        + " examined, "
        + extractedCount
        + " extracted in "
        + batchCount
        + " batch(es), "
        + dueCount
        + " due but not extracted, "
        + notDueCount
        + " not yet due";
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public ExtractionTrigger getTrigger() {
    return trigger;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public RemittanceType getRemittanceType() {
    return remittanceType;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public LocalDate getBusinessDate() {
    return businessDate;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public RunStatus getStatus() {
    return status;
  }

  public int getExaminedCount() {
    return examinedCount;
  }

  public int getExtractedCount() {
    return extractedCount;
  }

  public int getNotDueCount() {
    return notDueCount;
  }

  public int getDueCount() {
    return dueCount;
  }

  public int getBatchCount() {
    return batchCount;
  }

  public String getMessage() {
    return message;
  }

  /**
   * What a run covers.
   *
   * @param trigger what started it
   * @param insurerCode insurer, null for all
   * @param type remittance type, null for all
   * @param invoiceNo one invoice (RMTID.004), null for all
   */
  public record Scope(
      ExtractionTrigger trigger, String insurerCode, RemittanceType type, String invoiceNo) {}
}
