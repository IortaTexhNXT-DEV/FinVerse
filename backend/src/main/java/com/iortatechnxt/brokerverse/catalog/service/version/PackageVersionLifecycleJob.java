package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily lifecycle step of the package versions (BRPM.006/017; PRODUCT_MAINTENANCE_DESIGN section
 * 5.1): supersedes versions whose selling period ended, projects versions that took effect and
 * expires packages whose end date passed ({@link ProductExpired}). Runs on the package expiry
 * schedule ({@code brokerverse.jobs.package-expiry-cron}), next to the {@code
 * PACKAGE_EXPIRY_MONITOR} of {@code productmaint}, which raises the expiry alerts and drafts
 * renewal requests.
 */
@Component
public class PackageVersionLifecycleJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PACKAGE_VERSION_LIFECYCLE";

  private final ProductVersionService versions;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param versions package versions
   * @param cron schedule ({@code brokerverse.jobs.package-expiry-cron})
   */
  public PackageVersionLifecycleJob(
      ProductVersionService versions,
      @Value("${brokerverse.jobs.package-expiry-cron:0 0 17 * * *}") String cron) {
    this.versions = versions;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Supersedes ended package versions, applies versions that took effect and expires"
        + " packages whose end date passed";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    ProductVersionService.ExpiryOutcome outcome = versions.expireDue(businessDate);
    return new JobOutcome(
        outcome.superseded() + outcome.projected() + outcome.expired(),
        outcome.superseded()
            + " superseded, "
            + outcome.projected()
            + " applied, "
            + outcome.expired()
            + " expired");
  }
}
