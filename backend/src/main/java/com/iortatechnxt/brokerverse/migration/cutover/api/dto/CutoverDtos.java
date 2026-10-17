package com.iortatechnxt.brokerverse.migration.cutover.api.dto;

import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverPlan;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTask;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItem;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoCriterion;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoDecision;
import com.iortatechnxt.brokerverse.migration.cutover.domain.RunoffCohort;
import com.iortatechnxt.brokerverse.migration.cutover.service.CutoverService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Responses of the cutover, run-off and decommissioning screens. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CutoverDtos {

  private CutoverDtos() {}

  /**
   * A cutover plan.
   *
   * @param planNo number
   * @param name name
   * @param kind MOCK, DRESS_REHEARSAL or PRODUCTION
   * @param mockNo mock number
   * @param environment environment
   * @param goLiveDate go-live date
   * @param freezeStart freeze start
   * @param freezeEnd freeze end
   * @param status status
   */
  public record PlanResponse(
      String planNo,
      String name,
      String kind,
      Integer mockNo,
      String environment,
      LocalDate goLiveDate,
      LocalDateTime freezeStart,
      LocalDateTime freezeEnd,
      String status) {

    /**
     * Maps a plan.
     *
     * @param p plan
     * @return response
     */
    public static PlanResponse from(CutoverPlan p) {
      return new PlanResponse(
          p.getPlanNo(),
          p.getName(),
          p.getKind().name(),
          p.getMockNo(),
          p.getEnvironment(),
          p.getGoLiveDate(),
          p.getFreezeStart(),
          p.getFreezeEnd(),
          p.getStatus().name());
    }
  }

  /**
   * A task of the runbook.
   *
   * @param seq sequence
   * @param phase phase
   * @param task task
   * @param ownerRole owner role
   * @param objectCode object it loads
   * @param dependsOn sequences it depends on
   * @param plannedStart planned start
   * @param plannedEnd planned end
   * @param actualStart actual start
   * @param actualEnd actual end
   * @param status status
   * @param remarks remarks
   * @param updatedBy last updated by
   */
  public record TaskResponse(
      int seq,
      String phase,
      String task,
      String ownerRole,
      String objectCode,
      String dependsOn,
      LocalDateTime plannedStart,
      LocalDateTime plannedEnd,
      LocalDateTime actualStart,
      LocalDateTime actualEnd,
      String status,
      String remarks,
      String updatedBy) {

    /**
     * Maps a task.
     *
     * @param t task
     * @return response
     */
    public static TaskResponse from(CutoverTask t) {
      return new TaskResponse(
          t.getSeq(),
          t.getPhase(),
          t.getTask(),
          t.getOwnerRole(),
          t.getObjectCode(),
          t.getDependsOn(),
          t.getPlannedStart(),
          t.getPlannedEnd(),
          t.getActualStart(),
          t.getActualEnd(),
          t.getStatus().name(),
          t.getRemarks(),
          t.getUpdatedBy());
    }
  }

  /**
   * A go / no-go criterion.
   *
   * @param criterionNo number
   * @param name name
   * @param threshold threshold
   * @param manual recorded by the lead (not measured)
   * @param measuredValue measured value
   * @param met threshold met, null before the first measurement
   * @param note evidence
   * @param measuredBy measured by
   * @param measuredAt measured at
   */
  public record CriterionResponse(
      int criterionNo,
      String name,
      String threshold,
      boolean manual,
      String measuredValue,
      Boolean met,
      String note,
      String measuredBy,
      Instant measuredAt) {

    /**
     * Maps a criterion.
     *
     * @param c criterion
     * @return response
     */
    public static CriterionResponse from(GonogoCriterion c) {
      return new CriterionResponse(
          c.getCriterionNo(),
          c.getName(),
          c.getThreshold(),
          c.manual(),
          c.getMeasuredValue(),
          c.getMet(),
          c.getManualNote(),
          c.getMeasuredBy(),
          c.getMeasuredAt());
    }
  }

  /**
   * A go / no-go decision.
   *
   * @param decision GO or NO_GO
   * @param comment comment
   * @param criteriaMet criteria met
   * @param criteriaTotal criteria
   * @param decidedBy board member
   * @param decidedAt time
   */
  public record DecisionResponse(
      String decision,
      String comment,
      int criteriaMet,
      int criteriaTotal,
      String decidedBy,
      Instant decidedAt) {

    /**
     * Maps a decision.
     *
     * @param d decision
     * @return response
     */
    public static DecisionResponse from(GonogoDecision d) {
      return new DecisionResponse(
          d.getDecision(),
          d.getComment(),
          d.getCriteriaMet(),
          d.getCriteriaTotal(),
          d.getDecidedBy(),
          d.getDecidedAt());
    }
  }

  /**
   * A plan with its runbook, criteria and decisions.
   *
   * @param plan plan
   * @param tasks tasks
   * @param criteria criteria
   * @param decisions decisions, newest first
   */
  public record PlanDetail(
      PlanResponse plan,
      List<TaskResponse> tasks,
      List<CriterionResponse> criteria,
      List<DecisionResponse> decisions) {

    /**
     * Maps a plan view.
     *
     * @param v view
     * @return response
     */
    public static PlanDetail from(CutoverService.PlanView v) {
      return new PlanDetail(
          PlanResponse.from(v.plan()),
          v.tasks().stream().map(TaskResponse::from).toList(),
          v.criteria().stream().map(CriterionResponse::from).toList(),
          v.decisions().stream().map(DecisionResponse::from).toList());
    }
  }

  /**
   * A run-off cohort.
   *
   * @param snapshotDate snapshot
   * @param expiryMonth expiry month
   * @param sourceSystem source system
   * @param headersInForce headers in force
   * @param premiumInForce premium
   * @param renewed renewed
   * @param notRenewed not renewed
   * @param lapsed lapsed
   * @param stillOpen still open
   */
  public record CohortResponse(
      LocalDate snapshotDate,
      LocalDate expiryMonth,
      String sourceSystem,
      int headersInForce,
      BigDecimal premiumInForce,
      int renewed,
      int notRenewed,
      int lapsed,
      int stillOpen) {

    /**
     * Maps a cohort.
     *
     * @param c cohort
     * @return response
     */
    public static CohortResponse from(RunoffCohort c) {
      return new CohortResponse(
          c.getSnapshotDate(),
          c.getExpiryMonth(),
          c.getSourceSystem(),
          c.getHeadersInForce(),
          c.getPremiumInForce(),
          c.getRenewed(),
          c.getNotRenewed(),
          c.getLapsed(),
          c.getStillOpen());
    }
  }

  /**
   * A decommissioning criterion.
   *
   * @param id id
   * @param systemCode legacy system, LEGACY for the legacy context
   * @param milestone SYSTEM or CONTEXT
   * @param criterion criterion code
   * @param name name
   * @param description description
   * @param evidence evidence
   * @param status OPEN, MET, SIGNED or NOT_APPLICABLE
   * @param signedBy signed by
   * @param signedAt signed at
   */
  public record DecommissionResponse(
      Long id,
      String systemCode,
      String milestone,
      String criterion,
      String name,
      String description,
      String evidence,
      String status,
      String signedBy,
      Instant signedAt) {

    /**
     * Maps an item.
     *
     * @param d item
     * @return response
     */
    public static DecommissionResponse from(DecommissionItem d) {
      return new DecommissionResponse(
          d.getId(),
          d.getSystemCode(),
          d.getMilestone(),
          d.getCriterion(),
          d.getName(),
          d.getDescription(),
          d.getEvidence(),
          d.getStatus().name(),
          d.getSignedBy(),
          d.getSignedAt());
    }
  }
}
