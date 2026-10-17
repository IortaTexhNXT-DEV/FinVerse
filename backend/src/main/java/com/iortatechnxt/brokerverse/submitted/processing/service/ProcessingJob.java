package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code SBM_PROCESSING} (BRIDSP-09; FRS FR-SP-021; cron {@code
 * brokerverse.jobs.sbm-processing-cron}, 21:30 PHT): for every company, one processing run of the
 * records received, validated, classified, in review or waiting for a manual disposition, in chunks
 * of 200 records per transaction so a failing chunk does not undo the others.
 */
@Component
public class ProcessingJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_PROCESSING";

  private static final int CHUNK = 200;
  private static final Logger LOG = LoggerFactory.getLogger(ProcessingJob.class);

  private final SbmProcessingService processing;
  private final CompanyRepository companies;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param processing processing runs
   * @param companies companies
   * @param transactions transaction manager
   * @param cron schedule
   */
  public ProcessingJob(
      SbmProcessingService processing,
      CompanyRepository companies,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.sbm-processing-cron:-}") String cron) {
    this.processing = processing;
    this.companies = companies;
    this.tx = new TransactionTemplate(transactions);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Classifies and buckets the submitted policies received or changed, with the approved rules";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int processed = 0;
    int failedChunks = 0;
    for (Company company : companies.findAll()) {
      List<Long> ids = processing.scheduledScope(company.getId());
      if (ids.isEmpty()) {
        continue;
      }
      SbmRun run =
          tx.execute(
              s ->
                  processing.start(
                      new RunRequest(
                          company.getId(), SbmRun.Trigger.SCHEDULED, "Daily run", List.of())));
      for (int i = 0; i < ids.size(); i += CHUNK) {
        List<Long> chunk = ids.subList(i, Math.min(ids.size(), i + CHUNK));
        try {
          tx.executeWithoutResult(s -> processing.process(run.getId(), chunk));
          processed += chunk.size();
        } catch (RuntimeException e) {
          failedChunks++;
          LOG.warn("Processing chunk of run {} failed: {}", run.getRunNo(), e.getMessage());
        }
      }
      tx.executeWithoutResult(s -> processing.finish(run.getId()));
    }
    return new JobOutcome(
        processed,
        processed
            + " submitted policies processed"
            + (failedChunks > 0 ? ", " + failedChunks + " chunk(s) failed" : ""));
  }
}
