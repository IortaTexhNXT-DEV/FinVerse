package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges staging data (DATA_MIGRATION_DESIGN sections 6 and 18.3): the payloads of the rows of a
 * batch signed off or rolled back more than {@code MIG_STAGING_RETENTION_DAYS} ago, and of extracts
 * rejected that long ago, with their files. Counts, hashes and control totals stay on the extract
 * as reconciliation evidence.
 */
@Service
@Transactional
public class StagingPurgeService {

  private final MigBatchRepository batches;
  private final MigExtractRepository extracts;
  private final StoredFileService files;
  private final BatchLogger log;
  private final MigrationParameters parameters;
  private final AuditTrailService audit;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param extracts extracts
   * @param files file store
   * @param log run log
   * @param parameters retention
   * @param audit audit trail
   * @param jdbc JDBC
   * @param clock clock
   */
  public StagingPurgeService(
      MigBatchRepository batches,
      MigExtractRepository extracts,
      StoredFileService files,
      BatchLogger log,
      MigrationParameters parameters,
      AuditTrailService audit,
      JdbcTemplate jdbc,
      Clock clock) {
    this.batches = batches;
    this.extracts = extracts;
    this.files = files;
    this.log = log;
    this.parameters = parameters;
    this.audit = audit;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Purges what is due.
   *
   * @param today business date
   * @return batches and extracts purged
   */
  public int purge(LocalDate today) {
    int purged = 0;
    for (MigBatch b : due(today)) {
      jdbc.update(
          "update mig_stage_row set raw_payload = null, mapped_payload = null where batch_id = ?",
          b.getId());
      for (MigExtract e : extracts.findAllById(b.getExtractIds())) {
        purgeExtract(e);
      }
      b.purged(clock.instant());
      log.info(b, "PURGE", "Staging data and extract files purged");
      purged++;
    }
    LocalDate limit = today.minusDays(parameters.retentionDays());
    for (MigExtract e : extracts.findByStatus(ExtractStatus.REJECTED)) {
      if (e.getCheckedAt() != null
          && !e.getCheckedAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(limit)) {
        purgeExtract(e);
        purged++;
      }
    }
    return purged;
  }

  /**
   * Batches whose staging data is due for purge.
   *
   * @param today business date
   * @return batches
   */
  @Transactional(readOnly = true)
  public List<MigBatch> due(LocalDate today) {
    return batches
        .findByStatusInOrderByIdAsc(EnumSet.of(BatchStatus.SIGNED_OFF, BatchStatus.ROLLED_BACK))
        .stream()
        .filter(
            b ->
                b.getPurgedAt() == null
                    && b.getPurgeDueOn() != null
                    && !b.getPurgeDueOn().isAfter(today))
        .toList();
  }

  private void purgeExtract(MigExtract e) {
    if (e.getStatus() == ExtractStatus.PURGED) {
      return;
    }
    jdbc.update(
        "update mig_stage_row set raw_payload = null, mapped_payload = null where extract_id = ?",
        e.getId());
    deleteFile(e.getStoredFileId());
    deleteFile(e.getControlFileId());
    e.purged(clock.instant());
    audit.record(MigrationCodes.ENTITY_EXTRACT, e.getExtractNo(), AuditAction.DEACTIVATE, "Purged");
  }

  private void deleteFile(Long id) {
    if (id != null) {
      try {
        files.delete(id);
      } catch (RuntimeException e) {
        // already deleted by the 5-day lifecycle of the migration bucket
      }
    }
  }
}
