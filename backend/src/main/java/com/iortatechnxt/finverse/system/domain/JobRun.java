package com.iortatechnxt.finverse.system.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/** One execution of a background job (job monitor and batch processing history). */
@Entity
@Table(name = "sys_job_run")
public class JobRun extends BaseEntity {

  private static final int MAX_MESSAGE = 1000;

  @Column(name = "job_name", nullable = false, length = 60)
  private String jobName;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 10)
  private JobTrigger trigger;

  @Column(name = "triggered_by", nullable = false, length = 50)
  private String triggeredBy;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private JobRunStatus status = JobRunStatus.RUNNING;

  @Column(name = "items_processed", nullable = false)
  private int itemsProcessed;

  @Column(length = MAX_MESSAGE)
  private String message;

  protected JobRun() {}

  /**
   * Starts a run.
   *
   * @param jobName job name
   * @param trigger trigger
   * @param triggeredBy user or SYSTEM
   * @param startedAt start time
   */
  public JobRun(String jobName, JobTrigger trigger, String triggeredBy, Instant startedAt) {
    this.jobName = jobName;
    this.trigger = trigger;
    this.triggeredBy = triggeredBy;
    this.startedAt = startedAt;
  }

  /**
   * Completes the run successfully.
   *
   * @param items items processed
   * @param text summary
   * @param when finish time
   */
  public void succeed(int items, String text, Instant when) {
    finish(JobRunStatus.SUCCEEDED, items, text, when);
  }

  /**
   * Completes the run with a failure.
   *
   * @param text error summary
   * @param when finish time
   */
  public void fail(String text, Instant when) {
    finish(JobRunStatus.FAILED, itemsProcessed, text, when);
  }

  private void finish(JobRunStatus result, int items, String text, Instant when) {
    this.status = result;
    this.itemsProcessed = items;
    this.message =
        text != null && text.length() > MAX_MESSAGE ? text.substring(0, MAX_MESSAGE) : text;
    this.finishedAt = when;
  }

  public String getJobName() {
    return jobName;
  }

  public JobTrigger getTrigger() {
    return trigger;
  }

  public String getTriggeredBy() {
    return triggeredBy;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public JobRunStatus getStatus() {
    return status;
  }

  public int getItemsProcessed() {
    return itemsProcessed;
  }

  public String getMessage() {
    return message;
  }
}
