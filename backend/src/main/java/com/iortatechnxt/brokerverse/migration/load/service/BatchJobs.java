package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The batch jobs of the migration (DATA_MIGRATION_DESIGN section 20): {@code MIG_VALIDATE}
 * validates planned batches, {@code MIG_LOAD} loads approved batches, {@code MIG_RECONCILE}
 * reconciles loaded batches without a clean run, {@code MIG_STAGING_PURGE} purges staging data
 * after the retention. Validation and load are on demand (manual unless a cron is set); the purge
 * runs daily at 02:00 Philippine time.
 */
@Configuration(proxyBeanMethods = false)
public class BatchJobs {

  private static final Logger LOG = LoggerFactory.getLogger(BatchJobs.class);

  /**
   * {@code MIG_VALIDATE}.
   *
   * @param batches batches
   * @param validation validation
   * @param cron schedule
   * @return job
   */
  @Bean
  public ManagedJob migValidateJob(
      MigBatchRepository batches,
      ValidationService validation,
      @Value("${brokerverse.jobs.mig-validate-cron:-}") String cron) {
    return job(
        MigrationJobs.VALIDATE,
        "Validates the planned migration batches against the code maps and the data-quality rules",
        cron,
        () -> batches.findByStatusInOrderByIdAsc(EnumSet.of(BatchStatus.PLANNED)),
        b -> validation.validate(b.getBatchNo()));
  }

  /**
   * {@code MIG_LOAD}.
   *
   * @param batches batches
   * @param runner load runner
   * @param cron schedule
   * @return job
   */
  @Bean
  public ManagedJob migLoadJob(
      MigBatchRepository batches,
      LoadRunner runner,
      @Value("${brokerverse.jobs.mig-load-cron:-}") String cron) {
    return job(
        MigrationJobs.LOAD,
        "Loads the approved migration batches through the owning services",
        cron,
        () -> batches.findByStatusInOrderByIdAsc(EnumSet.of(BatchStatus.APPROVED)),
        b -> runner.load(b.getBatchNo()));
  }

  /**
   * {@code MIG_RECONCILE}.
   *
   * @param batches batches
   * @param recon reconciliation
   * @param cron schedule
   * @return job
   */
  @Bean
  public ManagedJob migReconcileJob(
      MigBatchRepository batches,
      ReconciliationService recon,
      @Value("${brokerverse.jobs.mig-reconcile-cron:-}") String cron) {
    return job(
        MigrationJobs.RECONCILE,
        "Reconciles the loaded migration batches on counts, amounts, hash totals, fields and the GL",
        cron,
        () ->
            batches.findByStatusInOrderByIdAsc(
                EnumSet.of(BatchStatus.LOADED, BatchStatus.LOADED_WITH_REJECTS)),
        b -> recon.reconcile(b.getBatchNo()));
  }

  /**
   * {@code MIG_STAGING_PURGE}.
   *
   * @param purge purge
   * @param cron schedule
   * @return job
   */
  @Bean
  public ManagedJob migStagingPurgeJob(
      StagingPurgeService purge,
      @Value("${brokerverse.jobs.mig-staging-purge-cron:-}") String cron) {
    return new SimpleJob(
        MigrationJobs.PURGE,
        "Purges migration staging data and extract files after the retention days",
        cron,
        date -> {
          int n = purge.purge(date);
          return new JobOutcome(n, n + " batches or extracts purged");
        });
  }

  private static ManagedJob job(
      String name,
      String description,
      String cron,
      Supplier<List<MigBatch>> due,
      Consumer<MigBatch> work) {
    return new SimpleJob(
        name,
        description,
        cron,
        date -> {
          int done = 0;
          int failed = 0;
          for (MigBatch b : due.get()) {
            try {
              work.accept(b);
              done++;
            } catch (RuntimeException e) {
              failed++;
              LOG.warn("{} of batch {} failed: {}", name, b.getBatchNo(), e.getMessage());
            }
          }
          return new JobOutcome(done, done + " batches processed, " + failed + " failed");
        });
  }

  /** A job from a function of the business date. */
  private record SimpleJob(
      String name, String description, String cron, Function<LocalDate, JobOutcome> run)
      implements ManagedJob {

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      return run.apply(businessDate);
    }
  }
}
