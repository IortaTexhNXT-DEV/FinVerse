package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RunStatus;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code REMITTANCE_EXTRACTION} (RMTID.001/003/005): the off-peak extraction of every insurer and
 * remittance type of every company, including the invoices queued for the end of the day. The
 * schedule is {@code brokerverse.jobs.remittance-extraction-cron} (default 12:00 UTC = 20:00 PHT;
 * BDOI's batch schedule is parked, OQ17).
 */
@Component
public class RemittanceExtractionJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "REMITTANCE_EXTRACTION";

  private final ExtractionService extraction;
  private final OrganizationService organization;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param extraction extraction
   * @param organization companies
   * @param cron schedule ("-" = manual only)
   */
  public RemittanceExtractionJob(
      ExtractionService extraction,
      OrganizationService organization,
      @Value("${brokerverse.jobs.remittance-extraction-cron:-}") String cron) {
    this.extraction = extraction;
    this.organization = organization;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Extracts the paid and cleared premium due to insurers into remittance batches";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<ExtractionRun> runs = new ArrayList<>();
    for (Company company : organization.listCompanies()) {
      runs.add(
          extraction.run(
              company.getId(),
              new Scope(ExtractionTrigger.SCHEDULED, null, null, null),
              businessDate));
    }
    int extracted = runs.stream().mapToInt(ExtractionRun::getExtractedCount).sum();
    int batches = runs.stream().mapToInt(ExtractionRun::getBatchCount).sum();
    long failed = runs.stream().filter(r -> r.getStatus() == RunStatus.FAILED).count();
    return new JobOutcome(
        extracted,
        extracted
            + " invoice(s) extracted into "
            + batches
            + " batch(es) in "
            + runs.size()
            + " run(s)"
            + (failed == 0 ? "" : ", " + failed + " failed"));
  }
}
