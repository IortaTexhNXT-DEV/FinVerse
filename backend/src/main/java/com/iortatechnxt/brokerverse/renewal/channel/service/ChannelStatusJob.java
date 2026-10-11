package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_CHANNEL_STATUS} (cron {@code brokerverse.jobs.renewal-channel-status-cron}, every 15
 * minutes by default): reads the delivery status of the CCM and MFT messages on their way and
 * transmits again the messages pending after a refused attempt.
 */
@Component
public class ChannelStatusJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_CHANNEL_STATUS";

  private final ChannelService channels;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param channels channel messages
   * @param cron schedule
   */
  public ChannelStatusJob(
      ChannelService channels,
      @Value("${brokerverse.jobs.renewal-channel-status-cron:0 */15 * * * *}") String cron) {
    this.channels = channels;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Reads the delivery status of the CCM and MFT messages and retries the pending ones";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int failed = channels.refresh();
    int pending = channels.retry();
    return new JobOutcome(failed, failed + " message(s) failed, " + pending + " still pending");
  }
}
