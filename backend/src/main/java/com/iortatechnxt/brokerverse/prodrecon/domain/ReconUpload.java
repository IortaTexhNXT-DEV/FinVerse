package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One attempt to upload an insurer production report (PRCID.009/010/031/032): file, checksum,
 * attempt number per insurer and month, outcome with row counts and the flow-in run, including
 * refused duplicates.
 */
@Entity
@Table(name = "prc_upload")
public class ReconUpload extends BaseEntity {

  private static final int MAX_MESSAGE = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "cycle_id")
  private Long cycleId;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "production_month")
  private LocalDate productionMonth;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(name = "attempt_no", nullable = false, updatable = false)
  private int attemptNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UploadStatus status;

  @Column(name = "run_no", length = 30)
  private String runNo;

  @Column(name = "rows_read", nullable = false)
  private int rowsRead;

  @Column(name = "rows_accepted", nullable = false)
  private int rowsAccepted;

  @Column(name = "rows_failed", nullable = false)
  private int rowsFailed;

  @Column(length = MAX_MESSAGE)
  private String message;

  protected ReconUpload() {}

  /**
   * A new attempt.
   *
   * @param companyId company
   * @param file file name and checksum
   * @param period insurer, production month and cycle (any may be null when unknown)
   * @param attemptNo attempt number for the insurer and month
   * @param status initial status
   */
  public ReconUpload(
      Long companyId, FileKey file, Period period, int attemptNo, UploadStatus status) {
    this.companyId = companyId;
    this.fileName = file.fileName();
    this.sha256 = file.sha256();
    this.insurerCode = period.insurerCode();
    this.productionMonth = period.productionMonth();
    this.cycleId = period.cycleId();
    this.attemptNo = attemptNo;
    this.status = status;
  }

  /**
   * Records the outcome.
   *
   * @param outcome status
   * @param run flow-in run number, may be null
   * @param counts rows read, accepted and failed
   * @param text message, may be null
   */
  public void finish(UploadStatus outcome, String run, RowCounts counts, String text) {
    this.status = outcome;
    this.runNo = run;
    this.rowsRead = counts.read();
    this.rowsAccepted = counts.accepted();
    this.rowsFailed = counts.failed();
    this.message =
        text == null || text.length() <= MAX_MESSAGE ? text : text.substring(0, MAX_MESSAGE);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public LocalDate getProductionMonth() {
    return productionMonth;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public int getAttemptNo() {
    return attemptNo;
  }

  public UploadStatus getStatus() {
    return status;
  }

  public String getRunNo() {
    return runNo;
  }

  public int getRowsRead() {
    return rowsRead;
  }

  public int getRowsAccepted() {
    return rowsAccepted;
  }

  public int getRowsFailed() {
    return rowsFailed;
  }

  public String getMessage() {
    return message;
  }

  /**
   * A file.
   *
   * @param fileName file name
   * @param sha256 checksum
   */
  public record FileKey(String fileName, String sha256) {}

  /**
   * What an upload is about.
   *
   * @param insurerCode insurer, may be null
   * @param productionMonth first day of the month, may be null
   * @param cycleId cycle, may be null
   */
  public record Period(String insurerCode, LocalDate productionMonth, Long cycleId) {

    /** Unknown (unreadable file). */
    public static final Period UNKNOWN = new Period(null, null, null);
  }

  /**
   * Row counts.
   *
   * @param read rows read
   * @param accepted rows accepted
   * @param failed rows failed
   */
  public record RowCounts(int read, int accepted, int failed) {

    /** Nothing read. */
    public static final RowCounts NONE = new RowCounts(0, 0, 0);
  }
}
