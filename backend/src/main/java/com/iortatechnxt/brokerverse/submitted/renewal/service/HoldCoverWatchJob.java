package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.renewal.service.RenewalFollowService.Watch;
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
 * {@code SBM_HOLD_COVER_WATCH} (BRIDSP-24, 32; cron {@code brokerverse.jobs.sbm-hold-cover-watch-cron},
 * 07:00 PHT): per company, the status of the handed-over renewals read from the Renewal module,
 * the insurers that have not accepted the hold cover and the hold covers of unbooked accounts.
 */
@Component
public class HoldCoverWatchJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_HOLD_COVER_WATCH";

  private static final Logger LOG = LoggerFactory.getLogger(HoldCoverWatchJob.class);

  private final RenewalFollowService follow;
  private final CompanyRepository companies;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param follow renewal follow-up
   * @param companies companies
   * @param transactions transaction manager
   * @param cron schedule
   */
  public HoldCoverWatchJob(
      RenewalFollowService follow,
      CompanyRepository companies,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.sbm-hold-cover-watch-cron:-}") String cron) {
    this.follow = follow;
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
    return "Follows the hold covers and the outcome of the renewals of submitted policies";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int alerted = 0;
    int closed = 0;
    for (Company company : companies.findAll()) {
      try {
        Watch w = tx.execute(s -> follow.watch(company.getId(), businessDate));
        if (w != null) {
          alerted += w.alerted();
          closed += w.closed();
        }
      } catch (RuntimeException e) {
        LOG.warn("Hold cover watch of {} failed: {}", company.getCode(), e.getMessage());
      }
    }
    return new JobOutcome(alerted + closed, alerted + " alerted, " + closed + " closed as not renewed");
  }
}
