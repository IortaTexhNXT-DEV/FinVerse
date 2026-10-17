package com.iortatechnxt.brokerverse.configpromo.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An import of a configuration package into this environment: options, the summary of the dry run,
 * the maker-checker decision, the apply and its snapshot.
 */
@Entity
@Table(name = "cfp_import")
public class PromotionImport extends BaseEntity {

  private static final int MAX_MESSAGE = 2000;

  @Column(name = "import_no", nullable = false, length = 30)
  private String importNo;

  @Column(name = "package_id", nullable = false)
  private Long packageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ImportStatus status;

  @Column(name = "options", nullable = false, columnDefinition = "text")
  private String options;

  @Column(name = "production", nullable = false)
  private boolean production;

  @Column(name = "pipeline", nullable = false)
  private boolean pipeline;

  @Column(name = "change_reference", length = 60)
  private String changeReference;

  @Column(name = "reason", length = 1000)
  private String reason;

  @Column(name = "compatible", nullable = false)
  private boolean compatible;

  @Column(name = "messages", nullable = false, columnDefinition = "text")
  private String messages;

  @Column(name = "blocker_count", nullable = false)
  private int blockerCount;

  @Column(name = "warning_count", nullable = false)
  private int warningCount;

  @Column(name = "added_count", nullable = false)
  private int addedCount;

  @Column(name = "changed_count", nullable = false)
  private int changedCount;

  @Column(name = "unchanged_count", nullable = false)
  private int unchangedCount;

  @Column(name = "only_in_target_count", nullable = false)
  private int onlyInTargetCount;

  @Column(name = "prepared_by", nullable = false, length = 50)
  private String preparedBy;

  @Column(name = "prepared_at", nullable = false)
  private Instant preparedAt;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 1000)
  private String decisionNote;

  @Column(name = "applied_at")
  private Instant appliedAt;

  @Column(name = "snapshot_package_id")
  private Long snapshotPackageId;

  @Column(name = "rollback_of_id")
  private Long rollbackOfId;

  @Column(name = "error_message", length = MAX_MESSAGE)
  private String errorMessage;

  protected PromotionImport() {}

  /**
   * Creates an import.
   *
   * @param importNo number
   * @param packageId package
   * @param request who, why and how
   */
  public PromotionImport(String importNo, Long packageId, ImportRequestFacts request) {
    this.importNo = importNo;
    this.packageId = packageId;
    this.status = ImportStatus.CHECKED;
    this.production = request.production();
    this.pipeline = request.pipeline();
    this.changeReference = request.changeReference();
    this.reason = request.reason();
    this.preparedBy = request.preparedBy();
    this.preparedAt = request.at();
    this.rollbackOfId = request.rollbackOfId();
    this.messages = "{}";
    this.options = "{}";
  }

  /**
   * Who, why and how of a new import.
   *
   * @param production whether this environment is production
   * @param pipeline whether a deployment pipeline prepared it
   * @param changeReference change request reference
   * @param reason reason
   * @param preparedBy maker
   * @param at time
   * @param rollbackOfId import this one rolls back, null otherwise
   */
  public record ImportRequestFacts(
      boolean production,
      boolean pipeline,
      String changeReference,
      String reason,
      String preparedBy,
      Instant at,
      Long rollbackOfId) {}

  /**
   * Numbers of items of a dry run.
   *
   * @param added added
   * @param changed changed
   * @param unchanged unchanged
   * @param onlyInTarget only in this environment
   */
  public record Counts(int added, int changed, int unchanged, int onlyInTarget) {}

  /**
   * Summary of a dry run.
   *
   * @param options options JSON
   * @param compatible whether the package fits this environment
   * @param messages refusals, blockers and warnings JSON
   * @param blockers number of blockers
   * @param warnings number of warnings
   * @param counts numbers of items
   */
  public record CheckResult(
      String options,
      boolean compatible,
      String messages,
      int blockers,
      int warnings,
      Counts counts) {}

  /**
   * Records the result of a (new) dry run; the import is open for submission again.
   *
   * @param result summary
   */
  public void checked(CheckResult result) {
    requireStatus(ImportStatus.CHECKED, ImportStatus.FAILED);
    this.status = ImportStatus.CHECKED;
    this.options = result.options();
    this.compatible = result.compatible();
    this.messages = result.messages();
    this.blockerCount = result.blockers();
    this.warningCount = result.warnings();
    this.addedCount = result.counts().added();
    this.changedCount = result.counts().changed();
    this.unchangedCount = result.counts().unchanged();
    this.onlyInTargetCount = result.counts().onlyInTarget();
    this.errorMessage = null;
  }

  /**
   * Submits the import for approval.
   *
   * @param user preparer
   * @param at time
   */
  public void submit(String user, Instant at) {
    requireStatus(ImportStatus.CHECKED);
    if (!compatible || blockerCount > 0) {
      throw new BusinessRuleException(
          "CONFIG_IMPORT_BLOCKED",
          "The import cannot be submitted while the dry run reports blockers; correct the package"
              + " or the options and check it again");
    }
    this.status = ImportStatus.SUBMITTED;
    this.submittedBy = user;
    this.submittedAt = at;
  }

  /**
   * Records the decision of the approver before the apply; the approver is never the preparer.
   *
   * @param approver approver
   * @param at time
   * @param note remarks
   */
  public void decide(String approver, Instant at, String note) {
    requireStatus(ImportStatus.SUBMITTED);
    if (approver.equalsIgnoreCase(preparedBy) || approver.equalsIgnoreCase(submittedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "A configuration import cannot be approved by the user who prepared it");
    }
    this.decidedBy = approver;
    this.decidedAt = at;
    this.decisionNote = note;
  }

  /**
   * Records the pipeline apply of its own dry run (outside production, when allowed).
   *
   * @param user pipeline user
   * @param at time
   */
  public void pipelineApproval(String user, Instant at) {
    requireStatus(ImportStatus.CHECKED);
    this.submittedBy = user;
    this.submittedAt = at;
    this.decidedBy = user;
    this.decidedAt = at;
    this.decisionNote = "Applied by the deployment pipeline outside production";
    this.status = ImportStatus.SUBMITTED;
  }

  /**
   * Marks the import applied.
   *
   * @param snapshotId snapshot package of the configuration before the import
   * @param at time
   */
  public void applied(Long snapshotId, Instant at) {
    requireStatus(ImportStatus.SUBMITTED);
    this.status = ImportStatus.APPLIED;
    this.snapshotPackageId = snapshotId;
    this.appliedAt = at;
  }

  /**
   * Marks the apply failed; nothing of it was kept.
   *
   * @param message reason
   */
  public void failed(String message) {
    this.status = ImportStatus.FAILED;
    this.errorMessage =
        message == null || message.length() <= MAX_MESSAGE
            ? message
            : message.substring(0, MAX_MESSAGE);
  }

  /**
   * Rejects the import.
   *
   * @param approver approver
   * @param at time
   * @param note reason
   */
  public void reject(String approver, Instant at, String note) {
    decide(approver, at, note);
    this.status = ImportStatus.REJECTED;
  }

  /** Withdraws the import. */
  public void cancel() {
    requireStatus(ImportStatus.CHECKED, ImportStatus.SUBMITTED, ImportStatus.FAILED);
    this.status = ImportStatus.CANCELLED;
  }

  private void requireStatus(ImportStatus... allowed) {
    for (ImportStatus s : allowed) {
      if (status == s) {
        return;
      }
    }
    throw new BusinessRuleException(
        "CONFIG_IMPORT_STATUS",
        "Import "
            + importNo
            + " is "
            + status.name().toLowerCase(java.util.Locale.ROOT)
            + "; this action is not possible now");
  }

  public String getImportNo() {
    return importNo;
  }

  public Long getPackageId() {
    return packageId;
  }

  public ImportStatus getStatus() {
    return status;
  }

  public String getOptions() {
    return options;
  }

  public boolean isProduction() {
    return production;
  }

  public boolean isPipeline() {
    return pipeline;
  }

  public String getChangeReference() {
    return changeReference;
  }

  public String getReason() {
    return reason;
  }

  public boolean isCompatible() {
    return compatible;
  }

  public String getMessages() {
    return messages;
  }

  public int getBlockerCount() {
    return blockerCount;
  }

  public int getWarningCount() {
    return warningCount;
  }

  public int getAddedCount() {
    return addedCount;
  }

  public int getChangedCount() {
    return changedCount;
  }

  public int getUnchangedCount() {
    return unchangedCount;
  }

  public int getOnlyInTargetCount() {
    return onlyInTargetCount;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public Instant getPreparedAt() {
    return preparedAt;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionNote() {
    return decisionNote;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }

  public Long getSnapshotPackageId() {
    return snapshotPackageId;
  }

  public Long getRollbackOfId() {
    return rollbackOfId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
