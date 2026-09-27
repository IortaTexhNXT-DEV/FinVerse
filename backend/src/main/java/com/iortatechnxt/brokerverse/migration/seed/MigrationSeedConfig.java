package com.iortatechnxt.brokerverse.migration.seed;

import com.iortatechnxt.brokerverse.migration.intake.service.IntakeService;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.load.service.LoadRunner;
import com.iortatechnxt.brokerverse.migration.load.service.ValidationService;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.migration.signoff.service.SignoffService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The pipeline services of the migration storyline (seed runner and end-to-end test). */
@Configuration(proxyBeanMethods = false)
public class MigrationSeedConfig {

  /**
   * The pipeline services.
   *
   * @param intake intake
   * @param plans batches
   * @param validation validation
   * @param runner load runner
   * @param recon reconciliation
   * @param signoffs gates
   * @return services
   */
  @Bean
  public MigrationStoryline.Services migrationStorylineServices(
      IntakeService intake,
      BatchPlanService plans,
      ValidationService validation,
      LoadRunner runner,
      ReconciliationService recon,
      SignoffService signoffs) {
    return new MigrationStoryline.Services(intake, plans, validation, runner, recon, signoffs);
  }
}
