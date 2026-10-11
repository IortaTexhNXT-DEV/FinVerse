package com.iortatechnxt.brokerverse.renewal.mft.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_MFT_INBOX} (cron {@code brokerverse.jobs.renewal-mft-inbox-cron}, every 15 minutes by
 * default): takes the insurers' placement responses, hold cover responses and e-policy files
 * received through MFT.
 */
@Component
public class MftInboxJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_MFT_INBOX";

  private final MftInbox inbox;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param inbox MFT inbound folders
   * @param cron schedule
   */
  public MftInboxJob(
      MftInbox inbox,
      @Value("${brokerverse.jobs.renewal-mft-inbox-cron:0 */15 * * * *}") String cron) {
    this.inbox = inbox;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Takes the placement responses, hold cover responses and e-policies received from the"
        + " insurers through MFT";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public Workload workload() {
    return Workload.INTEGRATION;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int files = inbox.poll();
    return new JobOutcome(files, files + " file(s) received");
  }
}
