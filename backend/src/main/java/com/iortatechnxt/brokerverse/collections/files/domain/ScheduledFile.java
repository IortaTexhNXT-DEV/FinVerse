package com.iortatechnxt.brokerverse.collections.files.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A Collections file published by a job or on request (BRCLXN.024-029, 045): the report, its period
 * and scope (unit and branch of a weekly file), the archived report run holding the file and the
 * time from which users may download it.
 */
@Entity
@Table(name = "clx_scheduled_file")
public class ScheduledFile extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Frequency frequency;

  @Column(name = "report_code", nullable = false, length = 40, updatable = false)
  private String reportCode;

  @Column(name = "period_key", nullable = false, length = 20, updatable = false)
  private String periodKey;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 80, updatable = false)
  private String scope;

  @Column(name = "report_run_id")
  private Long reportRunId;

  @Column(name = "row_count", nullable = false)
  private int rowCount;

  @Column(name = "available_from")
  private Instant availableFrom;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status;

  @Column(length = 500)
  private String message;

  protected ScheduledFile() {}

  /**
   * Records a published file.
   *
   * @param companyId company
   * @param spec report, period and scope
   * @param reportRunId archived run with the file
   * @param rowCount rows
   * @param availableFrom when users may download it, null = at once
   */
  public ScheduledFile(
      Long companyId, Spec spec, Long reportRunId, int rowCount, Instant availableFrom) {
    this(companyId, spec);
    this.reportRunId = reportRunId;
    this.rowCount = rowCount;
    this.availableFrom = availableFrom;
    this.status = Status.PUBLISHED;
  }

  /**
   * Records a file that could not be generated.
   *
   * @param companyId company
   * @param spec report, period and scope
   * @param message the error
   */
  public ScheduledFile(Long companyId, Spec spec, String message) {
    this(companyId, spec);
    this.status = Status.FAILED;
    final int max = 500;
    this.message = message == null || message.length() <= max ? message : message.substring(0, max);
  }

  private ScheduledFile(Long companyId, Spec spec) {
    this.companyId = companyId;
    this.frequency = spec.frequency();
    this.reportCode = spec.reportCode();
    this.periodKey = spec.periodKey();
    this.periodFrom = spec.from();
    this.periodTo = spec.to();
    this.scope = spec.scope() == null ? "" : spec.scope();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Frequency getFrequency() {
    return frequency;
  }

  public String getReportCode() {
    return reportCode;
  }

  public String getPeriodKey() {
    return periodKey;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getScope() {
    return scope;
  }

  public Long getReportRunId() {
    return reportRunId;
  }

  public int getRowCount() {
    return rowCount;
  }

  public Instant getAvailableFrom() {
    return availableFrom;
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  /** How often a file is produced. */
  public enum Frequency {
    /** Daily after the EOD (BRCLXN.045). */
    DAILY,
    /** Weekly, Saturday to Friday (BRCLXN.028). */
    WEEKLY,
    /** Monthly, previous month (BRCLXN.024/026). */
    MONTHLY,
    /** An export requested by a user (caveat p.93). */
    ON_REQUEST
  }

  /** Outcome of a publication. */
  public enum Status {
    /** Generated and archived. */
    PUBLISHED,
    /** Generation failed (alert CLX_FILE_NOT_PUBLISHED). */
    FAILED
  }

  /**
   * What a file covers.
   *
   * @param frequency frequency
   * @param reportCode report
   * @param periodKey period key (2026-09-24, 2026-W39, 2026-09)
   * @param from first day
   * @param to last day
   * @param scope unit and branch of a weekly file, requestor of an export; empty otherwise
   */
  public record Spec(
      Frequency frequency,
      String reportCode,
      String periodKey,
      LocalDate from,
      LocalDate to,
      String scope) {}
}
