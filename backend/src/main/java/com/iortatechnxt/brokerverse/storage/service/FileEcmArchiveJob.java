package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * {@code FILE_ECM_ARCHIVE}: publishes the final records of the classes marked "archive to ECM"
 * through the integration outbox ({@link StorageTopics#ECM_ARCHIVE_REQUESTED}). The ECM adapter
 * that consumes the event waits for the ECM interface specification of BDOI IT (DSQ02).
 */
@Component
public class FileEcmArchiveJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "FILE_ECM_ARCHIVE";

  private final StorageHousekeeping housekeeping;
  private final StorageProperties properties;

  /**
   * Creates the job.
   *
   * @param housekeeping units of work
   * @param properties storage settings (cron)
   */
  public FileEcmArchiveJob(StorageHousekeeping housekeeping, StorageProperties properties) {
    this.housekeeping = housekeeping;
    this.properties = properties;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Publishes final records of the ECM record classes for archiving in the ECM";
  }

  @Override
  public String cron() {
    return properties.jobs().ecmArchiveCron();
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int published = 0;
    for (Long id : housekeeping.ecmDue()) {
      published += housekeeping.publishEcm(id) ? 1 : 0;
    }
    return new JobOutcome(published, published + " records published for ECM archiving");
  }
}
