package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDatasetRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportStatus;
import com.iortatechnxt.brokerverse.configpromo.domain.PackageKind;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport.ImportRequestFacts;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImportRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The preparation of an import: upload of a package (signature verified, kept in the file store),
 * compatibility check and dry run, new dry runs with other options, submission for approval,
 * rejection and withdrawal, and the rollback of an applied import (an import of its snapshot). The
 * approval and the apply are in {@link ImportApplier}.
 */
@Service
public class ImportService {

  private static final String ENTITY = "ConfigImport";

  private final PackageStore store;
  private final DryRunner dryRunner;
  private final DryRunRecorder recorder;
  private final PromotionImportRepository imports;
  private final ImportDatasetRepository lines;
  private final DocumentNumberService numbers;
  private final PromotionSettings settings;
  private final PromotionNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param store package store
   * @param dryRunner dry run
   * @param recorder keeps dry run results
   * @param imports imports
   * @param lines dataset lines
   * @param numbers document numbers
   * @param settings settings
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param txManager transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ImportService(
      PackageStore store,
      DryRunner dryRunner,
      DryRunRecorder recorder,
      PromotionImportRepository imports,
      ImportDatasetRepository lines,
      DocumentNumberService numbers,
      PromotionSettings settings,
      PromotionNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.store = store;
    this.dryRunner = dryRunner;
    this.recorder = recorder;
    this.imports = imports;
    this.lines = lines;
    this.numbers = numbers;
    this.settings = settings;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * An uploaded package with the choices of its import.
   *
   * @param fileName file name
   * @param content zip bytes
   * @param options datasets, deactivations, users
   * @param changeReference change request reference (mandatory in production)
   * @param reason reason
   * @param pipeline whether a deployment pipeline sends it
   */
  public record UploadCommand(
      String fileName,
      byte[] content,
      ImportOptions options,
      String changeReference,
      String reason,
      boolean pipeline) {}

  /**
   * Uploads a package, checks it and runs the dry run.
   *
   * @param command package and choices
   * @return the import, status CHECKED
   */
  public PromotionImport upload(UploadCommand command) {
    ConfigPackage pkg = store.verify(command.content());
    PromotionImport imp =
        required(
            tx.execute(
                s -> {
                  PromotionPackage kept =
                      store.keep(
                          PackageKind.UPLOAD, pkg.manifest(), command.content(), pkg.keyId());
                  PromotionImport created =
                      imports.save(
                          new PromotionImport(
                              numbers.next("CFI-" + BusinessClock.today(clock).getYear()),
                              kept.getId(),
                              new ImportRequestFacts(
                                  settings.production(),
                                  command.pipeline(),
                                  blankToNull(command.changeReference()),
                                  blankToNull(command.reason()),
                                  currentUser.username(),
                                  clock.instant(),
                                  null)));
                  audit.record(
                      ENTITY,
                      created.getImportNo(),
                      AuditAction.CREATE,
                      "Uploaded package "
                          + kept.getPackageNo()
                          + " from "
                          + pkg.manifest().sourceEnvironment()
                          + ", SHA-256 "
                          + kept.getSha256());
                  return created;
                }));
    return check(imp.getId(), pkg, command.options());
  }

  /**
   * Runs the dry run again with other choices (or after the target changed).
   *
   * @param importId import
   * @param options choices
   * @return the import, status CHECKED
   */
  public PromotionImport recheck(Long importId, ImportOptions options) {
    PromotionImport imp = get(importId);
    requirePreparer(imp);
    ConfigPackage pkg = store.open(store.get(imp.getPackageId()));
    return check(importId, pkg, options);
  }

  private PromotionImport check(Long importId, ConfigPackage pkg, ImportOptions options) {
    DryRunner.Outcome outcome = dryRunner.run(pkg, options, currentUser.username());
    return tx.execute(s -> recorder.record(get(importId), outcome, options));
  }

  /**
   * Submits an import for approval.
   *
   * @param importId import
   * @return the import
   */
  @Transactional
  public PromotionImport submit(Long importId) {
    PromotionImport imp = get(importId);
    requirePreparer(imp);
    if (imp.isProduction() && (imp.getChangeReference() == null || imp.getReason() == null)) {
      throw new BusinessRuleException(
          "CONFIG_CHANGE_REFERENCE_REQUIRED",
          "A production import needs the change request number and the reason");
    }
    imp.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, imp.getImportNo(), AuditAction.SUBMIT, "Submitted for approval");
    notifier.submitted(imp);
    return imp;
  }

  /**
   * Rejects an import (the approver).
   *
   * @param importId import
   * @param note reason
   * @return the import
   */
  @Transactional
  public PromotionImport reject(Long importId, String note) {
    PromotionImport imp = get(importId);
    imp.reject(currentUser.username(), clock.instant(), note);
    audit.record(ENTITY, imp.getImportNo(), AuditAction.REJECT, "Rejected: " + note);
    notifier.decided(
        imp,
        "CONFIG_IMPORT_REJECTED",
        "Configuration import " + imp.getImportNo() + " rejected",
        "Reason: " + note);
    return imp;
  }

  /**
   * Withdraws an import (the preparer).
   *
   * @param importId import
   * @return the import
   */
  @Transactional
  public PromotionImport cancel(Long importId) {
    PromotionImport imp = get(importId);
    requirePreparer(imp);
    imp.cancel();
    audit.record(ENTITY, imp.getImportNo(), AuditAction.UPDATE, "Withdrawn");
    return imp;
  }

  /**
   * Prepares the rollback of an applied import: an import of the snapshot taken before it, which
   * deactivates the items it added and puts back the values it changed; approved like any import.
   *
   * @param importId applied import
   * @return the rollback import, status CHECKED
   */
  public PromotionImport rollback(Long importId) {
    PromotionImport original = get(importId);
    if (original.getStatus() != ImportStatus.APPLIED || original.getSnapshotPackageId() == null) {
      throw new BusinessRuleException(
          "CONFIG_ROLLBACK_NOT_POSSIBLE",
          "Only an applied import with a snapshot can be rolled back");
    }
    ImportOptions previous = recorder.options(original);
    Set<String> added = new LinkedHashSet<>();
    lines.findByImportIdOrderBySeq(importId).stream()
        .filter(l -> l.getAdded() > 0)
        .map(ImportDataset::getDatasetCode)
        .forEach(added::add);
    List<String> codes =
        lines.findByImportIdOrderBySeq(importId).stream()
            .map(ImportDataset::getDatasetCode)
            .toList();
    ImportOptions options = new ImportOptions(Set.copyOf(codes), added, previous.includeUsers());
    PromotionPackage snapshot = store.get(original.getSnapshotPackageId());
    ConfigPackage pkg = store.open(snapshot);
    PromotionImport imp =
        required(
            tx.execute(
                s -> {
                  PromotionImport created =
                      imports.save(
                          new PromotionImport(
                              numbers.next("CFI-" + BusinessClock.today(clock).getYear()),
                              snapshot.getId(),
                              new ImportRequestFacts(
                                  settings.production(),
                                  false,
                                  original.getChangeReference(),
                                  "Rollback of import " + original.getImportNo(),
                                  currentUser.username(),
                                  clock.instant(),
                                  original.getId())));
                  audit.record(
                      ENTITY,
                      created.getImportNo(),
                      AuditAction.CREATE,
                      "Rollback of "
                          + original.getImportNo()
                          + " from snapshot "
                          + snapshot.getPackageNo());
                  return created;
                }));
    return check(imp.getId(), pkg, options);
  }

  /**
   * An import.
   *
   * @param importId id
   * @return import
   */
  @Transactional(readOnly = true)
  public PromotionImport get(Long importId) {
    return imports
        .findById(importId)
        .orElseThrow(() -> new ResourceNotFoundException("Configuration import", importId));
  }

  /**
   * Imports, newest first.
   *
   * @param pageable page
   * @return imports
   */
  @Transactional(readOnly = true)
  public Page<PromotionImport> list(Pageable pageable) {
    return imports.findAllByOrderByIdDesc(pageable);
  }

  /**
   * The dataset lines of an import.
   *
   * @param importId import
   * @return lines in load order
   */
  @Transactional(readOnly = true)
  public List<ImportDataset> datasets(Long importId) {
    return lines.findByImportIdOrderBySeq(importId);
  }

  /**
   * The line of one dataset of an import.
   *
   * @param importId import
   * @param code dataset
   * @return line
   */
  @Transactional(readOnly = true)
  public ImportDataset dataset(Long importId, String code) {
    return lines
        .findByImportIdAndDatasetCode(importId, code)
        .orElseThrow(() -> new ResourceNotFoundException("Dataset of the import", code));
  }

  private void requirePreparer(PromotionImport imp) {
    if (!imp.getPreparedBy().equalsIgnoreCase(currentUser.username())) {
      throw new BusinessRuleException(
          "CONFIG_IMPORT_NOT_YOURS", "Only the user who prepared the import may change it");
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  /**
   * The options of an import as text (for the API).
   *
   * @param imp import
   * @return options
   */
  public ImportOptions options(PromotionImport imp) {
    return recorder.options(imp);
  }

  /** The result of a transaction callback that always returns one. */
  private static <T> T required(T result) {
    if (result == null) {
      throw new IllegalStateException("The transaction returned no result");
    }
    return result;
  }
}
