package com.iortatechnxt.brokerverse.submitted.setup.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleCondition;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleOutcome;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Records of the rule set API (FRS FR-SP-020). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class RuleSetDtos {

  private RuleSetDtos() {}

  /**
   * A rule set version.
   *
   * @param id id
   * @param code code
   * @param step step
   * @param segment segment, null for all
   * @param businessType NB or RB, null for both
   * @param versionNo version
   * @param status status
   * @param effectiveFrom effective date
   * @param description description
   * @param maker maker (creator or submitter)
   * @param submittedAt submission time
   * @param approvedBy checker
   * @param approvedAt decision time
   * @param decisionRemarks remarks or rejection reason
   * @param rules rules, highest priority first (empty in lists)
   */
  public record RuleSetView(
      Long id,
      String code,
      SbmStep step,
      String segment,
      String businessType,
      int versionNo,
      String status,
      LocalDate effectiveFrom,
      String description,
      String maker,
      Instant submittedAt,
      String approvedBy,
      Instant approvedAt,
      String decisionRemarks,
      List<RuleView> rules) {

    /**
     * Maps a rule set.
     *
     * @param s rule set
     * @param rules its rules
     * @return view
     */
    public static RuleSetView from(SbmRuleSet s, List<SbmRule> rules) {
      return new RuleSetView(
          s.getId(),
          s.getCode(),
          s.getStep(),
          s.getSegment(),
          s.getBusinessType(),
          s.getVersionNo(),
          s.getStatus().name(),
          s.getEffectiveFrom(),
          s.getDescription(),
          s.getSubmittedBy() != null ? s.getSubmittedBy() : s.getCreatedBy(),
          s.getSubmittedAt(),
          s.getApprovedBy(),
          s.getApprovedAt(),
          s.getDecisionRemarks(),
          rules.stream().map(RuleView::from).toList());
    }
  }

  /**
   * A rule.
   *
   * @param id id
   * @param priority priority
   * @param name name
   * @param conditions conditions
   * @param outcome outcome
   * @param reasonCode reason
   * @param stop stop flag
   * @param active active
   */
  public record RuleView(
      Long id,
      int priority,
      String name,
      List<SbmRuleCondition> conditions,
      SbmRuleOutcome outcome,
      String reasonCode,
      boolean stop,
      boolean active) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return view
     */
    public static RuleView from(SbmRule r) {
      return new RuleView(
          r.getId(),
          r.getPriority(),
          r.getName(),
          r.getConditions(),
          r.getOutcome(),
          r.getReasonCode(),
          r.isStop(),
          r.isActive());
    }
  }

  /**
   * A new rule set.
   *
   * @param companyId company
   * @param code code
   * @param step step
   * @param segment segment, null for all
   * @param businessType NB or RB, null for both
   * @param effectiveFrom effective date
   * @param description description
   */
  public record CreateRequest(
      @NotNull Long companyId,
      @NotBlank String code,
      @NotNull SbmStep step,
      String segment,
      String businessType,
      @NotNull LocalDate effectiveFrom,
      String description) {}

  /**
   * The header of a draft.
   *
   * @param effectiveFrom effective date
   * @param description description
   */
  public record HeaderRequest(@NotNull LocalDate effectiveFrom, String description) {}

  /**
   * A rule to add or change.
   *
   * @param priority priority
   * @param name name
   * @param conditions conditions
   * @param outcome outcome
   * @param reasonCode reason
   * @param stop stop flag
   * @param active active
   */
  public record RuleRequest(
      int priority,
      @NotBlank String name,
      @NotNull List<SbmRuleCondition> conditions,
      SbmRuleOutcome outcome,
      String reasonCode,
      boolean stop,
      boolean active) {

    /**
     * The rule content.
     *
     * @return content
     */
    public SbmRule.Content content() {
      return new SbmRule.Content(
          priority, name, conditions, outcome, blankToNull(reasonCode), stop, active);
    }

    private static String blankToNull(String value) {
      return value == null || value.isBlank() ? null : value;
    }
  }

  /**
   * The approver's decision.
   *
   * @param remarks remarks (reason of a rejection)
   */
  public record DecisionRequest(String remarks) {}

  /**
   * A new version.
   *
   * @param effectiveFrom effective date
   */
  public record VersionRequest(LocalDate effectiveFrom) {}
}
