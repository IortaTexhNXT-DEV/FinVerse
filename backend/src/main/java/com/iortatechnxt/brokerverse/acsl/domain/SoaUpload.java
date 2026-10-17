package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An insurer statement of account uploaded for a covered period (ACSL 2.2.1, 2.4.0): the file and
 * its checksum, the layout used, the rows read, loaded and failed (the upload log proving every row
 * was loaded) and the latest reconciliation run.
 */
@Entity
@Table(name = "acsl_soa_upload")
public class SoaUpload extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "upload_no", nullable = false, length = 30, updatable = false)
  private String uploadNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(name = "file_name", nullable = false, length = 250, updatable = false)
  private String fileName;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(name = "layout_code", nullable = false, length = 30, updatable = false)
  private String layoutCode;

  @Column(name = "rows_read", nullable = false)
  private int rowsRead;

  @Column(name = "rows_loaded", nullable = false)
  private int rowsLoaded;

  @Column(name = "rows_failed", nullable = false)
  private int rowsFailed;

  @Column(name = "last_run_id")
  private Long lastRunId;

  protected SoaUpload() {}

  /**
   * Records an upload.
   *
   * @param companyId company
   * @param uploadNo upload number
   * @param period insurer and covered period
   * @param file file name and checksum
   * @param layoutCode insurer code of the layout used
   */
  public SoaUpload(
      Long companyId, String uploadNo, Period period, FileFacts file, String layoutCode) {
    this.companyId = companyId;
    this.uploadNo = uploadNo;
    this.insurerCode = period.insurerCode();
    this.periodFrom = period.from();
    this.periodTo = period.to();
    this.fileName = file.name();
    this.sha256 = file.sha256();
    this.layoutCode = layoutCode;
  }

  /**
   * Records the row counts of the load.
   *
   * @param read rows read
   * @param loaded rows loaded
   * @param failed rows failed
   */
  public void counted(int read, int loaded, int failed) {
    this.rowsRead = read;
    this.rowsLoaded = loaded;
    this.rowsFailed = failed;
  }

  /**
   * Links the latest reconciliation run.
   *
   * @param runId run
   */
  public void reconciled(Long runId) {
    this.lastRunId = runId;
  }

  /**
   * The file name of its reconciliation report (ACSL 2.14.1 "Name of Insurer_Covered Period").
   *
   * @return file name without extension
   */
  public String reportName() {
    return insurerCode + "_" + periodFrom + "_" + periodTo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUploadNo() {
    return uploadNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public String getLayoutCode() {
    return layoutCode;
  }

  public int getRowsRead() {
    return rowsRead;
  }

  public int getRowsLoaded() {
    return rowsLoaded;
  }

  public int getRowsFailed() {
    return rowsFailed;
  }

  public Long getLastRunId() {
    return lastRunId;
  }

  /**
   * Insurer and covered period of a statement.
   *
   * @param insurerCode insurer
   * @param from first day covered
   * @param to last day covered
   */
  public record Period(String insurerCode, LocalDate from, LocalDate to) {}

  /**
   * Facts of an uploaded file.
   *
   * @param name file name
   * @param sha256 checksum
   */
  public record FileFacts(String name, String sha256) {}
}
