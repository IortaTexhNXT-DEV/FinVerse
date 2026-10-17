package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSourceRepository;
import com.iortatechnxt.brokerverse.submitted.service.port.SubmittedSourceFeed;
import com.iortatechnxt.brokerverse.submitted.service.port.SubmittedSourceFeed.FeedFile;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code SBM_INTAKE_PULL} (BRIDSP-01, 13; cron {@code brokerverse.jobs.sbm-intake-pull-cron}, off
 * until a transport is connected): pulls the files of every active source through {@link
 * SubmittedSourceFeed} and loads each like an upload of its source (validate, then commit).
 */
@Component
public class IntakePullJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SBM_INTAKE_PULL";

  private static final Logger LOG = LoggerFactory.getLogger(IntakePullJob.class);

  private final SubmittedSourceFeed feed;
  private final SbmSourceRepository sources;
  private final CompanyRepository companies;
  private final BulkService bulk;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param feed source feed
   * @param sources source register
   * @param companies companies
   * @param bulk uploads
   * @param cron schedule
   */
  public IntakePullJob(
      SubmittedSourceFeed feed,
      SbmSourceRepository sources,
      CompanyRepository companies,
      BulkService bulk,
      @Value("${brokerverse.jobs.sbm-intake-pull-cron:-}") String cron) {
    this.feed = feed;
    this.sources = sources;
    this.companies = companies;
    this.bulk = bulk;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Pulls the submitted policy source files from the bank systems and loads them";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int loaded = 0;
    int refused = 0;
    for (Company company : companies.findAll()) {
      for (SbmSource source : sources.findAllByOrderByCodeAsc()) {
        if (!source.isActive() || source.getBulkHandler() == null) {
          continue;
        }
        for (FeedFile file : feed.pull(company.getId(), source.getCode(), businessDate)) {
          try {
            BulkJob job =
                bulk.upload(
                    new BulkUpload(
                        company.getId(),
                        source.getBulkHandler(),
                        file.fileName(),
                        file.content(),
                        Map.of()));
            bulk.commit(job.getId());
            loaded++;
          } catch (RuntimeException e) {
            refused++;
            LOG.warn(
                "Pull of {} from {} refused: {}",
                file.fileName(),
                source.getCode(),
                e.getMessage());
          }
        }
      }
    }
    return new JobOutcome(loaded, loaded + " source files loaded, " + refused + " refused");
  }
}
