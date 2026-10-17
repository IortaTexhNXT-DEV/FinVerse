package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A change of bucket (BRRN.023 AC 6): from, to, rule set version and rule, check run, cause (rule
 * or override), user (created by) and remarks.
 */
@Entity
@Table(name = "rnw_bucket_history")
public class BucketHistory extends BaseEntity {

  /** Why the bucket changed. */
  public enum Cause {
    /** The bucket rules on a check run. */
    RULE,
    /** A controlled override. */
    OVERRIDE
  }

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_bucket", length = 20, updatable = false)
  private Bucket fromBucket;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_bucket", nullable = false, length = 20, updatable = false)
  private Bucket toBucket;

  @Column(name = "rule_set_version", updatable = false)
  private Integer ruleSetVersion;

  @Column(name = "rule_id", updatable = false)
  private Long ruleId;

  @Column(name = "check_run_id", updatable = false)
  private Long checkRunId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Cause cause;

  @Column(length = 200, updatable = false)
  private String remarks;

  protected BucketHistory() {}

  /**
   * Records a change.
   *
   * @param candidateId candidate
   * @param change from and to bucket
   * @param rule rule set version, rule and check run (null for an override)
   * @param cause cause
   * @param remarks remarks of an override
   */
  public BucketHistory(Long candidateId, Change change, RuleRef rule, Cause cause, String remarks) {
    this.candidateId = candidateId;
    this.fromBucket = change.from();
    this.toBucket = change.to();
    this.ruleSetVersion = rule == null ? null : rule.ruleSetVersion();
    this.ruleId = rule == null ? null : rule.ruleId();
    this.checkRunId = rule == null ? null : rule.checkRunId();
    this.cause = cause;
    this.remarks = remarks;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public Bucket getFromBucket() {
    return fromBucket;
  }

  public Bucket getToBucket() {
    return toBucket;
  }

  public Integer getRuleSetVersion() {
    return ruleSetVersion;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public Long getCheckRunId() {
    return checkRunId;
  }

  public Cause getCause() {
    return cause;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * A bucket change.
   *
   * @param from previous bucket, null at the first evaluation
   * @param to new bucket
   */
  public record Change(Bucket from, Bucket to) {}

  /**
   * The rule that decided a bucket.
   *
   * @param ruleSetVersion rule set version, null for the built-in rules
   * @param ruleId rule, null when no rule matched
   * @param checkRunId check run
   */
  public record RuleRef(Integer ruleSetVersion, Long ruleId, Long checkRunId) {}
}
