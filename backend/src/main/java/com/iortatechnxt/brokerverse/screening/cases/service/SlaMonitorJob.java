package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code SCR_SLA_MONITOR} (SNSRP-405, 802; cron {@code brokerverse.jobs.scr-sla-monitor-cron},
 * hourly): SLA reminders, breach flags with escalation and alert, and missing-document reminders of
 * the open screening cases ({@link SlaMonitor}).
 */
@Component
public class SlaMonitorJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SCR_SLA_MONITOR";

  private final SlaMonitor monitor;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param monitor the SLA check
   * @param cron schedule
   */
  public SlaMonitorJob(
      SlaMonitor monitor, @Value("${brokerverse.jobs.scr-sla-monitor-cron:-}") String cron) {
    this.monitor = monitor;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends the SLA reminders, flags and escalates the SLA breaches and reminds of missing"
        + " documents of the open screening cases (SNSRP-405, 802)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    SlaMonitor.Result result = monitor.run();
    return new JobOutcome(
        result.reminders() + result.breaches() + result.documentReminders(),
        result.reminders()
            + " reminder(s), "
            + result.breaches()
            + " breach(es) escalated, "
            + result.documentReminders()
            + " document reminder(s)");
  }
}
