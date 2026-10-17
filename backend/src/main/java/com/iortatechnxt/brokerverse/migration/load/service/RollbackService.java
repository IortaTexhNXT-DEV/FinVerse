package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.LoaderIdentity;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationWorkflow;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rollback of a batch before sign-off (DATA_MIGRATION_DESIGN section 11; FR-DM-015): the Data
 * Migration Lead requests it with a reason and a reconciliation approver other than the requester
 * approves it (workflow MIG_BATCH_ROLLBACK). The loader undoes each record newest first through the
 * owning service; when a record changed after the load (for example a payment applied to a legacy
 * invoice) the rollback is refused and the environment snapshot is the remaining option.
 */
@Service
@Transactional
public class RollbackService {

  private final BatchPlanService plans;
  private final LoaderRegistry loaders;
  private final XrefService xrefs;
  private final StageRowRepository rows;
  private final BatchCounter counter;
  private final BatchLogger log;
  private final MigrationWorkflow workflow;
  private final MigrationParameters parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans batches
   * @param loaders loaders
   * @param xrefs cross-references
   * @param rows staged rows
   * @param counter counts
   * @param log run log
   * @param workflow workflow cases
   * @param parameters retention
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RollbackService(
      BatchPlanService plans,
      LoaderRegistry loaders,
      XrefService xrefs,
      StageRowRepository rows,
      BatchCounter counter,
      BatchLogger log,
      MigrationWorkflow workflow,
      MigrationParameters parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.plans = plans;
    this.loaders = loaders;
    this.xrefs = xrefs;
    this.rows = rows;
    this.counter = counter;
    this.log = log;
    this.workflow = workflow;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests the rollback of a loaded batch.
   *
   * @param batchNo batch
   * @param reason reason
   * @return the batch
   */
  public MigBatch request(String batchNo, String reason) {
    MigBatch batch = plans.get(batchNo);
    MigrationLoader loader = loaders.require(batch.getObjectCode());
    if (!loader.reversible()) {
      throw new BusinessRuleException(
          "MIG_ROLLBACK_NOT_SUPPORTED",
          "Object "
              + batch.getObjectCode()
              + " cannot be rolled back record by record; restore the environment snapshot");
    }
    BatchStatus before = batch.getStatus();
    int round = batch.requestRollback(reason, currentUser.username(), clock.instant());
    workflow.open(
        batch.getCompanyId(),
        MigrationCodes.WF_ROLLBACK,
        new CaseRecord(
            MigrationCodes.ENTITY_ROLLBACK,
            batchNo + ":" + round,
            batchNo,
            "Rollback of " + batch.getObjectCode() + " batch " + batchNo,
            "/migration/batches?batch=" + batchNo,
            null));
    log.info(batch, "ROLLBACK", "Rollback requested (status before: " + before + "): " + reason);
    audit.record(
        MigrationCodes.ENTITY_BATCH, batchNo, AuditAction.SUBMIT, "Rollback requested: " + reason);
    return batch;
  }

  /**
   * Approves the rollback and undoes the records of the batch.
   *
   * @param batchNo batch
   * @param comment comment
   * @return the batch, ROLLED_BACK
   */
  public MigBatch approve(String batchNo, String comment) {
    MigBatch batch = plans.get(batchNo);
    batch.requireStatus("rolled back", BatchStatus.ROLLBACK_REQUESTED);
    MigrationLoader loader = loaders.require(batch.getObjectCode());
    List<KeyXref> loaded = xrefs.ofBatch(batch.getId());
    long changed = loaded.stream().filter(loader::changedSinceLoad).count();
    if (changed > 0) {
      throw new BusinessRuleException(
          "MIG_ROLLBACK_CHANGED",
          changed + " records were changed after the load and cannot be rolled back");
    }
    batch.decideRollback(true, currentUser.username(), clock.instant(), BatchStatus.LOADED);
    LoadContext ctx = new LoadContext(batch, CodeMaps.empty(), BusinessClock.today(clock));
    LoaderIdentity.run(
        () -> {
          for (KeyXref x : loaded) {
            loader.compensate(x, ctx);
            x.rolledBack(clock.instant());
          }
        });
    rows.findByBatchIdAndStatusInOrderByIdAsc(
            batch.getId(), EnumSet.of(RowStatus.LOADED, RowStatus.SKIPPED))
        .forEach(r -> r.mark(RowStatus.ROLLED_BACK));
    counter.recount(batch);
    batch.rolledBack(
        clock.instant(), BusinessClock.today(clock).plusDays(parameters.retentionDays()));
    workflow.move(
        MigrationCodes.ENTITY_ROLLBACK,
        batchNo + ":" + batch.getRollbackRound(),
        "approve",
        comment);
    log.info(batch, "ROLLBACK", "Rolled back " + loaded.size() + " records");
    audit.record(
        MigrationCodes.ENTITY_BATCH,
        batchNo,
        AuditAction.REVERSE,
        "Rolled back " + loaded.size() + " records");
    return batch;
  }

  /**
   * Rejects the rollback request; the batch returns to its status before the request.
   *
   * @param batchNo batch
   * @param reason reason
   * @return the batch
   */
  public MigBatch reject(String batchNo, String reason) {
    MigBatch batch = plans.get(batchNo);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the rejection");
    }
    BatchStatus back =
        batch.counts().rejected() > 0 ? BatchStatus.LOADED_WITH_REJECTS : BatchStatus.LOADED;
    batch.decideRollback(false, currentUser.username(), clock.instant(), back);
    workflow.move(
        MigrationCodes.ENTITY_ROLLBACK, batchNo + ":" + batch.getRollbackRound(), "reject", reason);
    log.info(batch, "ROLLBACK", "Rollback rejected: " + reason);
    audit.record(
        MigrationCodes.ENTITY_BATCH, batchNo, AuditAction.REJECT, "Rollback rejected: " + reason);
    return batch;
  }
}
