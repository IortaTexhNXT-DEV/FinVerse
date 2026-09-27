package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_NRNS_LETTERS} (FR-RN-083; cron {@code brokerverse.jobs.renewal-nrns-letters-cron}):
 * flags the renewals not submitted at the NRNS checkpoint and sends each one reminder letter.
 */
@Component
public class NrnsLettersJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_NRNS_LETTERS";

  private static final Logger LOG = LoggerFactory.getLogger(NrnsLettersJob.class);

  private final LetterSweeps sweeps;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sweeps letter sweeps
   * @param cron schedule
   */
  public NrnsLettersJob(
      LetterSweeps sweeps, @Value("${brokerverse.jobs.renewal-nrns-letters-cron:-}") String cron) {
    this.sweeps = sweeps;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Flags the renewals not submitted at the NRNS checkpoint and sends their reminder";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int done = 0;
    int failed = 0;
    for (Long id : sweeps.nrnsDue(businessDate)) {
      try {
        done += sweeps.remind(id) ? 1 : 0;
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("NRNS reminder of renewal {} not sent: {}", id, e.getMessage());
      }
    }
    return new JobOutcome(done, done + " reminder(s) sent, " + failed + " failed");
  }
}
