package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.service.port.ExtractInbox;
import com.iortatechnxt.brokerverse.migration.intake.service.port.ExtractInbox.InboxFile;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationJobs;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code MIG_INTAKE_SCAN} (DATA_MIGRATION_DESIGN section 20): takes the files waiting in the
 * extract inbox through the same intake checks as the console upload. With the console upload (no
 * inbox connected) the job has nothing to do; the schedule stays manual until an inbox is named.
 */
@Configuration(proxyBeanMethods = false)
public class IntakeJobs {

  /**
   * {@code MIG_INTAKE_SCAN}.
   *
   * @param inbox inbox
   * @param intake intake
   * @param cron schedule
   * @return job
   */
  @Bean
  public ManagedJob migIntakeScanJob(
      ExtractInbox inbox,
      IntakeService intake,
      @Value("${brokerverse.jobs.mig-intake-scan-cron:-}") String cron) {
    return new ManagedJob() {
      @Override
      public String name() {
        return MigrationJobs.INTAKE_SCAN;
      }

      @Override
      public String description() {
        return "Takes the legacy extracts waiting in the inbox through the intake checks";
      }

      @Override
      public String cron() {
        return cron;
      }

      @Override
      public Workload workload() {
        return Workload.INTEGRATION;
      }

      @Override
      public JobOutcome execute(LocalDate businessDate) {
        if (!inbox.connected()) {
          return new JobOutcome(
              0, "No extract inbox is connected; extracts are uploaded in the console");
        }
        int staged = 0;
        int rejected = 0;
        for (InboxFile f : inbox.waiting()) {
          MigExtract e =
              intake.receive(
                  f.companyId(),
                  new IntakeService.Upload(
                      null, "FULL", f.fileName(), f.content(), f.controlName(), f.control()));
          inbox.taken(f);
          if (e.getStatus() == ExtractStatus.REJECTED) {
            rejected++;
          } else {
            staged++;
          }
        }
        return new JobOutcome(
            staged + rejected, staged + " extracts staged, " + rejected + " rejected");
      }
    };
  }
}
