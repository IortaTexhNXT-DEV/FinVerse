package com.iortatechnxt.brokerverse.renewal.billing.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_CLPC_BILLING} (cron {@code brokerverse.jobs.renewal-clpc-billing-cron}, daily by
 * default): the CBG Home Billing Generation Job every day and the FFY Motor Billing Generation Job
 * on the set days of the month (10th and 25th by default).
 */
@Component
public class BillingFileJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_CLPC_BILLING";

  private final BillingFiles files;
  private final OrganizationService organization;
  private final SystemParameterService parameters;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param files billing files
   * @param organization companies
   * @param parameters FFY days
   * @param cron schedule
   */
  public BillingFileJob(
      BillingFiles files,
      OrganizationService organization,
      SystemParameterService parameters,
      @Value("${brokerverse.jobs.renewal-clpc-billing-cron:0 15 6 * * *}") String cron) {
    this.files = files;
    this.organization = organization;
    this.parameters = parameters;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Generates the CLPC billing files of the CBG Home and FFY Motor accounts For Billing"
        + " Generation";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    boolean ffyDay =
        parameters.items("RNW_CLPC_FFY_DAYS").stream()
            .anyMatch(d -> d.strip().equals(String.valueOf(businessDate.getDayOfMonth())));
    int made = 0;
    for (Company company : organization.listCompanies()) {
      made += files.scheduled(company.getId(), BillingFiles.CBG_HOME, businessDate).size();
      if (ffyDay) {
        made += files.scheduled(company.getId(), BillingFiles.FFY, businessDate).size();
      }
    }
    return new JobOutcome(made, made + " billing file(s)");
  }
}
