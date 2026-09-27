package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * The result of one check in a {@link CheckRun} (BRRN.020): outcome, severity from the check
 * settings, the message shown on the Checks and Bucket tab and a detail for the report.
 */
@Entity
@Table(name = "rnw_check_result")
public class CheckResult extends BaseEntity {

  private static final int MESSAGE_LENGTH = 500;
  private static final int DETAIL_LENGTH = 2000;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "check_code", nullable = false, length = 40, updatable = false)
  private String checkCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private CheckOutcome outcome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private CheckSeverity severity;

  @Column(length = MESSAGE_LENGTH, updatable = false)
  private String message;

  @Column(length = DETAIL_LENGTH, updatable = false)
  private String detail;

  protected CheckResult() {}

  /**
   * Records a result.
   *
   * @param run check run
   * @param checkCode check
   * @param outcome outcome
   * @param severity severity of the check
   * @param message message
   * @param detail detail (compared values)
   */
  public CheckResult(
      CheckRun run,
      String checkCode,
      CheckOutcome outcome,
      CheckSeverity severity,
      String message,
      String detail) {
    this.runId = run.getId();
    this.candidateId = run.getCandidateId();
    this.checkCode = checkCode;
    this.outcome = outcome;
    this.severity = severity;
    this.message = cut(message, MESSAGE_LENGTH);
    this.detail = cut(detail, DETAIL_LENGTH);
  }

  private static String cut(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getRunId() {
    return runId;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getCheckCode() {
    return checkCode;
  }

  public CheckOutcome getOutcome() {
    return outcome;
  }

  public CheckSeverity getSeverity() {
    return severity;
  }

  public String getMessage() {
    return message;
  }

  public String getDetail() {
    return detail;
  }

  /**
   * Whether the check failed.
   *
   * @return true for FAIL
   */
  public boolean failed() {
    return outcome == CheckOutcome.FAIL;
  }
}
