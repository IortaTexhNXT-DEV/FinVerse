package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A tagging rule of a RISK_RULES version (SNSRP-102). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_risk_rule")
public class RiskRuleRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "priority", nullable = false, updatable = false)
  private int priority;

  @Column(name = "category_code", nullable = false, length = 30, updatable = false)
  private String categoryCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "condition_attr", nullable = false, length = 30, updatable = false)
  private RiskAttribute conditionAttr;

  @Enumerated(EnumType.STRING)
  @Column(name = "operator", nullable = false, length = 10, updatable = false)
  private RuleOperator operator;

  @Column(name = "rule_values", nullable = false, length = 500, updatable = false)
  private String ruleValues;

  /** For JPA. */
  protected RiskRuleRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param priority evaluation order
   * @param categoryCode category assigned
   * @param conditionAttr attribute tested
   * @param operator operator
   * @param ruleValues values compared, comma separated
   */
  @SuppressWarnings("java:S107")
  public RiskRuleRow(
      Long versionId,
      int priority,
      String categoryCode,
      RiskAttribute conditionAttr,
      RuleOperator operator,
      String ruleValues) {
    this.versionId = versionId;
    this.priority = priority;
    this.categoryCode = categoryCode;
    this.conditionAttr = conditionAttr;
    this.operator = operator;
    this.ruleValues = ruleValues;
  }

  public Long getVersionId() {
    return versionId;
  }

  public int getPriority() {
    return priority;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public RiskAttribute getConditionAttr() {
    return conditionAttr;
  }

  public RuleOperator getOperator() {
    return operator;
  }

  public String getRuleValues() {
    return ruleValues;
  }
}
