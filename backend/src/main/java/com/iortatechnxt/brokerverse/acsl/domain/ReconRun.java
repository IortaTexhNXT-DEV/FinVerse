package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;

/**
 * One reconciliation of an insurer SOA against the books (ACSL 2.13.0-2.14.0), with the number of
 * lines per bucket; the upload's latest run is its reconciliation report.
 */
@Entity
@Table(name = "acsl_recon_run")
public class ReconRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "upload_id", nullable = false, updatable = false)
  private Long uploadId;

  @Column(name = "run_no", nullable = false, updatable = false)
  private int runNo;

  @Column(name = "run_at", nullable = false, updatable = false)
  private Instant runAt;

  @Column(name = "run_by", nullable = false, length = 50, updatable = false)
  private String runBy;

  @Column(nullable = false)
  private int outstanding;

  @Column(name = "for_remittance", nullable = false)
  private int forRemittance;

  @Column(nullable = false)
  private int remitted;

  @Column(nullable = false)
  private int cancelled;

  @Column(name = "direct_billed", nullable = false)
  private int directBilled;

  @Column(name = "not_found", nullable = false)
  private int notFound;

  @Column(name = "with_variance", nullable = false)
  private int withVariance;

  protected ReconRun() {}

  /**
   * Starts a run.
   *
   * @param uploadId upload
   * @param runNo run number of the upload
   * @param runAt when
   * @param runBy user
   */
  public ReconRun(Long uploadId, int runNo, Instant runAt, String runBy) {
    this.uploadId = uploadId;
    this.runNo = runNo;
    this.runAt = runAt;
    this.runBy = runBy;
  }

  /**
   * Records the counts per bucket.
   *
   * @param counts lines per bucket
   * @param variances lines with a premium or outstanding variance
   */
  public void counted(Map<ReconBucket, Integer> counts, int variances) {
    this.outstanding = counts.getOrDefault(ReconBucket.OUTSTANDING, 0);
    this.forRemittance = counts.getOrDefault(ReconBucket.FOR_REMITTANCE, 0);
    this.remitted = counts.getOrDefault(ReconBucket.REMITTED, 0);
    this.cancelled = counts.getOrDefault(ReconBucket.CANCELLED, 0);
    this.directBilled = counts.getOrDefault(ReconBucket.DIRECT_BILLED, 0);
    this.notFound = counts.getOrDefault(ReconBucket.NOT_FOUND, 0);
    this.withVariance = variances;
  }

  public Long getId() {
    return id;
  }

  public Long getUploadId() {
    return uploadId;
  }

  public int getRunNo() {
    return runNo;
  }

  public Instant getRunAt() {
    return runAt;
  }

  public String getRunBy() {
    return runBy;
  }

  public int getOutstanding() {
    return outstanding;
  }

  public int getForRemittance() {
    return forRemittance;
  }

  public int getRemitted() {
    return remitted;
  }

  public int getCancelled() {
    return cancelled;
  }

  public int getDirectBilled() {
    return directBilled;
  }

  public int getNotFound() {
    return notFound;
  }

  public int getWithVariance() {
    return withVariance;
  }
}
