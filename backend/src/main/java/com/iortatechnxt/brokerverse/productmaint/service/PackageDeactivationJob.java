package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily step PACKAGE_DEACTIVATION (BDOI FRS FRPM.003.05 and FRPM.003.07; cron {@code
 * brokerverse.jobs.package-deactivation-cron}): deactivates the products of approved deactivation
 * requests whose expiry date has passed. Package versions are expired by the catalog's daily step.
 */
@Component
public class PackageDeactivationJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PACKAGE_DEACTIVATION";

  private final DeactivationService deactivations;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param deactivations deactivation requests
   * @param cron schedule
   */
  public PackageDeactivationJob(
      DeactivationService deactivations,
      @Value("${brokerverse.jobs.package-deactivation-cron:0 5 17 * * *}") String cron) {
    this.deactivations = deactivations;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Deactivates the products of approved deactivation requests after their expiry date";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int done = deactivations.applyDue(businessDate);
    return new JobOutcome(done, done + " product(s) deactivated");
  }
}
