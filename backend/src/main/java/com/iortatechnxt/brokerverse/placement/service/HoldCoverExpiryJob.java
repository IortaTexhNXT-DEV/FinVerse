package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.ExpiryRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Hold cover expiry monitor (BRNB.103): alerts Processing a configurable number of days ({@code
 * HOLD_COVER_ALERT_DAYS}) before an open hold cover lapses and expires it afterwards.
 */
@Component
public class HoldCoverExpiryJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "HOLD_COVER_EXPIRY";

  private final HoldCoverService holdCovers;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param holdCovers hold cover service
   * @param cron schedule ({@code brokerverse.jobs.hold-cover-expiry-cron})
   */
  public HoldCoverExpiryJob(
      HoldCoverService holdCovers,
      @Value("${brokerverse.jobs.hold-cover-expiry-cron:0 30 0 * * *}") String cron) {
    this.holdCovers = holdCovers;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Alerts Processing of hold covers about to expire and expires lapsed hold covers";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    ExpiryRun run = holdCovers.runExpiry(businessDate);
    return new JobOutcome(
        run.alerted() + run.expired(),
        run.alerted() + " hold cover(s) expiring alerted; " + run.expired() + " expired");
  }
}
