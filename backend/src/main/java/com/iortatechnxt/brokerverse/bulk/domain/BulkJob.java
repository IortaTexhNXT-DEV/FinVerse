package com.iortatechnxt.brokerverse.bulk.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/** One uploaded file for a bulk handler, with its validation and commit counts. */
@Entity
@Table(name = "bulk_job")
public class BulkJob extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "job_no", nullable = false, length = 30, updatable = false)
  private String jobNo;

  @Column(name = "handler_code", nullable = false, length = 40, updatable = false)
  private String handlerCode;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(columnDefinition = "text", updatable = false)
  private String parameters;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BulkJobStatus status = BulkJobStatus.VALIDATED;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "valid_rows", nullable = false)
  private int validRows;

  @Column(name = "invalid_rows", nullable = false)
  private int invalidRows;

  @Column(name = "committed_rows", nullable = false)
  private int committedRows;

  @Column(name = "failed_rows", nullable = false)
  private int failedRows;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected BulkJob() {}

  /**
   * Creates a job.
   *
   * @param companyId company
   * @param jobNo job number
   * @param handlerCode handler
   * @param fileName uploaded file name
   * @param parameters handler parameters (JSON), may be null
   */
  public BulkJob(
      Long companyId, String jobNo, String handlerCode, String fileName, String parameters) {
    this.companyId = companyId;
    this.jobNo = jobNo;
    this.handlerCode = handlerCode;
    this.fileName = fileName;
    this.parameters = parameters;
  }

  /**
   * Records the validation result.
   *
   * @param valid valid rows
   * @param invalid invalid rows
   */
  public void validated(int valid, int invalid) {
    this.validRows = valid;
    this.invalidRows = invalid;
    this.totalRows = valid + invalid;
  }

  /**
   * Records the commit result.
   *
   * @param committed committed rows
   * @param failed rows failed at commit
   * @param when completion time
   */
  public void completed(int committed, int failed, Instant when) {
    requireValidated();
    this.committedRows = committed;
    this.failedRows = failed;
    this.status = BulkJobStatus.COMPLETED;
    this.completedAt = when;
  }

  /**
   * Discards the job.
   *
   * @param when time
   */
  public void cancel(Instant when) {
    requireValidated();
    this.status = BulkJobStatus.CANCELLED;
    this.completedAt = when;
  }

  /** Ensures the job still waits for a decision. */
  public void requireValidated() {
    if (status != BulkJobStatus.VALIDATED) {
      throw new BusinessRuleException(
          "BULK_JOB_CLOSED", "Upload " + jobNo + " is already " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getJobNo() {
    return jobNo;
  }

  public String getHandlerCode() {
    return handlerCode;
  }

  public String getFileName() {
    return fileName;
  }

  public String getParameters() {
    return parameters;
  }

  public BulkJobStatus getStatus() {
    return status;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public int getValidRows() {
    return validRows;
  }

  public int getInvalidRows() {
    return invalidRows;
  }

  public int getCommittedRows() {
    return committedRows;
  }

  public int getFailedRows() {
    return failedRows;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
