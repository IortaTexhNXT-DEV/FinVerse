package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * The result of one step of a run for one record (BRIDSP-08-10; design principle 3): outcome,
 * bucket, reason code and the rule and rule-set version that produced it, so every result explains
 * itself.
 */
@Entity
@Table(name = "sbm_run_result")
public class SbmRunResult extends BaseEntity {

  /** Outcome of a step. */
  public enum Outcome {
    /** The step passed without a bucket. */
    PASSED,
    /** The step set a bucket. */
    BUCKETED,
    /** No rule placed the record, or the rule sent it to the fallout. */
    FALLOUT,
    /** A manual tag overrode the rule. */
    OVERRIDDEN
  }

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private SbmStep step;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private Outcome outcome;

  @Column(updatable = false, length = 40)
  private String bucket;

  @Column(name = "reason_code", updatable = false, length = 40)
  private String reasonCode;

  @Column(name = "rule_id", updatable = false)
  private Long ruleId;

  @Column(name = "rule_name", updatable = false, length = 120)
  private String ruleName;

  @Column(name = "rule_set_code", updatable = false, length = 40)
  private String ruleSetCode;

  @Column(name = "rule_set_version", updatable = false)
  private Integer ruleSetVersion;

  @Column(updatable = false, length = 500)
  private String message;

  protected SbmRunResult() {}

  /**
   * A result.
   *
   * @param runId run
   * @param policyId record
   * @param step step
   * @param decision outcome, bucket, reason, rule and message
   */
  public SbmRunResult(Long runId, Long policyId, SbmStep step, Decision decision) {
    this.runId = runId;
    this.policyId = policyId;
    this.step = step;
    this.outcome = decision.outcome();
    this.bucket = decision.bucket();
    this.reasonCode = decision.reasonCode();
    this.ruleId = decision.ruleId();
    this.ruleName = decision.ruleName();
    this.ruleSetCode = decision.ruleSetCode();
    this.ruleSetVersion = decision.ruleSetVersion();
    String m = decision.message();
    this.message = m != null && m.length() > 500 ? m.substring(0, 500) : m;
  }

  public Long getRunId() {
    return runId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public SbmStep getStep() {
    return step;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public String getBucket() {
    return bucket;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public String getRuleName() {
    return ruleName;
  }

  public String getRuleSetCode() {
    return ruleSetCode;
  }

  public Integer getRuleSetVersion() {
    return ruleSetVersion;
  }

  public String getMessage() {
    return message;
  }

  /**
   * The decision of a step.
   *
   * @param outcome outcome
   * @param bucket bucket, may be null
   * @param reasonCode reason, may be null
   * @param ruleId rule, may be null
   * @param ruleName rule name, may be null
   * @param ruleSetCode rule set, may be null
   * @param ruleSetVersion rule set version, may be null
   * @param message message, may be null
   */
  public record Decision(
      Outcome outcome,
      String bucket,
      String reasonCode,
      Long ruleId,
      String ruleName,
      String ruleSetCode,
      Integer ruleSetVersion,
      String message) {}
}
