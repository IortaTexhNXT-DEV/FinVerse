package com.iortatechnxt.brokerverse.report.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Several reports run together with shared parameters and downloaded as one file (FRBS 2.4.5 /
 * 2.4.7): a ZIP of the individual files, or one merged PDF. A failed report does not stop the
 * batch; its item records the reason and the batch ends PARTIAL.
 */
@Entity
@Table(name = "report_batch")
public class ReportBatch extends BaseEntity {

  /** Status while running. */
  public static final String RUNNING = "RUNNING";

  @Column(name = "batch_no", nullable = false, length = 30)
  private String batchNo;

  @Column(name = "company_id")
  private Long companyId;

  @Column(nullable = false, length = 10)
  private String format;

  @Column(name = "merged_pdf", nullable = false)
  private boolean mergedPdf;

  @Column(nullable = false, length = 10)
  private String paper;

  @Column(nullable = false, length = 10)
  private String orientation;

  @Column(name = "fit_to_width", nullable = false)
  private boolean fitToWidth;

  @Column(length = 2000)
  private String parameters;

  @Column(nullable = false, length = 15)
  private String status = RUNNING;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "content_type", length = 100)
  private String contentType;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "content")
  private byte[] content;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "completed_at")
  private Instant completedAt;

  @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("itemNo")
  private final List<ReportBatchItem> items = new ArrayList<>();

  protected ReportBatch() {}

  /**
   * Starts a batch.
   *
   * @param batchNo batch number
   * @param companyId company of the shared parameters, may be null
   * @param spec format and print options
   * @param parameters shared parameters as text
   */
  public ReportBatch(String batchNo, Long companyId, BatchSpec spec, String parameters) {
    this.batchNo = batchNo;
    this.companyId = companyId;
    this.format = spec.format();
    this.mergedPdf = spec.mergedPdf();
    this.paper = spec.paper();
    this.orientation = spec.orientation();
    this.fitToWidth = spec.fitToWidth();
    this.parameters = parameters;
  }

  /**
   * Records the outcome of one report.
   *
   * @param reportCode report
   * @param ok whether it was produced
   * @param rowCount rows
   * @param error reason of a failure
   */
  public void addItem(String reportCode, boolean ok, int rowCount, String error) {
    items.add(new ReportBatchItem(this, items.size() + 1, reportCode, ok, rowCount, error));
  }

  /**
   * Stores the file and closes the batch: COMPLETED, PARTIAL or FAILED.
   *
   * @param name file name
   * @param type content type
   * @param bytes content, null when nothing was produced
   * @param when completion time
   */
  public void complete(String name, String type, byte[] bytes, Instant when) {
    long ok = items.stream().filter(ReportBatchItem::isOk).count();
    if (ok == items.size()) {
      this.status = "COMPLETED";
    } else {
      this.status = ok == 0 ? "FAILED" : "PARTIAL";
    }
    this.fileName = bytes == null ? null : name;
    this.contentType = bytes == null ? null : type;
    this.content = bytes == null ? null : bytes.clone();
    this.sizeBytes = bytes == null ? null : (long) bytes.length;
    this.completedAt = when;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFormat() {
    return format;
  }

  public boolean isMergedPdf() {
    return mergedPdf;
  }

  public String getPaper() {
    return paper;
  }

  public String getOrientation() {
    return orientation;
  }

  public boolean isFitToWidth() {
    return fitToWidth;
  }

  public String getParameters() {
    return parameters;
  }

  public String getStatus() {
    return status;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * File content.
   *
   * @return copy of the content, null when none
   */
  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public List<ReportBatchItem> getItems() {
    return List.copyOf(items);
  }

  /**
   * Format and print options of a batch.
   *
   * @param format export format name
   * @param mergedPdf one merged PDF instead of a ZIP
   * @param paper paper size
   * @param orientation orientation
   * @param fitToWidth fit tables to the page width
   */
  public record BatchSpec(
      String format, boolean mergedPdf, String paper, String orientation, boolean fitToWidth) {}
}
