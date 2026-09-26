package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Daily job generating the due occurrences of all active recurring journal templates. */
@Component
public class RecurringJournalJob implements ManagedJob {

  private final RecurringJournalService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service recurring journal service
   * @param cron schedule ({@code brokerverse.jobs.recurring-journals-cron})
   */
  public RecurringJournalJob(
      RecurringJournalService service,
      @Value("${brokerverse.jobs.recurring-journals-cron:0 0 1 * * *}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return RecurringJournalService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Generates due recurring and accrual journals (drafts or submitted for approval)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    return RecurringJournalService.outcome(
        new AtomicReference<>(), service.generateDue(businessDate), "All templates");
  }
}
