package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchMode;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plans batches (DATA_MIGRATION_DESIGN section 11; FR-DM-014, FR-DM-015): a batch takes one staged
 * extract of each layout in force of its object with the same as-of date; a RERUN batch takes the
 * rejected rows of its parent batch (and the rows of a corrected extract, for a resubmission).
 */
@Service
@Transactional
public class BatchPlanService {

  private final ObjectRegisterService register;
  private final LayoutService layouts;
  private final MigExtractRepository extracts;
  private final StageRowRepository rows;
  private final MigBatchRepository batches;
  private final BatchCounter counter;
  private final BatchLogger log;
  private final DocumentNumberService numbers;
  private final MigrationParameters parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register data objects
   * @param layouts layouts
   * @param extracts extracts
   * @param rows staged rows
   * @param batches batches
   * @param counter counts
   * @param log run log
   * @param numbers document numbers
   * @param parameters parameters
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BatchPlanService(
      ObjectRegisterService register,
      LayoutService layouts,
      MigExtractRepository extracts,
      StageRowRepository rows,
      MigBatchRepository batches,
      BatchCounter counter,
      BatchLogger log,
      DocumentNumberService numbers,
      MigrationParameters parameters,
      AuditTrailService audit,
      Clock clock) {
    this.register = register;
    this.layouts = layouts;
    this.extracts = extracts;
    this.rows = rows;
    this.batches = batches;
    this.counter = counter;
    this.log = log;
    this.numbers = numbers;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Plans a batch of an object from staged extracts.
   *
   * @param companyId company
   * @param objectCode object
   * @param extractNos extracts to use; empty takes the latest staged extract of each layout
   * @return the planned batch
   */
  public MigBatch plan(Long companyId, String objectCode, List<String> extractNos) {
    MigDataObject object = register.get(objectCode);
    if (!object.loadable() && !object.archive()) {
      throw new BusinessRuleException(
          "MIG_OBJECT_NOT_LOADABLE",
          "Object " + objectCode + " is not decided as Migrate, Carry-forward or Archive");
    }
    List<MigExtract> chosen = choose(companyId, object, extractNos);
    Set<LocalDate> days =
        chosen.stream().map(e -> e.getAsOf().toLocalDate()).collect(Collectors.toSet());
    if (days.size() > 1) {
      throw new BusinessRuleException(
          "MIG_ASOF_MISMATCH", "The extracts of a batch must have the same as-of date: " + days);
    }
    String mode = chosen.stream().anyMatch(e -> "DELTA".equals(e.getMode())) ? "DELTA" : "FULL";
    MigBatch batch =
        batches.save(
            new MigBatch(
                companyId,
                numbers.next("MGB-" + BusinessClock.today(clock).getYear()),
                objectCode,
                BatchMode.valueOf(mode),
                null,
                parameters.environmentClass()));
    for (MigExtract e : chosen) {
      batch.addExtract(e.getId());
      for (StageRow r : rows.findByExtractIdInOrderByIdAsc(List.of(e.getId()))) {
        r.validated(RowStatus.STAGED, null, batch.getId());
      }
    }
    counter.recount(batch);
    log.info(
        batch,
        "PLAN",
        "Planned from "
            + chosen.stream().map(MigExtract::getExtractNo).collect(Collectors.joining(", ")));
    audit.record(
        MigrationCodes.ENTITY_BATCH,
        batch.getBatchNo(),
        AuditAction.CREATE,
        "Planned " + objectCode);
    return batch;
  }

  private List<MigExtract> choose(Long companyId, MigDataObject object, List<String> extractNos) {
    Set<Long> used = usedExtracts(companyId, object.getCode());
    List<MigExtract> chosen = new ArrayList<>();
    if (extractNos != null && !extractNos.isEmpty()) {
      for (String no : extractNos) {
        MigExtract e =
            extracts
                .findByExtractNo(no)
                .orElseThrow(
                    () -> new ResourceNotFoundException(MigrationCodes.ENTITY_EXTRACT, no));
        requireUsable(e, object, used);
        chosen.add(e);
      }
    } else {
      for (Layout layout : layouts.inForce(object.getCode())) {
        extracts
            .findByCompanyIdAndObjectCodeAndStatusOrderByIdAsc(
                companyId, object.getCode(), ExtractStatus.STAGED)
            .stream()
            .filter(e -> e.getLayoutCode().equals(layout.getCode()) && !used.contains(e.getId()))
            .reduce((a, b) -> b)
            .ifPresent(chosen::add);
      }
    }
    if (chosen.isEmpty()) {
      throw new BusinessRuleException(
          "MIG_NO_EXTRACT", "Object " + object.getCode() + " has no staged extract to load");
    }
    return chosen;
  }

  private static void requireUsable(MigExtract e, MigDataObject object, Set<Long> used) {
    if (!e.getObjectCode().equals(object.getCode()) || e.getStatus() != ExtractStatus.STAGED) {
      throw new BusinessRuleException(
          "MIG_EXTRACT_NOT_STAGED",
          "Extract " + e.getExtractNo() + " is not a staged extract of object " + object.getCode());
    }
    if (used.contains(e.getId())) {
      throw new BusinessRuleException(
          "MIG_EXTRACT_USED", "Extract " + e.getExtractNo() + " is already in a batch");
    }
  }

  private Set<Long> usedExtracts(Long companyId, String objectCode) {
    Set<Long> used = new HashSet<>();
    for (MigBatch b : batches.findByCompanyIdAndObjectCodeOrderByIdDesc(companyId, objectCode)) {
      if (b.getStatus() != BatchStatus.ROLLED_BACK && b.getMode() != BatchMode.RERUN) {
        used.addAll(b.getExtractIds());
      }
    }
    return used;
  }

  /**
   * Creates a RERUN batch with the rejected rows of a batch and, for a resubmission, the rows of a
   * corrected extract.
   *
   * @param batchNo parent batch
   * @param correctedExtractId corrected extract, null for a rerun of the rejected rows
   * @return the rerun batch
   */
  public MigBatch rerun(String batchNo, Long correctedExtractId) {
    MigBatch parent = get(batchNo);
    parent.requireStatus(
        "rerun", BatchStatus.LOADED_WITH_REJECTS, BatchStatus.RECONCILED, BatchStatus.LOADED);
    List<StageRow> rejected =
        rows.findByBatchIdAndStatusInOrderByIdAsc(parent.getId(), EnumSet.of(RowStatus.REJECTED));
    List<StageRow> corrected =
        correctedExtractId == null
            ? List.of()
            : rows.findByExtractIdInOrderByIdAsc(List.of(correctedExtractId));
    if (rejected.isEmpty() && corrected.isEmpty()) {
      throw new BusinessRuleException(
          "MIG_NOTHING_TO_RERUN", "Batch " + batchNo + " has no rejected rows to load again");
    }
    MigBatch child =
        batches.save(
            new MigBatch(
                parent.getCompanyId(),
                numbers.next("MGB-" + BusinessClock.today(clock).getYear()),
                parent.getObjectCode(),
                BatchMode.RERUN,
                parent.getId(),
                parameters.environmentClass()));
    parent.getExtractIds().forEach(child::addExtract);
    if (correctedExtractId != null) {
      child.addExtract(correctedExtractId);
    }
    Set<String> correctedKeys =
        corrected.stream().map(StageRow::getLegacyKey).collect(Collectors.toSet());
    for (StageRow r : rejected) {
      if (!correctedKeys.contains(r.getLegacyKey())) {
        r.validated(RowStatus.STAGED, null, child.getId());
      }
    }
    corrected.forEach(r -> r.validated(RowStatus.STAGED, null, child.getId()));
    counter.recount(child);
    counter.recount(parent);
    log.info(child, "RERUN", "Rerun of " + batchNo + " with " + child.counts().staged() + " rows");
    log.info(parent, "RERUN", "Rejected rows moved to rerun batch " + child.getBatchNo());
    return child;
  }

  /**
   * A batch.
   *
   * @param batchNo number
   * @return batch
   */
  @Transactional(readOnly = true)
  public MigBatch get(String batchNo) {
    return batches
        .findByBatchNo(batchNo)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_BATCH, batchNo));
  }

  /**
   * A batch by id.
   *
   * @param id id
   * @return batch
   */
  @Transactional(readOnly = true)
  public MigBatch byId(Long id) {
    return batches
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_BATCH, id));
  }

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  @Transactional(readOnly = true)
  public Page<MigBatch> search(Long companyId, Pageable pageable) {
    return batches.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * The rows of a batch by layout, in file order.
   *
   * @param batch batch
   * @param statuses statuses
   * @return rows by layout
   */
  @Transactional(readOnly = true)
  public Map<String, List<StageRow>> rowsByLayout(MigBatch batch, Set<RowStatus> statuses) {
    return rows.findByBatchIdAndStatusInOrderByIdAsc(batch.getId(), statuses).stream()
        .collect(
            Collectors.groupingBy(
                StageRow::getLayoutCode, LinkedHashMap::new, Collectors.toList()));
  }
}
