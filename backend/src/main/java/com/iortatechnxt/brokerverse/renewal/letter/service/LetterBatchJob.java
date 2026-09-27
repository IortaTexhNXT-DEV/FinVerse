package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_LETTER_BATCH} (FR-RN-081; cron {@code brokerverse.jobs.renewal-letter-batch-cron}):
 * updates the delivery status of the renewal letters queued for e-mail and raises {@code
 * RNW_LETTER_FAILED} for each failure.
 */
@Component
public class LetterBatchJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_LETTER_BATCH";

  private static final Logger LOG = LoggerFactory.getLogger(LetterBatchJob.class);

  private final LetterSweeps sweeps;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sweeps letter sweeps
   * @param cron schedule
   */
  public LetterBatchJob(
      LetterSweeps sweeps, @Value("${brokerverse.jobs.renewal-letter-batch-cron:-}") String cron) {
    this.sweeps = sweeps;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Updates the delivery status of the renewal letters sent by e-mail";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int failed = sweeps.refreshDeliveries();
    LOG.debug("Renewal letter deliveries refreshed on {}", businessDate);
    return new JobOutcome(failed, failed + " letter(s) failed");
  }
}
