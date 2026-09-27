package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.fee.service.HandlingFeeService.Tagging;
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
 * {@code SBM_HANDLING_FEE_TAGGER} (BRIDSP-31; cron {@code
 * brokerverse.jobs.sbm-handling-fee-tagger-cron}, every 30 minutes): tags the unapplied payments of
 * the billed handling fees of every company, one transaction per company.
 */
@Component
public class HandlingFeeTaggerJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_HANDLING_FEE_TAGGER";

  private static final Logger LOG = LoggerFactory.getLogger(HandlingFeeTaggerJob.class);

  private final HandlingFeeService fees;
  private final CompanyRepository companies;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param fees handling fees
   * @param companies companies
   * @param transactions transaction manager
   * @param cron schedule
   */
  public HandlingFeeTaggerJob(
      HandlingFeeService fees,
      CompanyRepository companies,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.sbm-handling-fee-tagger-cron:-}") String cron) {
    this.fees = fees;
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
    return "Tags the unapplied payments of billed handling fees and has them applied with an official receipt";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int tagged = 0;
    int ambiguous = 0;
    for (Company company : companies.findAll()) {
      try {
        Tagging t = tx.execute(s -> fees.tag(company.getId()));
        if (t != null) {
          tagged += t.tagged();
          ambiguous += t.ambiguous().size();
        }
      } catch (RuntimeException e) {
        LOG.warn("Handling-fee tagging of {} failed: {}", company.getCode(), e.getMessage());
      }
    }
    return new JobOutcome(
        tagged, tagged + " payments tagged, " + ambiguous + " left for the handler");
  }
}
