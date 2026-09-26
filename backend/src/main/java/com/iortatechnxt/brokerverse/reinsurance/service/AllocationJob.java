package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Scheduled RI allocation: cedes, for every active company, the transactions approved from the
 * start of the business year to the business date that are not yet ceded. Manual only by default
 * ({@code brokerverse.jobs.ri-allocation-cron}); the reinsurance screen runs it for a chosen
 * period.
 */
@Component
public class AllocationJob implements ManagedJob {

  private final AllocationRunService runs;
  private final OrganizationService organization;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param runs allocation run
   * @param organization companies
   * @param cron schedule ({@code brokerverse.jobs.ri-allocation-cron}, "-" = manual only)
   */
  public AllocationJob(
      AllocationRunService runs,
      OrganizationService organization,
      @Value("${brokerverse.jobs.ri-allocation-cron:-}") String cron) {
    this.runs = runs;
    this.organization = organization;
    this.cron = cron;
  }

  @Override
  public String name() {
    return AllocationRunService.JOB_NAME;
  }

  @Override
  public String description() {
    return "Cedes approved premium transactions not yet ceded to the reinsurance programme";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int items = 0;
    StringBuilder message = new StringBuilder();
    for (Company c : organization.listCompanies()) {
      if (!c.isActive()) {
        continue;
      }
      JobOutcome o = runs.cedePeriod(c.getId(), businessDate.withDayOfYear(1), businessDate);
      items += o.itemsProcessed();
      message.append(c.getCode()).append(": ").append(o.message()).append("; ");
    }
    return new JobOutcome(items, message.toString());
  }
}
