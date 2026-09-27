package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_EXPIRY_SWEEP} (FR-RN-083; cron {@code brokerverse.jobs.renewal-expiry-sweep-cron}):
 * closes the renewals past expiry plus the non-acceptance days as EXPIRED_UNRENEWED with the
 * non-acceptance letter.
 */
@Component
public class ExpirySweepJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_EXPIRY_SWEEP";

  private static final Logger LOG = LoggerFactory.getLogger(ExpirySweepJob.class);

  private final LetterSweeps sweeps;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sweeps letter sweeps
   * @param cron schedule
   */
  public ExpirySweepJob(
      LetterSweeps sweeps, @Value("${brokerverse.jobs.renewal-expiry-sweep-cron:-}") String cron) {
    this.sweeps = sweeps;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Closes the renewals expired without renewal and sends the non-acceptance letter";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int done = 0;
    int failed = 0;
    for (Long id : sweeps.expired(businessDate)) {
      try {
        sweeps.expire(id);
        done++;
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Renewal {} not closed at expiry: {}", id, e.getMessage());
      }
    }
    return new JobOutcome(done, done + " renewal(s) closed at expiry, " + failed + " failed");
  }
}
