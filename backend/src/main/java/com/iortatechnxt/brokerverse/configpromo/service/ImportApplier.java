package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cache.service.CacheInvalidator;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDatasetRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImportRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.Analysis;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyActors;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyEngine;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyException;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetResult;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportAnalyzer;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.engine.Reconciler;
import com.iortatechnxt.brokerverse.configpromo.engine.Reconciliation;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Approves and applies an import: the approver is another user than the preparer; in production the
 * change window must be open; no period close or batch job may run (job locks); the configuration
 * of the datasets is kept as a snapshot first; then the package is applied in one transaction in
 * load order, reconciled, audited and the caches are cleared. A failure keeps nothing of the import
 * and is recorded on it.
 */
@Service
public class ImportApplier {

  private static final String ENTITY = "ConfigImport";

  private final ImportService imports;
  private final PromotionImportRepository repository;
  private final ImportDatasetRepository lines;
  private final DryRunRecorder recorder;
  private final PackageStore store;
  private final ExportService exports;
  private final CatalogueService catalogue;
  private final PromotionSettings settings;
  private final JobGuard guard;
  private final PromotionNotifier notifier;
  private final CacheInvalidator caches;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the applier.
   *
   * @param imports import lifecycle
   * @param repository imports
   * @param lines dataset lines
   * @param recorder dry run records
   * @param store package store
   * @param exports exports (snapshot)
   * @param catalogue catalogue
   * @param settings settings
   * @param guard job locks
   * @param notifier notifications
   * @param caches reference-data caches
   * @param audit audit trail
   * @param currentUser current user
   * @param jdbc database
   * @param txManager transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ImportApplier(
      ImportService imports,
      PromotionImportRepository repository,
      ImportDatasetRepository lines,
      DryRunRecorder recorder,
      PackageStore store,
      ExportService exports,
      CatalogueService catalogue,
      PromotionSettings settings,
      JobGuard guard,
      PromotionNotifier notifier,
      CacheInvalidator caches,
      AuditTrailService audit,
      CurrentUser currentUser,
      JdbcTemplate jdbc,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.imports = imports;
    this.repository = repository;
    this.lines = lines;
    this.recorder = recorder;
    this.store = store;
    this.exports = exports;
    this.catalogue = catalogue;
    this.settings = settings;
    this.guard = guard;
    this.notifier = notifier;
    this.caches = caches;
    this.audit = audit;
    this.currentUser = currentUser;
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * Approves an import and applies it (the approver, another user than the preparer).
   *
   * @param importId import
   * @param note remarks
   * @return the import, APPLIED or FAILED
   */
  public PromotionImport approve(Long importId, String note) {
    PromotionImport imp = imports.get(importId);
    requireWindow(imp);
    String approver = currentUser.username();
    JobLock.Lease lease = guard.acquire();
    try {
      tx.executeWithoutResult(
          s -> {
            PromotionImport current = imports.get(importId);
            current.decide(approver, clock.instant(), note);
            repository.save(current);
            audit.record(ENTITY, current.getImportNo(), AuditAction.AUTHORIZE, "Approved");
          });
      return apply(importId, imp.getPreparedBy(), approver);
    } finally {
      lease.close();
    }
  }

  /**
   * Applies the pipeline's own dry run outside production, when the parameter allows it.
   *
   * @param importId import, status CHECKED
   * @return the import, APPLIED or FAILED
   */
  public PromotionImport pipelineApply(Long importId) {
    if (!settings.pipelineApplyAllowed()) {
      throw new BusinessRuleException(
          "CONFIG_PIPELINE_APPLY_NOT_ALLOWED",
          "The pipeline may only prepare and submit the import here; a second user approves it");
    }
    String user = currentUser.username();
    JobLock.Lease lease = guard.acquire();
    try {
      tx.executeWithoutResult(
          s -> {
            PromotionImport current = imports.get(importId);
            if (!current.isCompatible() || current.getBlockerCount() > 0) {
              throw new BusinessRuleException(
                  "CONFIG_IMPORT_BLOCKED",
                  "The dry run reports blockers; the import is not applied");
            }
            current.pipelineApproval(user, clock.instant());
            repository.save(current);
            audit.record(
                ENTITY, current.getImportNo(), AuditAction.AUTHORIZE, "Applied by the pipeline");
          });
      return apply(importId, user, user);
    } finally {
      lease.close();
    }
  }

  private void requireWindow(PromotionImport imp) {
    if (!imp.isProduction()) {
      return;
    }
    String window = settings.productionWindow();
    if (window.isBlank()
        || !ChangeWindow.parse(window).isOpen(BusinessClock.now(clock).toLocalDateTime())) {
      throw new BusinessRuleException(
          "CONFIG_OUTSIDE_CHANGE_WINDOW",
          "Production imports are applied only in the change window"
              + (window.isBlank() ? ", which is not set" : " " + window));
    }
  }

  private PromotionImport apply(Long importId, String maker, String checker) {
    PromotionImport imp = imports.get(importId);
    PromotionPackage source = store.get(imp.getPackageId());
    ConfigPackage pkg = store.open(source);
    ImportOptions options = recorder.options(imp);
    try {
      List<String> codes = codes(pkg, options);
      PromotionPackage snapshot =
          tx.execute(
              s ->
                  exports.snapshot(
                      codes,
                      options.includeUsers(),
                      "Configuration before import " + imp.getImportNo()));
      tx.executeWithoutResult(
          s -> applyInTransaction(importId, pkg, options, snapshot, maker, checker));
      caches.clearAll();
      PromotionImport applied = imports.get(importId);
      notifier.decided(
          applied,
          "CONFIG_IMPORT_APPLIED",
          "Configuration import " + applied.getImportNo() + " applied",
          "Package " + source.getPackageNo() + " was applied and reconciled.");
      return applied;
    } catch (ApplyException | BusinessRuleException e) {
      return failed(importId, e.getMessage());
    }
  }

  private List<String> codes(ConfigPackage pkg, ImportOptions options) {
    return new ImportAnalyzer(jdbc, catalogue.reader(), u -> true).datasets(pkg, options);
  }

  private void applyInTransaction(
      Long importId,
      ConfigPackage pkg,
      ImportOptions options,
      PromotionPackage snapshot,
      String maker,
      String checker) {
    PromotionImport imp = imports.get(importId);
    DatasetReader reader = catalogue.reader();
    ImportAnalyzer analyzer = new ImportAnalyzer(jdbc, reader, u -> true);
    Analysis analysis = analyzer.analyze(pkg, options);
    if (!analysis.blockers().isEmpty()) {
      throw new ApplyException(
          "The configuration changed since the dry run: "
              + analysis.blockers().get(0).message()
              + "; check the import again");
    }
    List<DatasetResult> results =
        ApplyEngine.apply(
            jdbc,
            reader,
            analysis.diffs(),
            options.deactivate(),
            new ApplyActors(maker, checker, clock.instant()));
    List<Reconciliation> reconciliation =
        Reconciler.reconcile(reader, pkg, analyzer.datasets(pkg, options));
    record(importId, results, reconciliation);
    imp.applied(snapshot.getId(), clock.instant());
    repository.save(imp);
    long mismatched = reconciliation.stream().filter(r -> !r.matched()).count();
    audit.record(
        ENTITY,
        imp.getImportNo(),
        AuditAction.POST,
        "Applied package "
            + store.get(imp.getPackageId()).getSha256()
            + " (SHA-256), prepared by "
            + maker
            + ", approved by "
            + checker
            + (imp.getChangeReference() == null ? "" : ", change " + imp.getChangeReference())
            + "; snapshot "
            + snapshot.getPackageNo()
            + "; "
            + (reconciliation.size() - mismatched)
            + " of "
            + reconciliation.size()
            + " dataset(s) reconciled");
  }

  private void record(
      Long importId, List<DatasetResult> results, List<Reconciliation> reconciliation) {
    Map<String, ImportDataset> byCode =
        lines.findByImportIdOrderBySeq(importId).stream()
            .collect(Collectors.toMap(ImportDataset::getDatasetCode, Function.identity()));
    for (DatasetResult r : results) {
      ImportDataset line = byCode.get(r.code());
      if (line != null) {
        line.applied(r.inserted(), r.updated(), r.deactivated(), r.removed());
      }
    }
    for (Reconciliation r : reconciliation) {
      ImportDataset line = byCode.get(r.code());
      if (line != null) {
        line.reconciled(
            r.packageRows(), r.targetRows(), r.targetTotal(), r.packageSha256(), r.targetSha256());
      }
    }
    lines.saveAll(byCode.values());
  }

  private PromotionImport failed(Long importId, String message) {
    PromotionImport imp =
        tx.execute(
            s -> {
              PromotionImport current = imports.get(importId);
              current.failed(message);
              repository.save(current);
              audit.record(
                  ENTITY, current.getImportNo(), AuditAction.REJECT, "Apply failed: " + message);
              return current;
            });
    notifier.decided(
        imp,
        "CONFIG_IMPORT_FAILED",
        "Configuration import " + imp.getImportNo() + " failed",
        message);
    return imp;
  }
}
