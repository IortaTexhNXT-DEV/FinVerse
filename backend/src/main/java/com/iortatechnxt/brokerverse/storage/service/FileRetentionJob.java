package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * {@code FILE_RETENTION}: removes the objects of files whose retention ended (record class mapped
 * to the retention rules) and of files soft-deleted longer than {@code
 * brokerverse.storage.deleted-grace} ago. Files under legal hold are never removed; they are
 * counted in the run message. Announced inbound uploads never confirmed are closed.
 */
@Component
public class FileRetentionJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "FILE_RETENTION";

  private final StorageHousekeeping housekeeping;
  private final StorageProperties properties;

  /**
   * Creates the job.
   *
   * @param housekeeping units of work
   * @param properties storage settings (cron)
   */
  public FileRetentionJob(StorageHousekeeping housekeeping, StorageProperties properties) {
    this.housekeeping = housekeeping;
    this.properties = properties;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Removes stored files past retention or deleted (never under legal hold)";
  }

  @Override
  public String cron() {
    return properties.jobs().retentionCron();
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    housekeeping.abandonedUploads().forEach(housekeeping::abandon);
    int expired = 0;
    for (Long id : housekeeping.retentionDue(businessDate)) {
      expired += housekeeping.purge(id, "retention ended") ? 1 : 0;
    }
    int deleted = 0;
    for (Long id : housekeeping.deletedDue()) {
      deleted += housekeeping.purge(id, "deleted") ? 1 : 0;
    }
    long held = housekeeping.heldPastRetention(businessDate);
    return new JobOutcome(
        expired + deleted,
        expired
            + " past retention and "
            + deleted
            + " deleted files removed; "
            + held
            + " past retention kept under legal hold");
  }
}
