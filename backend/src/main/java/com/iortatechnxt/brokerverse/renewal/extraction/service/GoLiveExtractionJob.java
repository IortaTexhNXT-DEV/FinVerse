package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_GOLIVE_EXTRACTION} (DMQ37; FR-RN-016; manual, cron {@code
 * brokerverse.jobs.renewal-golive-extraction-cron}): run once from the cut-over runbook after the
 * migration load is signed off. Takes over every migrated policy expiring from the business date
 * (go-live) to {@code MIG_GOLIVE_RENEWAL_TO}; a second run creates nothing new.
 */
@Component
public class GoLiveExtractionJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_GOLIVE_EXTRACTION";

  private final GoLiveService goLive;
  private final CompanyRepository companies;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param goLive go-live take-over
   * @param companies companies
   * @param cron schedule ({@code brokerverse.jobs.renewal-golive-extraction-cron})
   */
  public GoLiveExtractionJob(
      GoLiveService goLive,
      CompanyRepository companies,
      @Value("${brokerverse.jobs.renewal-golive-extraction-cron:-}") String cron) {
    this.goLive = goLive;
    this.companies = companies;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Takes over as renewals the migrated policies expiring in the go-live window";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int created = 0;
    int urgent = 0;
    for (Company company : companies.findAll()) {
      ExtractionRun run = goLive.takeOver(company.getId(), businessDate);
      created += run.getNewCount();
      urgent += run.getUrgentCount();
    }
    return new JobOutcome(created, created + " renewal(s) taken over, " + urgent + " urgent");
  }
}
