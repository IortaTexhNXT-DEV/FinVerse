package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * A rule of a rule set (BRIDSP-08): priority, conditions, outcome, reason code and stop flag. The
 * rules of a step are tried highest priority first; the first match with {@code stop} ends the
 * step.
 */
@Entity
@Table(name = "sbm_rule")
public class SbmRule extends BaseEntity {

  @Column(name = "rule_set_id", nullable = false, updatable = false)
  private Long ruleSetId;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false, length = 120)
  private String name;

  @Embedded private SbmRuleOutcome outcome;

  @Column(name = "reason_code", length = 40)
  private String reasonCode;

  @Column(nullable = false)
  private boolean stop;

  @Column(nullable = false)
  private boolean active;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "sbm_rule_condition", joinColumns = @JoinColumn(name = "rule_id"))
  @OrderColumn(name = "seq")
  @SuppressWarnings("PMD.ImmutableField") // mapped by JPA
  private List<SbmRuleCondition> conditions = new ArrayList<>();

  protected SbmRule() {}

  /**
   * A rule.
   *
   * @param ruleSetId rule set
   * @param content priority, name, conditions, outcome, reason, stop and active
   */
  public SbmRule(Long ruleSetId, Content content) {
    this.ruleSetId = ruleSetId;
    change(content);
  }

  /**
   * Changes the rule.
   *
   * @param content priority, name, conditions, outcome, reason, stop and active
   */
  public final void change(Content content) {
    this.priority = content.priority();
    this.name = content.name();
    this.outcome = content.outcome();
    this.reasonCode = content.reasonCode();
    this.stop = content.stop();
    this.active = content.active();
    this.conditions.clear();
    this.conditions.addAll(content.conditions());
  }

  public Long getRuleSetId() {
    return ruleSetId;
  }

  public int getPriority() {
    return priority;
  }

  public String getName() {
    return name;
  }

  public SbmRuleOutcome getOutcome() {
    return outcome == null ? new SbmRuleOutcome(null, null, null, null, null) : outcome;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public boolean isStop() {
    return stop;
  }

  public boolean isActive() {
    return active;
  }

  public List<SbmRuleCondition> getConditions() {
    return List.copyOf(conditions);
  }

  /**
   * The content of a rule.
   *
   * @param priority priority (highest first)
   * @param name name
   * @param conditions conditions, all must hold
   * @param outcome outcome
   * @param reasonCode reason (LOV SBM_REASON), may be null
   * @param stop whether a match ends the step
   * @param active whether the rule applies
   */
  public record Content(
      int priority,
      String name,
      List<SbmRuleCondition> conditions,
      SbmRuleOutcome outcome,
      String reasonCode,
      boolean stop,
      boolean active) {

    /** Defensive copy. */
    public Content {
      conditions = List.copyOf(conditions);
    }
  }
}
