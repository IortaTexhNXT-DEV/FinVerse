package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.renewal.service.ExpiryScanService.Scan;
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
 * {@code SBM_EXPIRY_SCAN} (BRIDSP-23; cron {@code brokerverse.jobs.sbm-expiry-scan-cron}, 22:00
 * PHT): the expiry scan of every company, one transaction per company.
 */
@Component
public class ExpiryScanJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_EXPIRY_SCAN";

  private static final Logger LOG = LoggerFactory.getLogger(ExpiryScanJob.class);

  private final ExpiryScanService scans;
  private final CompanyRepository companies;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param scans expiry scan
   * @param companies companies
   * @param transactions transaction manager
   * @param cron schedule
   */
  public ExpiryScanJob(
      ExpiryScanService scans,
      CompanyRepository companies,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.sbm-expiry-scan-cron:-}") String cron) {
    this.scans = scans;
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
    return "Hands the submitted policies for renewal that near their expiry to Renewal";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int handed = 0;
    int noInsurer = 0;
    int replayed = 0;
    for (Company company : companies.findAll()) {
      try {
        Scan scan = tx.execute(s -> scans.scan(company.getId(), businessDate));
        if (scan != null) {
          handed += scan.handedOff();
          noInsurer += scan.noInsurer();
          replayed += scan.replayed();
        }
      } catch (RuntimeException e) {
        LOG.warn("Expiry scan of {} failed: {}", company.getCode(), e.getMessage());
      }
    }
    return new JobOutcome(
        handed,
        handed
            + " handed to Renewal, "
            + noInsurer
            + " without an insurer rule, "
            + replayed
            + " pending hand-offs taken by Renewal");
  }
}
