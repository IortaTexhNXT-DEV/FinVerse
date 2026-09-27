package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ends the load of a batch: recounts its rows, sets LOADED, LOADED_WITH_REJECTS or FAILED, writes
 * the run log, raises {@code MIG_LOAD_FAILED} when rows were rejected or the load stopped, and runs
 * the reconciliation (every load is reconciled, DATA_MIGRATION_DESIGN section 12).
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class LoadCompletion {

  private final MigBatchRepository batches;
  private final BatchCounter counter;
  private final BatchLogger log;
  private final List<LoadListener> listeners;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param counter counts
   * @param log run log
   * @param listeners steps run after a load (reconciliation)
   * @param alerts alerts
   * @param audit audit trail
   * @param clock clock
   */
  public LoadCompletion(
      MigBatchRepository batches,
      BatchCounter counter,
      BatchLogger log,
      List<LoadListener> listeners,
      AlertService alerts,
      AuditTrailService audit,
      Clock clock) {
    this.batches = batches;
    this.counter = counter;
    this.log = log;
    this.listeners = listeners;
    this.alerts = alerts;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Finishes a load.
   *
   * @param batchId batch
   * @param failed the load stopped on an error
   * @return the batch
   */
  public MigBatch finish(Long batchId, boolean failed) {
    MigBatch batch = batches.findById(batchId).orElseThrow();
    counter.recount(batch);
    batch.endLoad(failed, clock.instant());
    MigBatch.Counts c = batch.counts();
    String summary =
        "Loaded "
            + c.loaded()
            + ", skipped "
            + c.skipped()
            + ", rejected "
            + c.rejected()
            + ", excluded "
            + c.excluded()
            + " of "
            + c.staged();
    if (failed || c.rejected() > 0) {
      log.warn(batch, "LOAD", summary);
      alerts.raise(
          MigrationCodes.ALERT_LOAD_FAILED,
          new AlertFacts(
              batch.getCompanyId(),
              null,
              MigrationCodes.ENTITY_BATCH,
              batch.getBatchNo(),
              "Batch "
                  + batch.getBatchNo()
                  + (failed ? " stopped. " : " loaded with rejects. ")
                  + summary,
              null,
              MigrationCodes.ALERT_LOAD_FAILED + ":" + batch.getBatchNo()));
    } else {
      log.info(batch, "LOAD", summary);
    }
    audit.record(MigrationCodes.ENTITY_BATCH, batch.getBatchNo(), AuditAction.RUN, summary);
    if (!failed) {
      listeners.forEach(l -> l.loaded(batch));
    }
    return batch;
  }

  /**
   * Logs an error of a load.
   *
   * @param batchId batch
   * @param message message
   */
  public void logError(Long batchId, String message) {
    batches.findById(batchId).ifPresent(b -> log.error(b, "LOAD", message));
  }
}
