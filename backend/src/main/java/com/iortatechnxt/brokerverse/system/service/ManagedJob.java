package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import java.time.LocalDate;

/**
 * Port for background jobs shown in the job monitor.
 *
 * <p>Any module declares a job by implementing this interface as a Spring bean: the {@link
 * JobScheduler} schedules it by {@link #cron()}, {@link JobRunService} records every run, and
 * administrators can start it from the job monitor. Do not annotate the job with
 * {@code @Scheduled}.
 */
public interface ManagedJob {

  /**
   * Unique job name (UPPER_SNAKE_CASE), used as key in the run history.
   *
   * @return name
   */
  String name();

  /**
   * One line description for the monitor.
   *
   * @return description
   */
  String description();

  /**
   * Spring cron expression (6 fields, UTC); {@code "-"} disables scheduling (manual runs only).
   *
   * @return cron expression
   */
  String cron();

  /**
   * Workload of the job, which decides the runtime role that schedules it: {@link Workload#BATCH}
   * (the {@code jobs} deployment) unless the job moves data to or from another system, then {@link
   * Workload#INTEGRATION} (the {@code integration} deployment). Manual runs from the job monitor
   * are not affected.
   *
   * @return workload
   */
  default Workload workload() {
    return Workload.BATCH;
  }

  /**
   * Executes the job. Runs outside a transaction: open your own per unit of work so one failing
   * item does not roll back the others.
   *
   * @param businessDate business date of the run
   * @return outcome
   */
  JobOutcome execute(LocalDate businessDate);
}
