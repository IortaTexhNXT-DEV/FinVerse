package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A payment file received by upload or via MFT (FRS.CSH.05.01.02 to 05.01.06, 02.02.03 to
 * 02.02.04.05): its upload reference, type, name, size and checksum, the source, the checks it
 * passed or the reason it was refused, its raw copy kept read-only (source file), the uploader, the
 * processing time and the run that processed its rows.
 */
@Entity
@Table(name = "csh_channel_file")
public class ChannelFile extends BaseEntity {

  /** Received, checks passed, not processed yet. */
  public static final String RECEIVED = "RECEIVED";

  /** Refused by a file check. */
  public static final String REFUSED = "REFUSED";

  /** Every row processed. */
  public static final String PROCESSED = "PROCESSED";

  /** Processed with failed rows to correct and process again. */
  public static final String PARTIAL = "PARTIAL";

  private static final int MESSAGE_MAX = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "upload_ref", nullable = false, length = 40, updatable = false)
  private String uploadRef;

  @Column(name = "file_type", nullable = false, length = 30, updatable = false)
  private String fileType;

  @Column(name = "file_name", nullable = false, length = 250, updatable = false)
  private String fileName;

  @Column(name = "file_size", nullable = false, updatable = false)
  private long fileSize;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, length = 10, updatable = false)
  private String source;

  @Column(nullable = false, length = 20)
  private String status = RECEIVED;

  @Column(length = MESSAGE_MAX)
  private String message;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  @Column(name = "bulk_job_id")
  private Long bulkJobId;

  @Column(name = "bulk_job_no", length = 40)
  private String bulkJobNo;

  @Column(name = "rows_read", nullable = false)
  private int rowsRead;

  @Column(name = "rows_failed", nullable = false)
  private int rowsFailed;

  @Column(name = "header_count")
  private Integer headerCount;

  @Column(name = "header_total", precision = 19, scale = 2)
  private BigDecimal headerTotal;

  @Column(name = "detail_total", precision = 19, scale = 2)
  private BigDecimal detailTotal;

  @Column(name = "uploaded_by", nullable = false, length = 50, updatable = false)
  private String uploadedBy;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  protected ChannelFile() {}

  /**
   * Records a file as received.
   *
   * @param companyId company
   * @param uploadRef upload reference (upload id)
   * @param identity type, name, size, checksum and source
   * @param by uploader (the MFT user for files received via MFT)
   * @param at time
   */
  public ChannelFile(Long companyId, String uploadRef, Identity identity, String by, Instant at) {
    this.companyId = companyId;
    this.uploadRef = uploadRef;
    this.fileType = identity.fileType();
    this.fileName = identity.fileName();
    this.fileSize = identity.size();
    this.sha256 = identity.sha256();
    this.source = identity.source();
    this.uploadedBy = by;
    this.receivedAt = at;
  }

  /**
   * Refuses the file.
   *
   * @param reason reason
   */
  public void refuse(String reason) {
    this.status = REFUSED;
    this.message = cut(reason);
  }

  /**
   * Keeps the control figures of the file.
   *
   * @param count records the header announces, null without a header
   * @param total total the header or the TOTAL row announces, null without one
   * @param details total of the detail records
   * @param rows detail records read
   */
  public void controls(Integer count, BigDecimal total, BigDecimal details, int rows) {
    this.headerCount = count;
    this.headerTotal = total;
    this.detailTotal = details;
    this.rowsRead = rows;
  }

  /**
   * Links the raw copy kept read-only.
   *
   * @param id stored file
   */
  public void storedIn(Long id) {
    this.storedFileId = id;
  }

  /**
   * Records the run that processed the rows.
   *
   * @param jobId bulk run
   * @param jobNo its number
   * @param failed rows that failed
   * @param at time
   */
  public void processed(Long jobId, String jobNo, int failed, Instant at) {
    this.bulkJobId = jobId;
    this.bulkJobNo = jobNo;
    this.rowsFailed = failed;
    this.status = failed > 0 ? PARTIAL : PROCESSED;
    this.processedAt = at;
  }

  /**
   * Notes a fact of the processing (for example the reason a whole file was not processed).
   *
   * @param text text
   */
  public void note(String text) {
    this.message = cut(text);
  }

  private static String cut(String text) {
    return text == null || text.length() <= MESSAGE_MAX ? text : text.substring(0, MESSAGE_MAX);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUploadRef() {
    return uploadRef;
  }

  public String getFileType() {
    return fileType;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getSha256() {
    return sha256;
  }

  public String getSource() {
    return source;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Long getStoredFileId() {
    return storedFileId;
  }

  public Long getBulkJobId() {
    return bulkJobId;
  }

  public String getBulkJobNo() {
    return bulkJobNo;
  }

  public int getRowsRead() {
    return rowsRead;
  }

  public int getRowsFailed() {
    return rowsFailed;
  }

  public Integer getHeaderCount() {
    return headerCount;
  }

  public BigDecimal getHeaderTotal() {
    return headerTotal;
  }

  public BigDecimal getDetailTotal() {
    return detailTotal;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  /**
   * What identifies a file received.
   *
   * @param fileType payment file type
   * @param fileName name
   * @param size bytes
   * @param sha256 checksum of the content
   * @param source UPLOAD or MFT
   */
  public record Identity(
      String fileType, String fileName, long size, String sha256, String source) {}
}
