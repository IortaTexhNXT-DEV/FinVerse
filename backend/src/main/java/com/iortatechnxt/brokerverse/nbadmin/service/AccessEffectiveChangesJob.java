package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessScheduledChanges.Result;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily job UAM_EFFECTIVE_CHANGES (UAM-NFR-14; FR-UA-020): applies the approved access requests
 * whose effective date has come ({@code brokerverse.jobs.uam-effective-changes-cron}, 00:05 PHT).
 */
@Component
public class AccessEffectiveChangesJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "UAM_EFFECTIVE_CHANGES";

  private final AccessScheduledChanges changes;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param changes scheduled changes
   * @param cron schedule ({@code brokerverse.jobs.uam-effective-changes-cron})
   */
  public AccessEffectiveChangesJob(
      AccessScheduledChanges changes,
      @Value("${brokerverse.jobs.uam-effective-changes-cron:0 5 16 * * *}") String cron) {
    this.changes = changes;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Applies approved user access requests on their effective date";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Result result = changes.applyDue(businessDate);
    return new JobOutcome(
        result.applied() + result.failed(),
        result.applied() + " request(s) applied; " + result.failed() + " failed (alerted)");
  }
}
