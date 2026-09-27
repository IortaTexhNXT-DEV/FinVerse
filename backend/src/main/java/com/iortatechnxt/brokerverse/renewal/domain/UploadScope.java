package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The declared scope of a renewal upload (BRD 3.004.4): the expiry range and unit a dispositioned
 * file covers and whether it is complete. Only a complete file tags the renewals of its scope that
 * it does not list Not for Renewal (Non-renewable Accounts).
 */
@Entity
@Table(name = "rnw_upload_scope")
public class UploadScope extends BaseEntity {

  /** Dispositioned file. */
  public static final String DISPOSITION = "DISPOSITION";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "job_no", nullable = false, length = 40, updatable = false)
  private String jobNo;

  @Column(nullable = false, length = 20, updatable = false)
  private String kind;

  @Column(name = "expiry_from", updatable = false)
  private LocalDate expiryFrom;

  @Column(name = "expiry_to", updatable = false)
  private LocalDate expiryTo;

  @Column(name = "unit_code", length = 20, updatable = false)
  private String unitCode;

  @Column(name = "complete_file", nullable = false, updatable = false)
  private boolean completeFile;

  @Column(name = "applied_at")
  private Instant appliedAt;

  @Column(name = "tagged_count", nullable = false)
  private int taggedCount;

  protected UploadScope() {}

  /**
   * Declares the scope of an upload.
   *
   * @param companyId company
   * @param jobNo upload job
   * @param kind upload kind
   * @param range expiry range and unit, null when none is declared
   * @param completeFile whether the file is complete for the range and unit
   */
  public UploadScope(Long companyId, String jobNo, String kind, Range range, boolean completeFile) {
    this.companyId = companyId;
    this.jobNo = jobNo;
    this.kind = kind;
    this.expiryFrom = range == null ? null : range.from();
    this.expiryTo = range == null ? null : range.to();
    this.unitCode = range == null ? null : range.unit();
    this.completeFile = completeFile;
  }

  /**
   * Records the "not in the file" tagging.
   *
   * @param tagged renewals tagged
   * @param at time
   */
  public void applied(int tagged, Instant at) {
    this.taggedCount = tagged;
    this.appliedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getJobNo() {
    return jobNo;
  }

  public String getKind() {
    return kind;
  }

  public LocalDate getExpiryFrom() {
    return expiryFrom;
  }

  public LocalDate getExpiryTo() {
    return expiryTo;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public boolean isCompleteFile() {
    return completeFile;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }

  public int getTaggedCount() {
    return taggedCount;
  }

  /**
   * Expiry range and unit of a file.
   *
   * @param from first expiry
   * @param to last expiry
   * @param unit sales unit
   */
  public record Range(LocalDate from, LocalDate to, String unit) {}
}
