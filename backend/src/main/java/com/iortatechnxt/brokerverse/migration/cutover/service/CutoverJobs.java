package com.iortatechnxt.brokerverse.migration.cutover.service;

import com.iortatechnxt.brokerverse.migration.load.service.MigrationJobs;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The scheduled jobs of the cutover and run-off (DATA_MIGRATION_DESIGN section 20). */
@Configuration(proxyBeanMethods = false)
public class CutoverJobs {

  /**
   * The monthly run-off snapshot of the legacy in-force headers, per company.
   *
   * @param runoff run-off
   * @param cron schedule
   * @return the job
   */
  @Bean
  public ManagedJob migRunoffSnapshotJob(
      RunoffService runoff, @Value("${brokerverse.jobs.mig-runoff-snapshot-cron:-}") String cron) {
    return new ManagedJob() {
      @Override
      public String name() {
        return MigrationJobs.RUNOFF_SNAPSHOT;
      }

      @Override
      public String description() {
        return "Counts the legacy in-force headers by expiry month: renewed, not renewed, lapsed"
            + " and still open";
      }

      @Override
      public String cron() {
        return cron;
      }

      @Override
      public JobOutcome execute(LocalDate businessDate) {
        int cohorts = 0;
        for (Long companyId : runoff.companies()) {
          cohorts += runoff.snapshot(companyId, businessDate).size();
        }
        return new JobOutcome(cohorts, cohorts + " run-off cohorts measured");
      }
    };
  }
}
