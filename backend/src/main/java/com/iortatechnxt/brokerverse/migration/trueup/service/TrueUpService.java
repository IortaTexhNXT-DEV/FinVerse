package com.iortatechnxt.brokerverse.migration.trueup.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.service.LoadListener;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueupRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The FY2027 true-ups of the year-end cut-over (DATA_MIGRATION_DESIGN 17.7, option A): prepared by
 * the Comptrollership GL lead on a validated G03 batch, approved by the Head of Comptrollership
 * (never the preparer), posted by the load of the batch, reconciled (movement against the legacy
 * trial balances, migration clearing at zero) and signed. The year-end option is a parameter:
 * true-ups exist only for option A (and C, where the legacy books close FY2027 alongside).
 */
@Service
@Transactional
public class TrueUpService implements LoadListener {

  private static final Set<String> TRUEUP_NUMBERS = Set.of("1", "2", "3", "F");
  private static final String OBJECT = "G03";

  private final MigTrueupRepository trueups;
  private final MigBatchRepository batches;
  private final ReconciliationService recon;
  private final MigrationParameters parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final JdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param trueups true-ups
   * @param batches batches
   * @param recon reconciliation
   * @param parameters migration parameters
   * @param alerts alerts
   * @param audit audit trail
   * @param jdbc JDBC (counts of the posting)
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators
  public TrueUpService(
      MigTrueupRepository trueups,
      MigBatchRepository batches,
      ReconciliationService recon,
      MigrationParameters parameters,
      AlertService alerts,
      AuditTrailService audit,
      JdbcTemplate jdbc,
      CurrentUser currentUser,
      Clock clock) {
    this.trueups = trueups;
    this.batches = batches;
    this.recon = recon;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The true-ups of a company.
   *
   * @param companyId company
   * @return true-ups, oldest first
   */
  @Transactional(readOnly = true)
  public List<MigTrueup> list(Long companyId) {
    return trueups.findByCompanyIdOrderByIdAsc(companyId);
  }

  /**
   * One true-up.
   *
   * @param reference MIG-TU-n
   * @return true-up
   */
  @Transactional(readOnly = true)
  public MigTrueup get(String reference) {
    return trueups
        .findByReference(reference)
        .orElseThrow(() -> new ResourceNotFoundException("Opening-balance adjustment", reference));
  }

  /**
   * Prepares a true-up on a G03 batch (and the legacy trial balance of the same version).
   *
   * @param companyId company
   * @param request number, as-of date and batches
   * @return the true-up
   */
  public MigTrueup prepare(Long companyId, Prepare request) {
    if ("B".equals(parameters.yearEndOption())) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_OPTION",
          "Opening-balance adjustments apply to the year-end go-live; the cut-over option is B");
    }
    String no =
        request.trueupNo() == null ? "" : request.trueupNo().strip().toUpperCase(Locale.ROOT);
    if (!TRUEUP_NUMBERS.contains(no)) {
      throw new BusinessRuleException("MIG_TRUEUP_NO", "The adjustment number is 1, 2, 3 or F");
    }
    if (trueups.findByCompanyIdAndTrueupNo(companyId, no).isPresent()) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_EXISTS", "Opening-balance adjustment " + no + " already exists");
    }
    MigBatch batch = batch(request.batchNo(), OBJECT);
    Long tb = request.tbBatchNo() == null ? null : batch(request.tbBatchNo(), "G01").getId();
    MigTrueup t =
        trueups.save(
            new MigTrueup(
                companyId,
                no,
                new MigTrueup.Inputs(batch.getId(), tb),
                request.asOf(),
                currentUser.username(),
                clock.instant()));
    audit.record(
        MigrationCodes.ENTITY_TRUEUP,
        t.getReference(),
        AuditAction.CREATE,
        "Prepared on batch " + batch.getBatchNo());
    return t;
  }

  private MigBatch batch(String batchNo, String object) {
    MigBatch b =
        batches
            .findByBatchNo(batchNo)
            .orElseThrow(() -> new ResourceNotFoundException("Migration batch", batchNo));
    if (!object.equals(b.getObjectCode())) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_BATCH", "Batch " + batchNo + " is not a batch of object " + object);
    }
    return b;
  }

  /**
   * Submits a prepared true-up for approval.
   *
   * @param reference MIG-TU-n
   * @param note remarks
   * @return the true-up
   */
  public MigTrueup submit(String reference, String note) {
    MigTrueup t = get(reference);
    t.submit(currentUser.username(), note);
    audit.record(MigrationCodes.ENTITY_TRUEUP, reference, AuditAction.SUBMIT, note);
    return t;
  }

  /**
   * Approves or returns a true-up (Head of Comptrollership).
   *
   * @param reference MIG-TU-n
   * @param approve approve or return
   * @param note remarks (reason of a return)
   * @return the true-up
   */
  public MigTrueup decide(String reference, boolean approve, String note) {
    MigTrueup t = get(reference);
    t.decide(approve, currentUser.username(), note, clock.instant());
    audit.record(
        MigrationCodes.ENTITY_TRUEUP,
        reference,
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        note);
    return t;
  }

  /**
   * Records the posting when the G03 batch of an approved true-up is loaded.
   *
   * @param batch loaded batch
   */
  @Override
  public void loaded(MigBatch batch) {
    if (!OBJECT.equals(batch.getObjectCode())) {
      return;
    }
    trueups
        .findByBatchId(batch.getId())
        .filter(t -> t.getStatus() == MigTrueup.Status.APPROVED)
        .ifPresent(
            t -> {
              Integer journals =
                  jdbc.queryForObject(
                      "select count(distinct target_id) from mig_key_xref where batch_id = ?"
                          + " and target_entity = 'JournalBatch' and rolled_back_at is null",
                      Integer.class,
                      batch.getId());
              Integer items =
                  jdbc.queryForObject(
                      "select count(*) from mig_stage_row r join mig_batch_extract e"
                          + " on e.extract_id = r.extract_id where e.batch_id = ?"
                          + " and r.layout_code = 'G03D' and r.status = 'LOADED'",
                      Integer.class,
                      batch.getId());
              t.posted(
                  journals == null ? 0 : journals,
                  items == null ? 0 : items,
                  null,
                  clock.instant());
            });
  }

  /**
   * Reconciles a posted true-up (movement, clearing and sub-ledger checks of its batch).
   *
   * @param reference MIG-TU-n
   * @return the true-up
   */
  public MigTrueup reconcile(String reference) {
    MigTrueup t = get(reference);
    MigBatch batch =
        batches
            .findById(t.getBatchId())
            .orElseThrow(() -> new ResourceNotFoundException("Migration batch", t.getBatchId()));
    MigReconRun run = recon.reconcile(batch.getBatchNo());
    boolean clean = run.getBreakCount() == 0;
    t.reconciled(run.getId(), clean);
    if (!clean) {
      alerts.raise(
          MigrationCodes.ALERT_TRUEUP_BREAK,
          new AlertFacts(
              t.getCompanyId(),
              null,
              MigrationCodes.ENTITY_TRUEUP,
              reference,
              "Opening-balance adjustment "
                  + reference
                  + " has "
                  + run.getBreakCount()
                  + " reconciliation break(s)",
              BigDecimal.valueOf(run.getBreakCount()),
              MigrationCodes.ALERT_TRUEUP_BREAK + ":" + reference));
    }
    return t;
  }

  /**
   * Signs a reconciled true-up (Head of Comptrollership, never the preparer).
   *
   * @param reference MIG-TU-n
   * @return the true-up
   */
  public MigTrueup sign(String reference) {
    MigTrueup t = get(reference);
    t.sign(currentUser.username(), clock.instant());
    audit.record(MigrationCodes.ENTITY_TRUEUP, reference, AuditAction.AUTHORIZE, "Signed");
    return t;
  }

  /**
   * A true-up to prepare.
   *
   * @param trueupNo 1, 2, 3 or F
   * @param asOf as-of date of the legacy trial balance
   * @param batchNo G03 batch
   * @param tbBatchNo G01 batch of the legacy trial balance of the same version, may be null
   */
  public record Prepare(String trueupNo, LocalDate asOf, String batchNo, String tbBatchNo) {}
}
