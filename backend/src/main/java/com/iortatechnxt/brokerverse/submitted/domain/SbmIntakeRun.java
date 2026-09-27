package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An intake run (BRIDSP-01): one source file loaded, with its file hash and the counts of the rows
 * received, created, updated, duplicated and failed, and the processing run it started.
 */
@Entity
@Table(name = "sbm_intake_run")
public class SbmIntakeRun extends BaseEntity {

  /** Open while its rows are committed. */
  public static final String OPEN = "OPEN";

  /** All rows committed. */
  public static final String COMPLETED = "COMPLETED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, updatable = false, length = 30)
  private String runNo;

  @Column(name = "source_code", nullable = false, updatable = false, length = 30)
  private String sourceCode;

  @Column(name = "bulk_job_no", updatable = false, length = 30)
  private String bulkJobNo;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "file_sha256", length = 64)
  private String fileSha256;

  @Column(nullable = false)
  private int received;

  @Column(nullable = false)
  private int created;

  @Column(nullable = false)
  private int updated;

  @Column(nullable = false)
  private int duplicate;

  @Column(nullable = false)
  private int failed;

  @Column(nullable = false, length = 20)
  private String status = OPEN;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "processing_run_no", length = 30)
  private String processingRunNo;

  protected SbmIntakeRun() {}

  /**
   * Opens a run.
   *
   * @param companyId company
   * @param runNo run number
   * @param sourceCode source
   * @param file bulk job, file name and hash
   * @param startedAt start
   */
  public SbmIntakeRun(
      Long companyId, String runNo, String sourceCode, FileFacts file, Instant startedAt) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.sourceCode = sourceCode;
    this.bulkJobNo = file.bulkJobNo();
    this.fileName = file.fileName();
    this.fileSha256 = file.sha256();
    this.startedAt = startedAt;
  }

  /**
   * Counts a row.
   *
   * @param outcome CREATED, UPDATED or DUPLICATE
   */
  public void count(String outcome) {
    received++;
    switch (outcome) {
      case "CREATED" -> created++;
      case "UPDATED" -> updated++;
      default -> duplicate++;
    }
  }

  /**
   * Closes the run.
   *
   * @param failedRows rows that failed
   * @param at time
   * @param processingRun processing run started for its records, may be null
   */
  public void complete(int failedRows, Instant at, String processingRun) {
    this.failed = failedRows;
    this.received = received + failedRows;
    this.status = COMPLETED;
    this.finishedAt = at;
    this.processingRunNo = processingRun;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public String getBulkJobNo() {
    return bulkJobNo;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileSha256() {
    return fileSha256;
  }

  public int getReceived() {
    return received;
  }

  public int getCreated() {
    return created;
  }

  public int getUpdated() {
    return updated;
  }

  public int getDuplicate() {
    return duplicate;
  }

  public int getFailed() {
    return failed;
  }

  public String getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public String getProcessingRunNo() {
    return processingRunNo;
  }

  /**
   * The file of a run.
   *
   * @param bulkJobNo bulk upload number
   * @param fileName file name
   * @param sha256 file hash
   */
  public record FileFacts(String bulkJobNo, String fileName, String sha256) {}
}
