package com.iortatechnxt.brokerverse.migration.intake.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A data-quality issue of a staged row (DATA_MIGRATION_DESIGN section 8; FR-DM-013): the rule, its
 * severity, the field and value and the message, and how the issue was resolved (fixed at source,
 * mapped, waived by the data owner with a reason, or the row excluded with a manual-entry plan).
 */
@Entity
@Table(name = "mig_issue")
public class MigIssue extends BaseEntity {

  private static final int MAX_VALUE = 300;
  private static final int MAX_MESSAGE = 1000;

  /** Severity of an issue. */
  public enum Severity {
    /** The row does not load. */
    ERROR,
    /** The row loads and is reported. */
    WARNING
  }

  /** Resolution of an issue. */
  public enum Resolution {
    /** Not resolved. */
    OPEN,
    /** Corrected in a new extract. */
    FIXED_AT_SOURCE,
    /** Corrected by a new code map version. */
    MAPPED,
    /** Waived by the data owner; the row loads. */
    WAIVED,
    /** Row excluded from the batch by the data owner. */
    EXCLUDED
  }

  @Column(name = "stage_row_id", nullable = false, updatable = false)
  private Long stageRowId;

  @Column(name = "batch_id")
  private Long batchId;

  @Column(name = "rule_code", nullable = false, length = 20, updatable = false)
  private String ruleCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Severity severity;

  @Column(length = 60, updatable = false)
  private String field;

  @Column(length = 300, updatable = false)
  private String value;

  @Column(nullable = false, length = 1000, updatable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Resolution resolution = Resolution.OPEN;

  @Column(name = "resolution_note", length = 1000)
  private String resolutionNote;

  @Column(name = "waiver_reason", length = 40)
  private String waiverReason;

  @Column(name = "resolved_by", length = 50)
  private String resolvedBy;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected MigIssue() {}

  /**
   * A new issue.
   *
   * @param stageRowId staged row
   * @param batchId batch
   * @param finding rule result
   */
  public MigIssue(Long stageRowId, Long batchId, Finding finding) {
    this.stageRowId = stageRowId;
    this.batchId = batchId;
    this.ruleCode = finding.ruleCode();
    this.severity = finding.severity();
    this.field = finding.field();
    this.value = clip(finding.value(), MAX_VALUE);
    this.message = clip(finding.message(), MAX_MESSAGE);
  }

  private static String clip(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  /**
   * Resolves the issue.
   *
   * @param how resolution
   * @param reason waiver reason code (waiver and exclusion)
   * @param note note
   * @param user user
   * @param when time
   */
  public void resolve(Resolution how, String reason, String note, String user, Instant when) {
    this.resolution = how;
    this.waiverReason = reason;
    this.resolutionNote = note;
    this.resolvedBy = user;
    this.resolvedAt = when;
  }

  public boolean isOpenError() {
    return severity == Severity.ERROR && resolution == Resolution.OPEN;
  }

  public Long getStageRowId() {
    return stageRowId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public Severity getSeverity() {
    return severity;
  }

  public String getField() {
    return field;
  }

  public String getValue() {
    return value;
  }

  public String getMessage() {
    return message;
  }

  public Resolution getResolution() {
    return resolution;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public String getWaiverReason() {
    return waiverReason;
  }

  public String getResolvedBy() {
    return resolvedBy;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  /**
   * The result of one rule on one row.
   *
   * @param ruleCode rule
   * @param severity severity
   * @param field column
   * @param value value
   * @param message message
   */
  public record Finding(
      String ruleCode, Severity severity, String field, String value, String message) {

    /**
     * An error finding.
     *
     * @param rule rule
     * @param field column
     * @param value value
     * @param message message
     * @return finding
     */
    public static Finding error(String rule, String field, String value, String message) {
      return new Finding(rule, Severity.ERROR, field, value, message);
    }

    /**
     * A warning finding.
     *
     * @param rule rule
     * @param field column
     * @param value value
     * @param message message
     * @return finding
     */
    public static Finding warning(String rule, String field, String value, String message) {
      return new Finding(rule, Severity.WARNING, field, value, message);
    }
  }
}
