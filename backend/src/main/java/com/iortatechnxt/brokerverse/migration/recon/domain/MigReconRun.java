package com.iortatechnxt.brokerverse.migration.recon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A reconciliation run of a batch or of a true-up (DATA_MIGRATION_DESIGN section 12; FR-DM-020):
 * its lines per level (L1 count, L2 amount, L3 hash, L4 field, L5 GL; TU for the true-up checks)
 * and its status: MATCHED, BREAKS while a break is open, EXPLAINED when every break has an approved
 * explanation, SIGNED at gate G5.
 */
@Entity
@Table(name = "mig_recon_run")
public class MigReconRun extends BaseEntity {

  /** Status of a run. */
  public enum Status {
    MATCHED,
    BREAKS,
    EXPLAINED,
    SIGNED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 20, updatable = false)
  private String runNo;

  @Column(name = "batch_id", updatable = false)
  private Long batchId;

  @Column(name = "trueup_id", updatable = false)
  private Long trueupId;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "as_of", nullable = false, updatable = false)
  private LocalDate asOf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.MATCHED;

  @Column(name = "break_count", nullable = false)
  private int breakCount;

  @Column(name = "run_by", nullable = false, length = 50, updatable = false)
  private String runBy;

  @Column(name = "run_at", nullable = false, updatable = false)
  private Instant runAt;

  protected MigReconRun() {}

  /**
   * A new run.
   *
   * @param companyId company
   * @param runNo number
   * @param scope batch or true-up of the run
   * @param asOf as-of date
   * @param runBy user
   * @param runAt time
   */
  public MigReconRun(
      Long companyId, String runNo, Scope scope, LocalDate asOf, String runBy, Instant runAt) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.batchId = scope.batchId();
    this.trueupId = scope.trueupId();
    this.objectCode = scope.objectCode();
    this.asOf = asOf;
    this.runBy = runBy;
    this.runAt = runAt;
  }

  /**
   * Sets the status from the open and explained breaks.
   *
   * @param open breaks without an approved explanation
   * @param explained explained breaks
   */
  public void summarise(int open, int explained) {
    this.breakCount = open;
    if (status == Status.SIGNED) {
      return;
    }
    if (open > 0) {
      this.status = Status.BREAKS;
    } else {
      this.status = explained > 0 ? Status.EXPLAINED : Status.MATCHED;
    }
  }

  /** Signed at gate G5. */
  public void sign() {
    this.status = Status.SIGNED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getTrueupId() {
    return trueupId;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public LocalDate getAsOf() {
    return asOf;
  }

  public Status getStatus() {
    return status;
  }

  public int getBreakCount() {
    return breakCount;
  }

  public String getRunBy() {
    return runBy;
  }

  public Instant getRunAt() {
    return runAt;
  }

  /**
   * What a run reconciles.
   *
   * @param batchId batch, null for a true-up
   * @param trueupId true-up, null for a batch
   * @param objectCode object
   */
  public record Scope(Long batchId, Long trueupId, String objectCode) {}
}
