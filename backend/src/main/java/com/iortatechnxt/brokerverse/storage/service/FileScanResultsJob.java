package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * {@code FILE_SCAN_RESULTS}: reads the malware scan tag of files still {@code PENDING}; clean files
 * become downloadable, others are quarantined (DOCUMENT_STORAGE_DECISION, decision 2).
 */
@Component
public class FileScanResultsJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "FILE_SCAN_RESULTS";

  private static final int BATCH = 500;

  private final FileScanService scans;
  private final StorageProperties properties;

  /**
   * Creates the job.
   *
   * @param scans scan results
   * @param properties storage settings (cron)
   */
  public FileScanResultsJob(FileScanService scans, StorageProperties properties) {
    this.scans = scans;
    this.properties = properties;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Reads the malware scan results of stored files and quarantines infected ones";
  }

  @Override
  public String cron() {
    return properties.jobs().scanResultsCron();
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int clean = 0;
    int quarantined = 0;
    int pending = 0;
    for (Long id : scans.pendingIds(BATCH)) {
      ScanStatus status = scans.refresh(id);
      if (status == ScanStatus.CLEAN) {
        clean++;
      } else if (status == ScanStatus.QUARANTINED) {
        quarantined++;
      } else {
        pending++;
      }
    }
    return new JobOutcome(
        clean + quarantined,
        clean + " clean, " + quarantined + " quarantined, " + pending + " still pending");
  }
}
