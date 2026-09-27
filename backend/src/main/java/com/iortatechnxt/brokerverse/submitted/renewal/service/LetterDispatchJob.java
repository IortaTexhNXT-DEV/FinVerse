package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.renewal.service.LetterService.Dispatch;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code SBM_LETTER_DISPATCH} (BRIDSP-22; cron {@code brokerverse.jobs.sbm-letter-dispatch-cron},
 * 06:30 PHT): the letters due by the letter rules and the print batches of the day, one
 * transaction per company.
 */
@Component
public class LetterDispatchJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_LETTER_DISPATCH";

  private static final Logger LOG = LoggerFactory.getLogger(LetterDispatchJob.class);

  private final LetterService letters;
  private final CompanyRepository companies;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param letters letters
   * @param companies companies
   * @param transactions transaction manager
   * @param cron schedule
   */
  public LetterDispatchJob(
      LetterService letters,
      CompanyRepository companies,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.sbm-letter-dispatch-cron:-}") String cron) {
    this.letters = letters;
    this.companies = companies;
    this.tx = new TransactionTemplate(transactions);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends the letters on submitted policies that are due and builds the print batches";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int produced = 0;
    int batches = 0;
    for (Company company : companies.findAll()) {
      try {
        Dispatch d = tx.execute(s -> letters.dispatch(company.getId(), businessDate));
        if (d != null) {
          produced += d.letters();
          batches += d.printBatches();
        }
      } catch (RuntimeException e) {
        LOG.warn("Letter dispatch of {} failed: {}", company.getCode(), e.getMessage());
      }
    }
    return new JobOutcome(produced, produced + " letters, " + batches + " print batches");
  }
}
