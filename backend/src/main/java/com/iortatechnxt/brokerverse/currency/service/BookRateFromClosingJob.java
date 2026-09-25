package com.iortatechnxt.brokerverse.currency.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Job {@value RevaluationRateService#JOB_NAME} (FRBS 2.2.0, OQ08 proposal): copies the revaluation
 * rates of the month that ends on the business date (or of the previous month on any other day) as
 * the BOOK rates of the following month. Cron {@code brokerverse.jobs.book-rate-from-closing-cron}
 * (default 16:30 UTC on the last day = 00:30 Manila on the 1st).
 */
@Component
public class BookRateFromClosingJob implements ManagedJob {

  private final RevaluationRateService service;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param service revaluation rates
   * @param cron schedule, "-" for manual only
   */
  public BookRateFromClosingJob(
      RevaluationRateService service,
      @Value("${brokerverse.jobs.book-rate-from-closing-cron:-}") String cron) {
    this.service = service;
    this.cron = cron;
  }

  @Override
  public String name() {
    return RevaluationRateService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Copies the month-end revaluation rates as the BOOK rates of the next month (FRBS 2.2.0)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    YearMonth month = YearMonth.from(businessDate.plusDays(1)).minusMonths(1);
    int created = service.copyToBook(month, false).size();
    return new JobOutcome(created, created + " BOOK rate(s) copied from the rates of " + month);
  }
}
