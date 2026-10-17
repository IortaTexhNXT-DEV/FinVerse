package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.storage.BucketClass;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.FileStoreException;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.ObjectSummary;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code FILE_ORPHAN_RECONCILIATION}: the object is written before its metadata row, so a failed
 * transaction leaves an object without a row. Objects older than {@code
 * brokerverse.storage.orphan-age} (24 hours) without a row are deleted (DOCUMENT_STORAGE_DECISION
 * section 3 "Transactions"); objects under legal hold are kept.
 */
@Component
public class FileOrphanReconciliationJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "FILE_ORPHAN_RECONCILIATION";

  private static final Logger LOG = LoggerFactory.getLogger(FileOrphanReconciliationJob.class);
  private static final int PAGE = 500;

  private final FileStore store;
  private final StorageHousekeeping housekeeping;
  private final StoredFileService files;
  private final StorageProperties properties;
  private final Clock clock;

  /**
   * Creates the job.
   *
   * @param store object store
   * @param housekeeping units of work
   * @param files bucket names
   * @param properties storage settings
   * @param clock clock
   */
  public FileOrphanReconciliationJob(
      FileStore store,
      StorageHousekeeping housekeeping,
      StoredFileService files,
      StorageProperties properties,
      Clock clock) {
    this.store = store;
    this.housekeeping = housekeeping;
    this.files = files;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Deletes stored objects that have had no metadata row for 24 hours";
  }

  @Override
  public String cron() {
    return properties.jobs().orphanCron();
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Instant cutoff = clock.instant().minus(properties.orphanAge());
    Pass pass = new Pass(cutoff);
    for (BucketClass bucket : BucketClass.values()) {
      try {
        store.list(bucket, "", pass::accept);
      } catch (FileStoreException e) {
        LOG.info("Bucket class {} skipped: {}", bucket, e.getMessage());
      }
      pass.flush();
    }
    return new JobOutcome(
        pass.deleted,
        pass.deleted
            + " orphan objects deleted of "
            + pass.checked
            + " objects older than 24 hours");
  }

  /** One run: collects old objects page by page and deletes those without a row. */
  private final class Pass {

    private final Instant cutoff;
    private final List<ObjectRef> page = new ArrayList<>();
    private int checked;
    private int deleted;

    private Pass(Instant cutoff) {
      this.cutoff = cutoff;
    }

    private void accept(ObjectSummary object) {
      if (object.lastModified() != null && object.lastModified().isBefore(cutoff)) {
        page.add(object.ref());
        checked++;
        if (page.size() >= PAGE) {
          flush();
        }
      }
    }

    private void flush() {
      if (page.isEmpty()) {
        return;
      }
      String bucket = files.bucketName(page.get(0));
      Set<String> known =
          housekeeping.knownKeys(bucket, page.stream().map(ObjectRef::key).toList());
      for (ObjectRef ref : page) {
        if (!known.contains(ref.key()) && housekeeping.deleteOrphan(ref)) {
          deleted++;
        }
      }
      page.clear();
    }
  }
}
