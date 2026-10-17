package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService.MonitorRun;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily package expiry monitor PACKAGE_EXPIRY_MONITOR (BRPM.017; cron {@code
 * brokerverse.jobs.package-expiry-cron}): per company, raises PACKAGE_EXPIRING for the released
 * packages reaching their end date within the notice period (again at the reminder days) and, when
 * PACKAGE_RENEWAL_AUTODRAFT is on, drafts their RENEW requests; it also copies the PKG_SLA_*
 * parameters into the workflow stages. The version state changes (SUPERSEDED / EXPIRED) belong to
 * the catalog, which publishes {@code ProductExpired}.
 */
@Component
public class PackageExpiryMonitorJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PACKAGE_EXPIRY_MONITOR";

  private final ProductVersionQueryService versions;
  private final PackageExpiryService expiry;
  private final PackageSlaSync slas;
  private final OrganizationService organization;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param versions catalog package versions
   * @param expiry expiry monitor
   * @param slas SLA parameter synchronisation
   * @param organization companies
   * @param cron schedule ({@code brokerverse.jobs.package-expiry-cron})
   */
  public PackageExpiryMonitorJob(
      ProductVersionQueryService versions,
      PackageExpiryService expiry,
      PackageSlaSync slas,
      OrganizationService organization,
      @Value("${brokerverse.jobs.package-expiry-cron:0 0 17 * * *}") String cron) {
    this.versions = versions;
    this.expiry = expiry;
    this.slas = slas;
    this.organization = organization;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Alerts TSU and MBS of packages reaching their end date and drafts renewal requests";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int alerted = 0;
    int drafted = 0;
    for (Company company : organization.listCompanies()) {
      MonitorRun run = expiry.monitor(versions, company.getId());
      alerted += run.alerted();
      drafted += run.drafted();
    }
    int stages = slas.sync();
    return new JobOutcome(
        alerted + drafted,
        alerted
            + " package expiry alert(s); "
            + drafted
            + " renewal request(s) drafted; "
            + stages
            + " stage SLA(s) updated");
  }
}
