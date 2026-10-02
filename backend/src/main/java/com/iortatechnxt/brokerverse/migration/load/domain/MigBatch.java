package com.iortatechnxt.brokerverse.migration.load.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A migration batch (DATA_MIGRATION_DESIGN section 11; FR-DM-013 to 015): one object loaded from
 * one checked extract of each of its layouts, validated with the approved code map versions it
 * records, approved for load (gate G4) by a user other than the operator, loaded, reconciled and
 * signed off, or rolled back. Counts: staged = valid + warning + invalid before the load, and
 * loaded + skipped + rejected + excluded = staged after it.
 */
@Entity
@Table(name = "mig_batch")
public class MigBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 20, updatable = false)
  private String batchNo;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "environment_class", nullable = false, length = 20, updatable = false)
  private String environmentClass;

  @Column(name = "cutover_plan_id")
  private Long cutoverPlanId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private BatchMode mode;

  @Column(name = "parent_batch_id", updatable = false)
  private Long parentBatchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private BatchStatus status = BatchStatus.PLANNED;

  @Column(name = "staged_count", nullable = false)
  private int stagedCount;

  @Column(name = "valid_count", nullable = false)
  private int validCount;

  @Column(name = "warning_count", nullable = false)
  private int warningCount;

  @Column(name = "invalid_count", nullable = false)
  private int invalidCount;

  @Column(name = "loaded_count", nullable = false)
  private int loadedCount;

  @Column(name = "skipped_count", nullable = false)
  private int skippedCount;

  @Column(name = "rejected_count", nullable = false)
  private int rejectedCount;

  @Column(name = "excluded_count", nullable = false)
  private int excludedCount;

  @Column(name = "waived_count", nullable = false)
  private int waivedCount;

  @Column(name = "error_rate", precision = 9, scale = 4)
  private BigDecimal errorRate;

  @Column(name = "unmapped_count", nullable = false)
  private int unmappedCount;

  @Column(name = "review_count", nullable = false)
  private int reviewCount;

  @Column(name = "validated_by", length = 50)
  private String validatedBy;

  @Column(name = "validated_at")
  private Instant validatedAt;

  @Column(name = "load_approved_by", length = 50)
  private String loadApprovedBy;

  @Column(name = "load_approved_at")
  private Instant loadApprovedAt;

  @Column(name = "loaded_by", length = 50)
  private String loadedBy;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "signed_off_at")
  private Instant signedOffAt;

  @Column(name = "rollback_reason", length = 1000)
  private String rollbackReason;

  @Column(name = "rollback_requested_by", length = 50)
  private String rollbackRequestedBy;

  @Column(name = "rollback_requested_at")
  private Instant rollbackRequestedAt;

  @Column(name = "rollback_decided_by", length = 50)
  private String rollbackDecidedBy;

  @Column(name = "rollback_decided_at")
  private Instant rollbackDecidedAt;

  @Column(name = "rollback_round", nullable = false)
  private int rollbackRound;

  @Column(name = "purge_due_on")
  private LocalDate purgeDueOn;

  @Column(name = "purged_at")
  private Instant purgedAt;

  @ElementCollection
  @CollectionTable(name = "mig_batch_extract", joinColumns = @JoinColumn(name = "batch_id"))
  @Column(name = "extract_id")
  private final Set<Long> extractIds = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "mig_batch_map_version", joinColumns = @JoinColumn(name = "batch_id"))
  @MapKeyColumn(name = "set_code")
  @Column(name = "version_no")
  private final Map<String, Integer> mapVersions = new HashMap<>();

  protected MigBatch() {}

  /**
   * A planned batch.
   *
   * @param companyId company
   * @param batchNo number MGB-yyyy-nnnnn
   * @param objectCode object
   * @param mode FULL, DELTA or RERUN
   * @param parentBatchId parent of a rerun
   * @param environmentClass environment class
   */
  public MigBatch(
      Long companyId,
      String batchNo,
      String objectCode,
      BatchMode mode,
      Long parentBatchId,
      String environmentClass) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.objectCode = objectCode;
    this.mode = mode;
    this.parentBatchId = parentBatchId;
    this.environmentClass = environmentClass;
  }

  /**
   * Refuses the action unless the batch is in one of the statuses.
   *
   * @param action action named in the message
   * @param allowed statuses
   */
  public void requireStatus(String action, BatchStatus... allowed) {
    for (BatchStatus s : allowed) {
      if (status == s) {
        return;
      }
    }
    throw new BusinessRuleException(
        "MIG_BATCH_STATUS", "Batch " + batchNo + " is " + status + "; it cannot be " + action);
  }

  /**
   * The validation result.
   *
   * @param counts counts after validation
   * @param rate error rate in percent
   * @param user operator
   * @param when time
   */
  public void validated(Counts counts, BigDecimal rate, String user, Instant when) {
    requireStatus("validated", BatchStatus.PLANNED, BatchStatus.VALIDATED);
    applyCounts(counts);
    this.errorRate = rate;
    this.validatedBy = user;
    this.validatedAt = when;
    this.status = BatchStatus.VALIDATED;
  }

  /**
   * Unmapped codes and pairs waiting in the client review queue at validation.
   *
   * @param unmapped unmapped legacy codes
   * @param review client pairs to review
   */
  public void validationFindings(int unmapped, int review) {
    this.unmappedCount = unmapped;
    this.reviewCount = review;
  }

  /**
   * Updates the counts (waivers, exclusions, load progress).
   *
   * @param counts counts
   * @param rate error rate
   */
  public void recount(Counts counts, BigDecimal rate) {
    applyCounts(counts);
    this.errorRate = rate;
  }

  private void applyCounts(Counts c) {
    this.stagedCount = c.staged();
    this.validCount = c.valid();
    this.warningCount = c.warning();
    this.invalidCount = c.invalid();
    this.loadedCount = c.loaded();
    this.skippedCount = c.skipped();
    this.rejectedCount = c.rejected();
    this.excludedCount = c.excluded();
    this.waivedCount = c.waived();
  }

  /**
   * Approves the load (gate G4). The approver is not the operator who validated the batch.
   *
   * @param user Data Migration Lead
   * @param when time
   */
  public void approveLoad(String user, Instant when) {
    requireStatus("approved for load", BatchStatus.VALIDATED);
    if (CurrentUser.sameUser(user, validatedBy) || CurrentUser.sameUser(user, getCreatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.loadApprovedBy = user;
    this.loadApprovedAt = when;
    this.status = BatchStatus.APPROVED;
  }

  /**
   * The load starts.
   *
   * @param user operator
   * @param when time
   */
  public void startLoad(String user, Instant when) {
    requireStatus("loaded", BatchStatus.APPROVED, BatchStatus.LOADING);
    if (CurrentUser.sameUser(user, loadApprovedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.loadedBy = user;
    this.startedAt = startedAt == null ? when : startedAt;
    this.status = BatchStatus.LOADING;
  }

  /**
   * The load ended.
   *
   * @param failed the load stopped on an error
   * @param when time
   */
  public void endLoad(boolean failed, Instant when) {
    this.endedAt = when;
    if (failed) {
      this.status = BatchStatus.FAILED;
    } else {
      this.status = rejectedCount > 0 ? BatchStatus.LOADED_WITH_REJECTS : BatchStatus.LOADED;
    }
  }

  /** The reconciliation of the batch has no open break. */
  public void reconciled() {
    if (status == BatchStatus.LOADED || status == BatchStatus.LOADED_WITH_REJECTS) {
      this.status = BatchStatus.RECONCILED;
    }
  }

  /**
   * Accepted at gate G6.
   *
   * @param when time
   * @param purgeDue date the staging data is purged
   */
  public void signedOff(Instant when, LocalDate purgeDue) {
    this.status = BatchStatus.SIGNED_OFF;
    this.signedOffAt = when;
    this.purgeDueOn = purgeDue;
  }

  /**
   * A rollback is requested.
   *
   * @param reason reason
   * @param user Data Migration Lead
   * @param when time
   * @return the round of the request (workflow key)
   */
  public int requestRollback(String reason, String user, Instant when) {
    if (!status.holdsLoad() || status == BatchStatus.ROLLBACK_REQUESTED) {
      throw new BusinessRuleException(
          "MIG_BATCH_STATUS", "Batch " + batchNo + " is " + status + "; it cannot be rolled back");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the rollback");
    }
    this.rollbackReason = reason.strip();
    this.rollbackRequestedBy = user;
    this.rollbackRequestedAt = when;
    this.rollbackRound++;
    this.status = BatchStatus.ROLLBACK_REQUESTED;
    return rollbackRound;
  }

  /**
   * The approver decided the rollback request.
   *
   * @param approve approved
   * @param user approver (not the requester)
   * @param when time
   * @param statusBefore status to go back to on a rejection
   */
  public void decideRollback(boolean approve, String user, Instant when, BatchStatus statusBefore) {
    requireStatus("decided", BatchStatus.ROLLBACK_REQUESTED);
    if (CurrentUser.sameUser(user, rollbackRequestedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.rollbackDecidedBy = user;
    this.rollbackDecidedAt = when;
    this.status = approve ? BatchStatus.ROLLING_BACK : statusBefore;
  }

  /**
   * The rollback finished.
   *
   * @param when time
   * @param purgeDue date the staging data is purged
   */
  public void rolledBack(Instant when, LocalDate purgeDue) {
    this.status = BatchStatus.ROLLED_BACK;
    this.endedAt = when;
    this.purgeDueOn = purgeDue;
  }

  /**
   * Staging payloads purged.
   *
   * @param when time
   */
  public void purged(Instant when) {
    this.purgedAt = when;
  }

  /**
   * Records the code map versions the validation used.
   *
   * @param versions set code to version number
   */
  public void useMapVersions(Map<String, Integer> versions) {
    mapVersions.clear();
    mapVersions.putAll(versions);
  }

  /**
   * Adds an extract to the batch.
   *
   * @param extractId extract
   */
  public void addExtract(Long extractId) {
    extractIds.add(extractId);
  }

  public void setCutoverPlanId(Long cutoverPlanId) {
    this.cutoverPlanId = cutoverPlanId;
  }

  /**
   * The counts of the batch.
   *
   * @return counts
   */
  public Counts counts() {
    return new Counts(
        stagedCount,
        validCount,
        warningCount,
        invalidCount,
        loadedCount,
        skippedCount,
        rejectedCount,
        excludedCount,
        waivedCount);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getEnvironmentClass() {
    return environmentClass;
  }

  public Long getCutoverPlanId() {
    return cutoverPlanId;
  }

  public BatchMode getMode() {
    return mode;
  }

  public Long getParentBatchId() {
    return parentBatchId;
  }

  public BatchStatus getStatus() {
    return status;
  }

  public BigDecimal getErrorRate() {
    return errorRate;
  }

  public int getUnmappedCount() {
    return unmappedCount;
  }

  public int getReviewCount() {
    return reviewCount;
  }

  public String getValidatedBy() {
    return validatedBy;
  }

  public Instant getValidatedAt() {
    return validatedAt;
  }

  public String getLoadApprovedBy() {
    return loadApprovedBy;
  }

  public Instant getLoadApprovedAt() {
    return loadApprovedAt;
  }

  public String getLoadedBy() {
    return loadedBy;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public Instant getSignedOffAt() {
    return signedOffAt;
  }

  public String getRollbackReason() {
    return rollbackReason;
  }

  public String getRollbackRequestedBy() {
    return rollbackRequestedBy;
  }

  public Instant getRollbackRequestedAt() {
    return rollbackRequestedAt;
  }

  public String getRollbackDecidedBy() {
    return rollbackDecidedBy;
  }

  public Instant getRollbackDecidedAt() {
    return rollbackDecidedAt;
  }

  public int getRollbackRound() {
    return rollbackRound;
  }

  public LocalDate getPurgeDueOn() {
    return purgeDueOn;
  }

  public Instant getPurgedAt() {
    return purgedAt;
  }

  public Set<Long> getExtractIds() {
    return Set.copyOf(extractIds);
  }

  public Map<String, Integer> getMapVersions() {
    return Map.copyOf(mapVersions);
  }

  /**
   * Row counts of a batch.
   *
   * @param staged rows in the batch
   * @param valid valid rows
   * @param warning rows with warnings
   * @param invalid rows with errors
   * @param loaded loaded rows
   * @param skipped rows already loaded and unchanged
   * @param rejected rows refused by the owning service
   * @param excluded rows excluded by the data owner
   * @param waived rows with waived errors
   */
  public record Counts(
      int staged,
      int valid,
      int warning,
      int invalid,
      int loaded,
      int skipped,
      int rejected,
      int excluded,
      int waived) {}
}
