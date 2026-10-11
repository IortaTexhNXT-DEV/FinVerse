package com.iortatechnxt.brokerverse.screening.str.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An extraction of AML Committee-approved STRs in the AMLC layout (SNSRP-706; FR-SS-071): batch,
 * period, layout version, count, file (name, SHA-256, report archive run), whether it is a
 * re-extraction with its reason, user and time, and the STRs in the file.
 */
@Entity
@Table(name = "scr_str_extraction")
public class StrExtraction extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(name = "layout_version_id", nullable = false, updatable = false)
  private Long layoutVersionId;

  @Column(name = "str_count", nullable = false, updatable = false)
  private int strCount;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "sha256", nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(name = "report_run_id")
  private Long reportRunId;

  @Column(name = "re_extraction", nullable = false, updatable = false)
  private boolean reExtraction;

  @Column(name = "reason", length = 1000, updatable = false)
  private String reason;

  @Column(name = "extracted_by", nullable = false, length = 50, updatable = false)
  private String extractedBy;

  @Column(name = "extracted_at", nullable = false, updatable = false)
  private Instant extractedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "scr_str_extraction_item",
      joinColumns = @JoinColumn(name = "extraction_id"))
  @Column(name = "str_id")
  private Set<Long> strIds = new LinkedHashSet<>();

  /** For JPA. */
  protected StrExtraction() {}

  /**
   * Records an extraction.
   *
   * @param batchNo the batch number
   * @param companyId company
   * @param batch period, layout, file, reason and STRs
   * @param by user
   * @param at when
   */
  public StrExtraction(String batchNo, Long companyId, Batch batch, String by, Instant at) {
    this.batchNo = batchNo;
    this.companyId = companyId;
    this.periodFrom = batch.from();
    this.periodTo = batch.to();
    this.layoutVersionId = batch.layoutVersionId();
    this.strCount = batch.strIds().size();
    this.fileName = batch.fileName();
    this.sha256 = batch.sha256();
    this.reExtraction = batch.reason() != null;
    this.reason = batch.reason();
    this.strIds = new LinkedHashSet<>(batch.strIds());
    this.extractedBy = by;
    this.extractedAt = at;
  }

  /**
   * Links the archived file.
   *
   * @param runId the report archive run
   */
  public void archived(Long runId) {
    this.reportRunId = runId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public Long getLayoutVersionId() {
    return layoutVersionId;
  }

  public int getStrCount() {
    return strCount;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public Long getReportRunId() {
    return reportRunId;
  }

  public boolean isReExtraction() {
    return reExtraction;
  }

  public String getReason() {
    return reason;
  }

  public String getExtractedBy() {
    return extractedBy;
  }

  public Instant getExtractedAt() {
    return extractedAt;
  }

  public Set<Long> getStrIds() {
    return Set.copyOf(strIds);
  }

  /**
   * What an extraction holds.
   *
   * @param from period start
   * @param to period end
   * @param layoutVersionId the STR_LAYOUT version used
   * @param fileName the file name
   * @param sha256 the file's SHA-256
   * @param reason the re-extraction reason, null for a first extraction
   * @param strIds the STRs in the file
   */
  public record Batch(
      LocalDate from,
      LocalDate to,
      Long layoutVersionId,
      String fileName,
      String sha256,
      String reason,
      Set<Long> strIds) {}
}
