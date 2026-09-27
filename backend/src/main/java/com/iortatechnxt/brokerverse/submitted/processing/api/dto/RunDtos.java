package com.iortatechnxt.brokerverse.submitted.processing.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheck;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResult;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Records of the processing run API (FRS FR-SP-021, 022, 050). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class RunDtos {

  private RunDtos() {}

  /**
   * A run.
   *
   * @param id run id
   * @param runNo run number
   * @param trigger trigger
   * @param scope scope
   * @param startedAt start
   * @param finishedAt end
   * @param startedBy user or SYSTEM
   * @param total records
   * @param passed passed without bucket
   * @param bucketed bucketed
   * @param fallout in fallout
   * @param overridden overridden by a manual tag
   * @param breaches above an insurer limit
   */
  public record RunView(
      Long id,
      String runNo,
      String trigger,
      String scope,
      Instant startedAt,
      Instant finishedAt,
      String startedBy,
      int total,
      int passed,
      int bucketed,
      int fallout,
      int overridden,
      int breaches) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return view
     */
    public static RunView from(SbmRun r) {
      return new RunView(
          r.getId(),
          r.getRunNo(),
          r.getTrigger().name(),
          r.getScope(),
          r.getStartedAt(),
          r.getFinishedAt(),
          r.getCreatedBy(),
          r.getTotal(),
          r.getPassed(),
          r.getBucketed(),
          r.getFallout(),
          r.getOverridden(),
          r.getBreaches());
    }
  }

  /**
   * A result row.
   *
   * @param id result id
   * @param runId run
   * @param policyId record
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param step step
   * @param outcome outcome
   * @param bucket bucket code
   * @param reasonCode reason code
   * @param ruleName rule
   * @param ruleSetCode rule set
   * @param ruleSetVersion rule set version
   * @param message message
   * @param at time
   */
  public record ResultView(
      Long id,
      Long runId,
      Long policyId,
      String sbmNo,
      String assuredName,
      String step,
      String outcome,
      String bucket,
      String reasonCode,
      String ruleName,
      String ruleSetCode,
      Integer ruleSetVersion,
      String message,
      Instant at) {

    /**
     * Maps a result.
     *
     * @param r result
     * @param sbmNo masterlist number
     * @param assuredName assured
     * @return view
     */
    public static ResultView from(SbmRunResult r, String sbmNo, String assuredName) {
      return new ResultView(
          r.getId(),
          r.getRunId(),
          r.getPolicyId(),
          sbmNo,
          assuredName,
          r.getStep().name(),
          r.getOutcome().name(),
          r.getBucket(),
          r.getReasonCode(),
          r.getRuleName(),
          r.getRuleSetCode(),
          r.getRuleSetVersion(),
          r.getMessage(),
          r.getCreatedAt());
    }
  }

  /**
   * A limit check.
   *
   * @param id check id
   * @param runId run
   * @param attribute what is limited
   * @param limit limit
   * @param value value of the record
   * @param breached whether it is above the limit
   * @param at time
   */
  public record LimitCheckView(
      Long id,
      Long runId,
      String attribute,
      String limit,
      String value,
      boolean breached,
      Instant at) {

    /**
     * Maps a check.
     *
     * @param c check
     * @return view
     */
    public static LimitCheckView from(SbmLimitCheck c) {
      return new LimitCheckView(
          c.getId(),
          c.getRunId(),
          c.getAttribute(),
          c.getLimitValue(),
          c.getActualValue(),
          c.isBreached(),
          c.getCreatedAt());
    }
  }

  /**
   * The rule results and limit checks of a record.
   *
   * @param results results, newest first
   * @param limitChecks checks, newest first
   */
  public record PolicyResults(List<ResultView> results, List<LimitCheckView> limitChecks) {}

  /**
   * A run to start by hand.
   *
   * @param companyId company
   * @param policyIds records, empty for every record not yet in the renewal
   */
  public record RunRequest(@NotNull Long companyId, List<Long> policyIds) {}
}
