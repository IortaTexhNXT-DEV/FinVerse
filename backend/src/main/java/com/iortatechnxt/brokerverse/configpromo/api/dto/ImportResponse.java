package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews.Messages;
import java.time.Instant;

/**
 * A configuration import.
 *
 * @param id id
 * @param importNo number
 * @param packageId package
 * @param packageNo package number
 * @param sourceEnvironment environment of the package
 * @param status status
 * @param production whether this environment is production
 * @param pipeline whether a pipeline prepared it
 * @param changeReference change request
 * @param reason reason
 * @param compatible whether the package fits this environment
 * @param blockerCount blockers
 * @param warningCount warnings
 * @param added items added
 * @param changed items changed
 * @param unchanged items unchanged
 * @param onlyInTarget items only in this environment
 * @param preparedBy maker
 * @param preparedAt time
 * @param submittedBy submitter
 * @param submittedAt time
 * @param decidedBy approver
 * @param decidedAt time
 * @param decisionNote remarks of the approver
 * @param appliedAt time of the apply
 * @param snapshotPackageId snapshot taken before the apply
 * @param rollbackOfId import this one rolls back
 * @param errorMessage why the apply failed
 * @param options choices
 * @param messages findings
 */
public record ImportResponse(
    Long id,
    String importNo,
    Long packageId,
    String packageNo,
    String sourceEnvironment,
    String status,
    boolean production,
    boolean pipeline,
    String changeReference,
    String reason,
    boolean compatible,
    int blockerCount,
    int warningCount,
    int added,
    int changed,
    int unchanged,
    int onlyInTarget,
    String preparedBy,
    Instant preparedAt,
    String submittedBy,
    Instant submittedAt,
    String decidedBy,
    Instant decidedAt,
    String decisionNote,
    Instant appliedAt,
    Long snapshotPackageId,
    Long rollbackOfId,
    String errorMessage,
    ImportOptions options,
    Messages messages) {

  /**
   * Maps an import.
   *
   * @param i import
   * @param packageNo number of its package
   * @param sourceEnvironment environment of its package
   * @param options choices
   * @param messages findings
   * @return response
   */
  public static ImportResponse from(
      PromotionImport i,
      String packageNo,
      String sourceEnvironment,
      ImportOptions options,
      Messages messages) {
    return new ImportResponse(
        i.getId(),
        i.getImportNo(),
        i.getPackageId(),
        packageNo,
        sourceEnvironment,
        i.getStatus().name(),
        i.isProduction(),
        i.isPipeline(),
        i.getChangeReference(),
        i.getReason(),
        i.isCompatible(),
        i.getBlockerCount(),
        i.getWarningCount(),
        i.getAddedCount(),
        i.getChangedCount(),
        i.getUnchangedCount(),
        i.getOnlyInTargetCount(),
        i.getPreparedBy(),
        i.getPreparedAt(),
        i.getSubmittedBy(),
        i.getSubmittedAt(),
        i.getDecidedBy(),
        i.getDecidedAt(),
        i.getDecisionNote(),
        i.getAppliedAt(),
        i.getSnapshotPackageId(),
        i.getRollbackOfId(),
        i.getErrorMessage(),
        options,
        messages);
  }
}
