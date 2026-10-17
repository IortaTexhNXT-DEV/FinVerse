package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_HOLD_COVER} (cron {@code brokerverse.jobs.renewal-hold-cover-cron}, daily by default):
 * requests the hold cover of the Clean CBG accounts the set days before expiry, one file per
 * insurer, and on the set day of the month extends the hold cover of the expired and unbooked CBG
 * Home accounts.
 */
@Component
public class HoldCoverJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_HOLD_COVER";

  private final HoldCoverBatches batches;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param batches automatic requests
   * @param cron schedule
   */
  public HoldCoverJob(
      HoldCoverBatches batches,
      @Value("${brokerverse.jobs.renewal-hold-cover-cron:0 30 21 * * *}") String cron) {
    this.batches = batches;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Requests the hold cover of the Clean CBG accounts and extends the CBG Home ones";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int cbg = batches.cbgClean(businessDate);
    int extended = batches.cbgHomeExtensions(businessDate);
    return new JobOutcome(cbg + extended, cbg + " request(s), " + extended + " extension(s)");
  }
}
