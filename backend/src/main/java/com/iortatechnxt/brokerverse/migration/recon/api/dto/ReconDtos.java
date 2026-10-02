package com.iortatechnxt.brokerverse.migration.recon.api.dto;

import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the reconciliation (FR-DM-019 to FR-DM-021). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ReconDtos {

  private ReconDtos() {}

  /**
   * A reconciliation run with its lines.
   *
   * @param runNo number
   * @param objectCode object
   * @param asOf as-of date
   * @param status status
   * @param breakCount open breaks
   * @param runBy operator
   * @param runAt time
   * @param lines lines
   */
  public record RunResponse(
      String runNo,
      String objectCode,
      LocalDate asOf,
      String status,
      int breakCount,
      String runBy,
      Instant runAt,
      List<LineResponse> lines) {

    /**
     * Maps a run.
     *
     * @param r run
     * @param lines its lines
     * @return response
     */
    public static RunResponse from(MigReconRun r, List<ReconLine> lines) {
      return new RunResponse(
          r.getRunNo(),
          r.getObjectCode(),
          r.getAsOf(),
          r.getStatus().name(),
          r.getBreakCount(),
          r.getRunBy(),
          r.getRunAt(),
          lines.stream().map(LineResponse::from).toList());
    }
  }

  /**
   * A reconciliation line.
   *
   * @param id id
   * @param level level L1 to L5
   * @param measure measure
   * @param currency currency
   * @param sourceValue source value
   * @param stagedValue staged value
   * @param targetValue BIBS value
   * @param difference difference
   * @param tolerance tolerance
   * @param status MATCHED, BREAK, EXPLAINED or APPROVED
   * @param detail detail
   * @param breakReason reason code
   * @param explanation explanation
   * @param explainedBy explainer
   * @param approvedBy approver
   */
  public record LineResponse(
      Long id,
      String level,
      String measure,
      String currency,
      BigDecimal sourceValue,
      BigDecimal stagedValue,
      BigDecimal targetValue,
      BigDecimal difference,
      BigDecimal tolerance,
      String status,
      String detail,
      String breakReason,
      String explanation,
      String explainedBy,
      String approvedBy) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static LineResponse from(ReconLine l) {
      return new LineResponse(
          l.getId(),
          l.getLevel(),
          l.getMeasure(),
          l.getCurrency(),
          l.getSourceValue(),
          l.getStagedValue(),
          l.getTargetValue(),
          l.getDifference(),
          l.getTolerance(),
          l.getStatus().name(),
          l.getDetail(),
          l.getBreakReason(),
          l.getExplanation(),
          l.getExplainedBy(),
          l.getApprovedBy());
    }
  }

  /**
   * The explanation of a break.
   *
   * @param reason reason code (list of values MIG_BREAK_REASON)
   * @param text explanation
   */
  public record ExplainRequest(@NotBlank String reason, @NotBlank String text) {}
}
