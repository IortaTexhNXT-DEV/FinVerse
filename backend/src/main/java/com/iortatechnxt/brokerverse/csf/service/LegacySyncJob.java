package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code CSF_LEGACY_SYNC} (FR-CSF-022; cron {@code brokerverse.jobs.csf-legacy-sync-cron}, every 15
 * minutes once the legacy interface exists, manual only by default): sends the queued contact
 * changes to QPS and EBIX, one transaction per outbox row. Nothing is sent while {@code
 * CSF_LEGACY_SYNC_ENABLED} is off.
 */
@Component
public class LegacySyncJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "CSF_LEGACY_SYNC";

  private final LegacySyncService sync;
  private final CsfParameters parameters;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sync legacy write-back
   * @param parameters CSF parameters
   * @param transactions transaction manager
   * @param cron schedule
   */
  public LegacySyncJob(
      LegacySyncService sync,
      CsfParameters parameters,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.csf-legacy-sync-cron:-}") String cron) {
    this.sync = sync;
    this.parameters = parameters;
    this.tx = new TransactionTemplate(transactions);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends the client contact changes of the contact centre to the legacy policy systems";
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
    if (!parameters.legacySyncEnabled()) {
      return new JobOutcome(0, "Sending to the legacy systems is switched off; nothing was sent");
    }
    List<Long> rows = tx.execute(s -> sync.rowsToSend());
    int sent = 0;
    int failed = 0;
    for (Long id : rows == null ? List.<Long>of() : rows) {
      if (Boolean.TRUE.equals(tx.execute(s -> sync.send(id)))) {
        sent++;
      } else {
        failed++;
      }
    }
    return new JobOutcome(sent, sent + " contact changes sent, " + failed + " failed");
  }
}
