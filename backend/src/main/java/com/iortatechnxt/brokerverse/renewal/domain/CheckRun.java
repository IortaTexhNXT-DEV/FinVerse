package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One run of the checks of a candidate (BRRN.020): what triggered it, when, and the bucket before
 * and after with the rule-set version (BRRN.023). Its results are {@link CheckResult} rows.
 */
@Entity
@Table(name = "rnw_check_run")
public class CheckRun extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_kind", nullable = false, length = 20, updatable = false)
  private CheckTrigger trigger;

  @Column(name = "run_at", nullable = false, updatable = false)
  private Instant runAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "bucket_before", length = 20)
  private Bucket bucketBefore;

  @Enumerated(EnumType.STRING)
  @Column(name = "bucket_after", nullable = false, length = 20)
  private Bucket bucketAfter;

  @Column(name = "rule_set_version")
  private Integer ruleSetVersion;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  protected CheckRun() {}

  /**
   * Records a run.
   *
   * @param candidate candidate
   * @param trigger what ran the checks
   * @param runAt time
   * @param outcome bucket before and after, rule set version and number of failed checks
   */
  public CheckRun(
      RenewalCandidate candidate, CheckTrigger trigger, Instant runAt, Outcome outcome) {
    this.companyId = candidate.getCompanyId();
    this.candidateId = candidate.getId();
    this.trigger = trigger;
    this.runAt = runAt;
    this.bucketBefore = outcome.before();
    this.bucketAfter = outcome.after();
    this.ruleSetVersion = outcome.ruleSetVersion();
    this.failedCount = outcome.failed();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public CheckTrigger getTrigger() {
    return trigger;
  }

  public Instant getRunAt() {
    return runAt;
  }

  public Bucket getBucketBefore() {
    return bucketBefore;
  }

  public Bucket getBucketAfter() {
    return bucketAfter;
  }

  public Integer getRuleSetVersion() {
    return ruleSetVersion;
  }

  public int getFailedCount() {
    return failedCount;
  }

  /**
   * What a run decided.
   *
   * @param before bucket before
   * @param after bucket after
   * @param ruleSetVersion rule set version, null for the built-in rules
   * @param failed number of failed checks
   */
  public record Outcome(Bucket before, Bucket after, Integer ruleSetVersion, int failed) {}
}
