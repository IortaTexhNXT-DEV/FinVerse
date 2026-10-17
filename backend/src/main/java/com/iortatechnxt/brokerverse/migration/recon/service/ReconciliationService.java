package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.service.BatchLogger;
import com.iortatechnxt.brokerverse.migration.load.service.LoadListener;
import com.iortatechnxt.brokerverse.migration.load.service.LoaderRegistry;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRunRepository;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLineRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciliation of a loaded batch (BRID 1.1b; DATA_MIGRATION_DESIGN section 12; FR-DM-020): L1
 * counts (received = control; loaded + skipped + rejected + excluded = staged), L2 amounts per
 * control total (control file, staging, BIBS), L3 hash totals and cross-reference keys, L4 fields
 * of every loaded record read back from BIBS, and the object checks (L5 GL). A break is explained
 * with a reason and approved by a reconciliation approver; the reconciliation cannot be signed
 * while a break is open.
 */
@Service
@Transactional
public class ReconciliationService implements LoadListener {

  private final MigBatchRepository batches;
  private final MigExtractRepository extracts;
  private final StageRowRepository rows;
  private final MigReconRunRepository runs;
  private final ReconLineRepository lines;
  private final LoaderRegistry loaders;
  private final XrefService xrefs;
  private final List<ReconCheck> checks;
  private final BatchLogger log;
  private final DocumentNumberService numbers;
  private final MigrationParameters parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final ReconMeasures measures;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param extracts extracts
   * @param rows staged rows
   * @param runs runs
   * @param lines lines
   * @param loaders loaders
   * @param xrefs cross-references
   * @param checks object checks
   * @param log run log
   * @param numbers document numbers
   * @param parameters tolerance
   * @param alerts alerts
   * @param audit audit trail
   * @param measures reconciliation measures
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReconciliationService(
      MigBatchRepository batches,
      MigExtractRepository extracts,
      StageRowRepository rows,
      MigReconRunRepository runs,
      ReconLineRepository lines,
      LoaderRegistry loaders,
      XrefService xrefs,
      List<ReconCheck> checks,
      BatchLogger log,
      DocumentNumberService numbers,
      MigrationParameters parameters,
      AlertService alerts,
      AuditTrailService audit,
      ReconMeasures measures,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.extracts = extracts;
    this.rows = rows;
    this.runs = runs;
    this.lines = lines;
    this.loaders = loaders;
    this.xrefs = xrefs;
    this.checks = checks;
    this.log = log;
    this.numbers = numbers;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.measures = measures;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public void loaded(MigBatch batch) {
    reconcile(batch);
  }

  /**
   * Reconciles a batch by its number.
   *
   * @param batchNo batch
   * @return the run
   */
  public MigReconRun reconcile(String batchNo) {
    MigBatch batch =
        batches
            .findByBatchNo(batchNo)
            .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_BATCH, batchNo));
    if (!batch.getStatus().holdsLoad()) {
      throw new BusinessRuleException(
          "MIG_BATCH_STATUS",
          "Batch " + batchNo + " is " + batch.getStatus() + "; it has no load to reconcile");
    }
    return reconcile(batch);
  }

  private MigReconRun reconcile(MigBatch batch) {
    List<MigExtract> ext = extracts.findAllById(batch.getExtractIds());
    LocalDate asOf =
        ext.stream()
            .map(e -> e.getAsOf().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(BusinessClock.today(clock));
    MigReconRun run =
        runs.save(
            new MigReconRun(
                batch.getCompanyId(),
                numbers.next("MGR-" + BusinessClock.today(clock).getYear()),
                new MigReconRun.Scope(batch.getId(), null, batch.getObjectCode()),
                asOf,
                currentUser.username(),
                clock.instant()));
    List<StageRow> all = familyRows(batch);
    MigrationLoader loader = loaders.find(batch.getObjectCode()).orElse(null);
    List<ReconLineSpec> specs =
        measures.lines(batch, ext, all, loader, xrefs.ofBatch(batch.getId()));
    for (ReconCheck check : checks) {
      if (check.appliesTo(batch.getObjectCode())) {
        specs.addAll(check.lines(batch));
      }
    }
    BigDecimal tolerance = parameters.amountTolerance();
    for (ReconLineSpec s : specs) {
      lines.save(
          new ReconLine(
              run.getId(),
              s.level(),
              s.measure(),
              s.currency(),
              s.values(),
              tolerance,
              s.detail()));
    }
    summarise(run, batch);
    return run;
  }

  private List<StageRow> familyRows(MigBatch batch) {
    List<StageRow> all =
        new ArrayList<>(
            rows.findByBatchIdAndStatusInOrderByIdAsc(
                batch.getId(), EnumSet.allOf(RowStatus.class)));
    for (MigBatch child : batches.findByParentBatchIdOrderByIdAsc(batch.getId())) {
      all.addAll(
          rows.findByBatchIdAndStatusInOrderByIdAsc(child.getId(), EnumSet.allOf(RowStatus.class)));
    }
    return all;
  }

  private void summarise(MigReconRun run, MigBatch batch) {
    List<ReconLine> list = lines.findByRunIdOrderByLevelAscIdAsc(run.getId());
    int open = (int) list.stream().filter(l -> l.getStatus() == ReconLine.Status.BREAK).count();
    int explained =
        (int) list.stream().filter(l -> l.getStatus() == ReconLine.Status.EXPLAINED).count();
    run.summarise(open, explained);
    if (open == 0) {
      batch.reconciled();
      log.info(batch, "RECONCILE", "Reconciled (" + run.getRunNo() + "): " + run.getStatus());
    } else {
      log.warn(batch, "RECONCILE", run.getRunNo() + ": " + open + " breaks");
      alerts.raise(
          MigrationCodes.ALERT_RECON_BREAK,
          new AlertFacts(
              batch.getCompanyId(),
              null,
              MigrationCodes.ENTITY_BATCH,
              batch.getBatchNo(),
              "Reconciliation "
                  + run.getRunNo()
                  + " of batch "
                  + batch.getBatchNo()
                  + " has "
                  + open
                  + " breaks",
              null,
              MigrationCodes.ALERT_RECON_BREAK + ":" + run.getRunNo()));
    }
  }

  /**
   * Explains a break.
   *
   * @param lineId line
   * @param reason reason code
   * @param text explanation
   * @return the line
   */
  public ReconLine explain(Long lineId, String reason, String text) {
    ReconLine line = line(lineId);
    line.explain(reason, text, currentUser.username(), clock.instant());
    audit.record("MigReconLine", lineId, AuditAction.UPDATE, "Explained (" + reason + "): " + text);
    return line;
  }

  /**
   * Approves the explanation of a break.
   *
   * @param lineId line
   * @return the line
   */
  public ReconLine approve(Long lineId) {
    ReconLine line = line(lineId);
    line.approve(currentUser.username(), clock.instant());
    MigReconRun run = runs.findById(line.getRunId()).orElseThrow();
    List<ReconLine> list = lines.findByRunIdOrderByLevelAscIdAsc(run.getId());
    int open = (int) list.stream().filter(l -> l.getStatus() == ReconLine.Status.BREAK).count();
    int explained =
        (int) list.stream().filter(l -> l.getStatus() == ReconLine.Status.EXPLAINED).count();
    run.summarise(open, explained);
    if (open == 0 && run.getBatchId() != null) {
      batches.findById(run.getBatchId()).ifPresent(MigBatch::reconciled);
    }
    audit.record("MigReconLine", lineId, AuditAction.AUTHORIZE, "Explanation approved");
    return line;
  }

  private ReconLine line(Long lineId) {
    return lines
        .findById(lineId)
        .orElseThrow(() -> new ResourceNotFoundException("MigReconLine", lineId));
  }

  /**
   * The latest run of a batch.
   *
   * @param batchId batch
   * @return run
   */
  @Transactional(readOnly = true)
  public Optional<MigReconRun> latest(Long batchId) {
    return runs.findByBatchIdOrderByIdDesc(batchId).stream().findFirst();
  }

  /**
   * The lines of a run.
   *
   * @param runId run
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<ReconLine> lines(Long runId) {
    return lines.findByRunIdOrderByLevelAscIdAsc(runId);
  }
}
