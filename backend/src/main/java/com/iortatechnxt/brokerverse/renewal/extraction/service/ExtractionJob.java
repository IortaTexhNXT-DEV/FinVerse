package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_EXTRACTION} (BRRN.030; FR-RN-010; cron {@code
 * brokerverse.jobs.renewal-extraction-cron}, 01:00 PHT): extracts, for every company, the policies
 * whose expiry is the business date plus the lead days of their segment, with the checks. A failed
 * company raises {@code RNW_EXTRACTION_FAILED}; the next run takes the missed dates.
 */
@Component
public class ExtractionJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_EXTRACTION";

  private static final Logger LOG = LoggerFactory.getLogger(ExtractionJob.class);

  private final ExtractionService extraction;
  private final CompanyRepository companies;
  private final AlertService alerts;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param extraction extraction
   * @param companies companies
   * @param alerts alerts
   * @param cron schedule ({@code brokerverse.jobs.renewal-extraction-cron})
   */
  public ExtractionJob(
      ExtractionService extraction,
      CompanyRepository companies,
      AlertService alerts,
      @Value("${brokerverse.jobs.renewal-extraction-cron:-}") String cron) {
    this.extraction = extraction;
    this.companies = companies;
    this.alerts = alerts;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Extracts the policies reaching the renewal lead days as renewals and runs their checks";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int created = 0;
    int failed = 0;
    for (Company company : companies.findAll()) {
      try {
        ExtractionRun run = extraction.extractDue(company.getId(), businessDate);
        created += run.getNewCount();
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Renewal extraction of {} failed: {}", company.getCode(), e.getMessage());
        alerts.raise(
            RenewalCodes.ALERT_EXTRACTION_FAILED,
            new AlertFacts(
                company.getId(),
                null,
                RenewalCodes.ENTITY,
                JOB_NAME,
                "The renewal extraction of " + businessDate + " failed: " + e.getMessage(),
                null,
                RenewalCodes.ALERT_EXTRACTION_FAILED + ":" + company.getId() + ":" + businessDate));
      }
    }
    return new JobOutcome(created, created + " renewal(s) extracted, " + failed + " failed");
  }
}
