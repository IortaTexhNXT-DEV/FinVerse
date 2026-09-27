package com.iortatechnxt.brokerverse.migration.recon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A line of a reconciliation run: level, measure and currency with the source (control file),
 * staged and target values, the difference against the tolerance and the status. A BREAK needs an
 * explanation with a reason, approved by a reconciliation approver other than the user who
 * explained it.
 */
@Entity
@Table(name = "mig_recon_line")
public class ReconLine extends BaseEntity {

  private static final int MAX_MEASURE = 200;
  private static final int MAX_DETAIL = 1000;

  /** Status of a line. */
  public enum Status {
    MATCHED,
    BREAK,
    EXPLAINED
  }

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(nullable = false, length = 4, updatable = false)
  private String level;

  @Column(nullable = false, length = 200, updatable = false)
  private String measure;

  @Column(length = 3, updatable = false)
  private String currency;

  @Column(name = "source_value", precision = 21, scale = 2, updatable = false)
  private BigDecimal sourceValue;

  @Column(name = "staged_value", precision = 21, scale = 2, updatable = false)
  private BigDecimal stagedValue;

  @Column(name = "target_value", precision = 21, scale = 2, updatable = false)
  private BigDecimal targetValue;

  @Column(precision = 21, scale = 2, updatable = false)
  private BigDecimal difference;

  @Column(nullable = false, precision = 21, scale = 2, updatable = false)
  private BigDecimal tolerance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status;

  @Column(length = 1000, updatable = false)
  private String detail;

  @Column(name = "break_reason", length = 40)
  private String breakReason;

  @Column(length = 1000)
  private String explanation;

  @Column(name = "explained_by", length = 50)
  private String explainedBy;

  @Column(name = "explained_at")
  private Instant explainedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  protected ReconLine() {}

  /**
   * A line; the status follows from the difference and the tolerance.
   *
   * @param runId run
   * @param level level
   * @param measure measure
   * @param currency currency
   * @param values source, staged and target values
   * @param tolerance tolerance
   * @param detail detail (differences per field, keys)
   */
  public ReconLine(
      Long runId,
      String level,
      String measure,
      String currency,
      Values values,
      BigDecimal tolerance,
      String detail) {
    this.runId = runId;
    this.level = level;
    this.measure = measure.length() > MAX_MEASURE ? measure.substring(0, MAX_MEASURE) : measure;
    this.currency = currency;
    this.sourceValue = values.source();
    this.stagedValue = values.staged();
    this.targetValue = values.target();
    this.tolerance = tolerance;
    BigDecimal expected = values.source() != null ? values.source() : values.staged();
    this.difference =
        expected == null || values.target() == null ? null : values.target().subtract(expected);
    boolean matched =
        difference == null ? values.matched() : difference.abs().compareTo(tolerance) <= 0;
    this.status = matched ? Status.MATCHED : Status.BREAK;
    this.detail =
        detail == null || detail.length() <= MAX_DETAIL ? detail : detail.substring(0, MAX_DETAIL);
  }

  /**
   * Explains a break.
   *
   * @param reason reason code (list MIG_BREAK_REASON)
   * @param text explanation
   * @param user user
   * @param when time
   */
  public void explain(String reason, String text, String user, Instant when) {
    if (status == Status.MATCHED) {
      throw new BusinessRuleException("MIG_LINE_MATCHED", "This line has no difference to explain");
    }
    if (text == null || text.isBlank()) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the explanation of the break");
    }
    this.breakReason = reason;
    this.explanation = text.strip();
    this.explainedBy = user;
    this.explainedAt = when;
    this.approvedBy = null;
    this.approvedAt = null;
    this.status = Status.BREAK;
  }

  /**
   * Approves the explanation.
   *
   * @param user reconciliation approver
   * @param when time
   */
  public void approve(String user, Instant when) {
    if (explanation == null) {
      throw new BusinessRuleException("MIG_NOT_EXPLAINED", "Explain the break before approving it");
    }
    if (CurrentUser.sameUser(user, explainedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.approvedBy = user;
    this.approvedAt = when;
    this.status = Status.EXPLAINED;
  }

  public Long getRunId() {
    return runId;
  }

  public String getLevel() {
    return level;
  }

  public String getMeasure() {
    return measure;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getSourceValue() {
    return sourceValue;
  }

  public BigDecimal getStagedValue() {
    return stagedValue;
  }

  public BigDecimal getTargetValue() {
    return targetValue;
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public BigDecimal getTolerance() {
    return tolerance;
  }

  public Status getStatus() {
    return status;
  }

  public String getDetail() {
    return detail;
  }

  public String getBreakReason() {
    return breakReason;
  }

  public String getExplanation() {
    return explanation;
  }

  public String getExplainedBy() {
    return explainedBy;
  }

  public Instant getExplainedAt() {
    return explainedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  /**
   * Values of a line.
   *
   * @param source value of the control file (or legacy)
   * @param staged value in staging
   * @param target value read from BIBS
   * @param matched result of a line without amounts (hash and field checks)
   */
  public record Values(BigDecimal source, BigDecimal staged, BigDecimal target, boolean matched) {

    /**
     * Amount or count values compared by difference.
     *
     * @param source source
     * @param staged staged
     * @param target target
     * @return values
     */
    public static Values of(BigDecimal source, BigDecimal staged, BigDecimal target) {
      return new Values(source, staged, target, true);
    }

    /**
     * A check without amounts.
     *
     * @param matched result
     * @return values
     */
    public static Values check(boolean matched) {
      return new Values(null, null, null, matched);
    }
  }
}
