package com.iortatechnxt.brokerverse.renewal.kyc.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code RNW_KYC_MONITOR} (cron {@code brokerverse.jobs.renewal-kyc-monitor-cron}, daily by
 * default): refreshes the KYC status of the open renewal accounts.
 */
@Component
public class KycMonitoringJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_KYC_MONITOR";

  private final KycMonitoring monitoring;
  private final OrganizationService organization;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param monitoring KYC monitoring
   * @param organization companies
   * @param cron schedule
   */
  public KycMonitoringJob(
      KycMonitoring monitoring,
      OrganizationService organization,
      @Value("${brokerverse.jobs.renewal-kyc-monitor-cron:0 40 5 * * *}") String cron) {
    this.monitoring = monitoring;
    this.organization = organization;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Refreshes the KYC status of the open renewal accounts";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int changed = 0;
    for (Company company : organization.listCompanies()) {
      changed += monitoring.refresh(company.getId());
    }
    return new JobOutcome(changed, changed + " KYC status change(s)");
  }
}
