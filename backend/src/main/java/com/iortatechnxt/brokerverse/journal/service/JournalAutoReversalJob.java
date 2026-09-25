package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily job {@value JournalAutoReversalService#JOB_NAME} (FRBS 2.8.1): posts the automatic reversal
 * of every journal whose reversal date has come. Cron {@code
 * brokerverse.jobs.journal-auto-reversal-cron} (default 16:05 UTC = 00:05 PHT).
 */
@Component
public class JournalAutoReversalJob implements ManagedJob {

  private final JournalAutoReversalService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service reversal service
   * @param cron schedule, "-" for manual only
   */
  public JournalAutoReversalJob(
      JournalAutoReversalService service,
      @Value("${brokerverse.jobs.journal-auto-reversal-cron:-}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JournalAutoReversalService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Posts the automatic reversal of accruals on their reversal date (FRBS 2.8.1)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    JournalAutoReversalService.Result result = service.reverseDue(businessDate);
    String message =
        result.reversed().size()
            + " journal(s) reversed"
            + (result.failed().isEmpty() ? "" : "; not reversed: " + result.failed());
    return new JobOutcome(result.reversed().size(), message);
  }
}
