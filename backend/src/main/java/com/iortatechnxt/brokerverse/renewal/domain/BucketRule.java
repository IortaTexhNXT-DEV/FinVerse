package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One rule of a {@link BucketRuleSet}: when a check (any check when blank) of a severity (any when
 * blank) has the outcome FAIL or WARN, the candidate goes to the result bucket.
 */
@Entity
@Table(name = "rnw_bucket_rule")
public class BucketRule extends BaseEntity {

  @Column(nullable = false)
  private int priority;

  @Column(name = "check_code", length = 40)
  private String checkCode;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CheckSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CheckOutcome outcome;

  @Enumerated(EnumType.STRING)
  @Column(name = "result_bucket", nullable = false, length = 20)
  private Bucket resultBucket;

  protected BucketRule() {}

  /**
   * Creates a rule.
   *
   * @param data rule data
   */
  public BucketRule(Data data) {
    this.priority = data.priority();
    this.checkCode = data.checkCode();
    this.severity = data.severity();
    this.outcome = data.outcome();
    this.resultBucket = data.bucket();
  }

  /**
   * Whether the rule matches a check result.
   *
   * @param code check
   * @param resultOutcome outcome of the check
   * @param resultSeverity severity of the check
   * @return true when the check, severity and outcome match
   */
  public boolean matches(String code, CheckOutcome resultOutcome, CheckSeverity resultSeverity) {
    return resultOutcome == outcome
        && (checkCode == null || checkCode.equals(code))
        && (severity == null || severity == resultSeverity);
  }

  public int getPriority() {
    return priority;
  }

  public String getCheckCode() {
    return checkCode;
  }

  public CheckSeverity getSeverity() {
    return severity;
  }

  public CheckOutcome getOutcome() {
    return outcome;
  }

  public Bucket getResultBucket() {
    return resultBucket;
  }

  /**
   * Data of a rule.
   *
   * @param priority priority (lowest first)
   * @param checkCode check, null for any
   * @param severity severity, null for any
   * @param outcome FAIL or WARN
   * @param bucket result bucket
   */
  public record Data(
      int priority,
      String checkCode,
      CheckSeverity severity,
      CheckOutcome outcome,
      Bucket bucket) {}
}
