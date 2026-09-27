package com.iortatechnxt.brokerverse.migration.load.api.dto;

import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchLog;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.Resubmission;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the batches (FR-DM-013 to FR-DM-015). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class BatchDtos {

  private BatchDtos() {}

  /**
   * A batch to plan.
   *
   * @param objectCode object
   * @param extractNos extracts, empty for the latest staged extract of each layout
   */
  public record PlanRequest(@NotBlank String objectCode, List<String> extractNos) {}

  /**
   * A batch.
   *
   * @param batchNo number
   * @param objectCode object
   * @param mode mode
   * @param parentBatchNo parent of a rerun
   * @param status status
   * @param environmentClass environment class
   * @param counts row counts
   * @param errorRate error rate in percent
   * @param unmappedCount unmapped codes
   * @param reviewCount client pairs to review
   * @param mapVersions code map versions used
   * @param extractNos extracts
   * @param validatedBy operator of the validation
   * @param validatedAt time
   * @param loadApprovedBy approver of the load
   * @param loadApprovedAt time
   * @param loadedBy operator of the load
   * @param startedAt load start
   * @param endedAt load end
   * @param signedOffAt acceptance time
   * @param rollbackReason rollback reason
   * @param rollbackRequestedBy requester
   * @param purgeDueOn purge date
   * @param purgedAt purge time
   * @param createdBy creator
   * @param createdAt time
   */
  public record BatchResponse(
      String batchNo,
      String objectCode,
      String mode,
      String parentBatchNo,
      String status,
      String environmentClass,
      MigBatch.Counts counts,
      BigDecimal errorRate,
      int unmappedCount,
      int reviewCount,
      Map<String, Integer> mapVersions,
      List<String> extractNos,
      String validatedBy,
      Instant validatedAt,
      String loadApprovedBy,
      Instant loadApprovedAt,
      String loadedBy,
      Instant startedAt,
      Instant endedAt,
      Instant signedOffAt,
      String rollbackReason,
      String rollbackRequestedBy,
      LocalDate purgeDueOn,
      Instant purgedAt,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a batch.
     *
     * @param b batch
     * @param parentNo parent batch number
     * @param extractNos extract numbers
     * @return response
     */
    public static BatchResponse from(MigBatch b, String parentNo, List<String> extractNos) {
      return new BatchResponse(
          b.getBatchNo(),
          b.getObjectCode(),
          b.getMode().name(),
          parentNo,
          b.getStatus().name(),
          b.getEnvironmentClass(),
          b.counts(),
          b.getErrorRate(),
          b.getUnmappedCount(),
          b.getReviewCount(),
          b.getMapVersions(),
          extractNos,
          b.getValidatedBy(),
          b.getValidatedAt(),
          b.getLoadApprovedBy(),
          b.getLoadApprovedAt(),
          b.getLoadedBy(),
          b.getStartedAt(),
          b.getEndedAt(),
          b.getSignedOffAt(),
          b.getRollbackReason(),
          b.getRollbackRequestedBy(),
          b.getPurgeDueOn(),
          b.getPurgedAt(),
          b.getCreatedBy(),
          b.getCreatedAt());
    }
  }

  /**
   * A line of the run log.
   *
   * @param step step
   * @param level level
   * @param message message
   * @param counts counts
   * @param loggedBy user
   * @param loggedAt time
   */
  public record LogResponse(
      String step, String level, String message, String counts, String loggedBy, Instant loggedAt) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static LogResponse from(BatchLog l) {
      return new LogResponse(
          l.getStep(),
          l.getLevel(),
          l.getMessage(),
          l.getCounts(),
          l.getLoggedBy(),
          l.getLoggedAt());
    }
  }

  /**
   * A staged row.
   *
   * @param id id
   * @param layoutCode layout
   * @param rowNo row number
   * @param legacyKey legacy key
   * @param status status
   * @param targetEntity target entity
   * @param targetCode target code
   * @param message message
   * @param values values (mapped, or raw before validation)
   */
  public record RowResponse(
      Long id,
      String layoutCode,
      int rowNo,
      String legacyKey,
      String status,
      String targetEntity,
      String targetCode,
      String message,
      Map<String, String> values) {

    /**
     * Maps a row.
     *
     * @param r row
     * @return response
     */
    public static RowResponse from(StageRow r) {
      return new RowResponse(
          r.getId(),
          r.getLayoutCode(),
          r.getRowNo(),
          r.getLegacyKey(),
          r.getStatus().name(),
          r.getTargetEntity(),
          r.getTargetCode(),
          r.getMessage(),
          r.getMappedPayload().isEmpty() ? r.getRawPayload() : r.getMappedPayload());
    }
  }

  /**
   * An issue.
   *
   * @param id id
   * @param rowId row
   * @param ruleCode rule
   * @param severity severity
   * @param field column
   * @param value value
   * @param message message
   * @param resolution resolution
   * @param waiverReason reason
   * @param resolutionNote note
   * @param resolvedBy user
   * @param resolvedAt time
   */
  public record IssueResponse(
      Long id,
      Long rowId,
      String ruleCode,
      String severity,
      String field,
      String value,
      String message,
      String resolution,
      String waiverReason,
      String resolutionNote,
      String resolvedBy,
      Instant resolvedAt) {

    /**
     * Maps an issue.
     *
     * @param i issue
     * @return response
     */
    public static IssueResponse from(MigIssue i) {
      return new IssueResponse(
          i.getId(),
          i.getStageRowId(),
          i.getRuleCode(),
          i.getSeverity().name(),
          i.getField(),
          i.getValue(),
          i.getMessage(),
          i.getResolution().name(),
          i.getWaiverReason(),
          i.getResolutionNote(),
          i.getResolvedBy(),
          i.getResolvedAt());
    }
  }

  /**
   * Rows waived or excluded by the data owner.
   *
   * @param rowIds rows
   * @param reason reason code (list MIG_WAIVER_REASON)
   * @param note remarks or manual-entry plan
   */
  public record RowsRequest(
      @NotEmpty List<Long> rowIds, @NotBlank String reason, @NotBlank String note) {}

  /**
   * Resolution of an issue by the Data Steward.
   *
   * @param resolution FIXED_AT_SOURCE or MAPPED
   * @param note note
   */
  public record ResolveRequest(MigIssue.Resolution resolution, String note) {}

  /**
   * A gate sign-off.
   *
   * @param approve approve or reject
   * @param role role signed in (G6)
   * @param comment comment
   */
  public record SignRequest(boolean approve, String role, String comment) {}

  /**
   * A sign-off.
   *
   * @param id id
   * @param objectCode object
   * @param batchId batch
   * @param gate gate
   * @param gateLabel gate label
   * @param roleCode role
   * @param username user
   * @param decision decision
   * @param comment comment
   * @param evidenceName evidence file
   * @param evidenceFileId evidence file id
   * @param signedAt time
   */
  public record SignoffResponse(
      Long id,
      String objectCode,
      Long batchId,
      String gate,
      String gateLabel,
      String roleCode,
      String username,
      String decision,
      String comment,
      String evidenceName,
      Long evidenceFileId,
      Instant signedAt) {

    /**
     * Maps a sign-off.
     *
     * @param s sign-off
     * @return response
     */
    public static SignoffResponse from(MigSignoff s) {
      return new SignoffResponse(
          s.getId(),
          s.getObjectCode(),
          s.getBatchId(),
          s.getGate().name(),
          s.getGate().label(),
          s.getRoleCode(),
          s.getUsername(),
          s.getDecision().name(),
          s.getComment(),
          s.getEvidenceName(),
          s.getEvidenceFileId(),
          s.getSignedAt());
    }
  }

  /**
   * A resubmission to prepare.
   *
   * @param extractNo corrected extract
   */
  public record ResubmitRequest(@NotBlank String extractNo) {}

  /**
   * A checker's decision on a resubmission.
   *
   * @param approve approve or return
   * @param note note
   */
  public record DecideRequest(boolean approve, String note) {}

  /**
   * A resubmission.
   *
   * @param resubmissionNo number
   * @param objectCode object
   * @param status status
   * @param correctedRows corrected rows
   * @param preparedBy maker
   * @param preparedAt time
   * @param decidedBy checker
   * @param decidedAt time
   * @param decisionNote note
   */
  public record ResubmissionResponse(
      String resubmissionNo,
      String objectCode,
      String status,
      int correctedRows,
      String preparedBy,
      Instant preparedAt,
      String decidedBy,
      Instant decidedAt,
      String decisionNote) {

    /**
     * Maps a resubmission.
     *
     * @param r resubmission
     * @return response
     */
    public static ResubmissionResponse from(Resubmission r) {
      return new ResubmissionResponse(
          r.getResubmissionNo(),
          r.getObjectCode(),
          r.getStatus().name(),
          r.getCorrectedRows(),
          r.getPreparedBy(),
          r.getPreparedAt(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getDecisionNote());
    }
  }
}
