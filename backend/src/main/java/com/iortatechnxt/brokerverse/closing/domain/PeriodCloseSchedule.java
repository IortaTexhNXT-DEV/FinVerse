package com.iortatechnxt.brokerverse.closing.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Scheduled month-end close of a period (FRBS 2.6.0 / Addendum 1): the GL Team Lead picks the date
 * and time; the job {@code GL_PERIOD_CLOSE} runs the checklist then and closes the period, or
 * records why it could not and raises {@code GL_CLOSE_FAILED}.
 */
@Entity
@Table(name = "acc_period_close_schedule")
public class PeriodCloseSchedule extends BaseEntity {

  /** Waiting for its time. */
  public static final String SCHEDULED = "SCHEDULED";

  /** The period was closed. */
  public static final String COMPLETED = "COMPLETED";

  /** The close was attempted and refused. */
  public static final String FAILED = "FAILED";

  /** Withdrawn before its time. */
  public static final String CANCELLED = "CANCELLED";

  private static final int MAX_RESULT = 2000;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "period_id", nullable = false)
  private Long periodId;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(nullable = false, length = 15)
  private String status = SCHEDULED;

  @Column(name = "executed_at")
  private Instant executedAt;

  @Column(length = MAX_RESULT)
  private String result;

  protected PeriodCloseSchedule() {}

  /**
   * Schedules a close.
   *
   * @param companyId company
   * @param periodId period to close
   * @param scheduledAt when
   */
  public PeriodCloseSchedule(Long companyId, Long periodId, Instant scheduledAt) {
    this.companyId = companyId;
    this.periodId = periodId;
    this.scheduledAt = scheduledAt;
  }

  /**
   * Records the outcome of the run.
   *
   * @param closed whether the period was closed
   * @param when run time
   * @param text outcome, e.g. the blocking checklist items
   */
  public void executed(boolean closed, Instant when, String text) {
    requireScheduled();
    this.status = closed ? COMPLETED : FAILED;
    this.executedAt = when;
    this.result =
        text == null || text.length() <= MAX_RESULT ? text : text.substring(0, MAX_RESULT);
  }

  /**
   * Withdraws the schedule.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    requireScheduled();
    this.status = CANCELLED;
    this.result = reason;
  }

  private void requireScheduled() {
    if (!SCHEDULED.equals(status)) {
      throw new BusinessRuleException(
          "CLOSE_SCHEDULE_NOT_ACTIVE", "The scheduled close is already " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPeriodId() {
    return periodId;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public String getStatus() {
    return status;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public String getResult() {
    return result;
  }
}
