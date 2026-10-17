package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** One limit checked for a record in a run (BRIDSP-16): the limit, the value and the result. */
@Entity
@Table(name = "sbm_limit_check")
public class SbmLimitCheck extends BaseEntity {

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "limit_rule_id", nullable = false, updatable = false)
  private Long limitRuleId;

  @Column(nullable = false, updatable = false, length = 40)
  private String attribute;

  @Column(name = "limit_value", nullable = false, updatable = false, length = 120)
  private String limitValue;

  @Column(name = "actual_value", updatable = false, length = 120)
  private String actualValue;

  @Column(nullable = false, updatable = false)
  private boolean breached;

  protected SbmLimitCheck() {}

  /**
   * A check.
   *
   * @param runId run, may be null
   * @param policyId record
   * @param limitRuleId limit rule
   * @param result attribute, limit, value and breached flag
   */
  public SbmLimitCheck(Long runId, Long policyId, Long limitRuleId, Result result) {
    this.runId = runId;
    this.policyId = policyId;
    this.limitRuleId = limitRuleId;
    this.attribute = result.attribute();
    this.limitValue = result.limit();
    this.actualValue = result.value();
    this.breached = result.breached();
  }

  public Long getRunId() {
    return runId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Long getLimitRuleId() {
    return limitRuleId;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getLimitValue() {
    return limitValue;
  }

  public String getActualValue() {
    return actualValue;
  }

  public boolean isBreached() {
    return breached;
  }

  /**
   * The result of a check.
   *
   * @param attribute what is limited (Sum insured, Vehicle age, or a fact)
   * @param limit limit
   * @param value value of the record
   * @param breached whether the value is above the limit
   */
  public record Result(String attribute, String limit, String value, boolean breached) {}
}
