package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Job {@value BrokingBooksCloseService#JOB_NAME} (FRBS 3.4.1): cuts off the broking books at month
 * end. Cron {@code brokerverse.jobs.broking-books-close-cron} (default 15:00 UTC on the last day =
 * 23:00 Manila, parameter {@code BROKING_CLOSE_TIME}); on other days it does nothing.
 */
@Component
public class BrokingBooksCloseJob implements ManagedJob {

  private final BrokingBooksCloseService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service broking books cut-off
   * @param cron schedule, "-" for manual only
   */
  public BrokingBooksCloseJob(
      BrokingBooksCloseService service,
      @Value("${brokerverse.jobs.broking-books-close-cron:-}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return BrokingBooksCloseService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Closes the broking books of the month on its last day (FRBS 3.4.0 / 3.4.1)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<String> closed = service.closeMonthEnd(businessDate);
    return new JobOutcome(
        closed.size(),
        closed.isEmpty()
            ? "Not a month end or already closed: nothing to do"
            : "Broking books closed: " + String.join(", ", closed));
  }
}
