package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue.Finding;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssueRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapLoader;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.matching.service.ClientMatcher;
import com.iortatechnxt.brokerverse.migration.quality.service.RuleEngine;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The validation step of a batch and the resolution of its issues (BRID 1.1b, 3.1; FR-DM-012,
 * FR-DM-013): rows are mapped with the approved code maps and checked by the rule engine; for
 * clients the matching runs; each row becomes VALID, WARNING or INVALID and the batch VALIDATED
 * with its counts and error rate. The data owner waives failing rows (they load) or excludes them
 * with a manual-entry plan (they are neither loaded nor counted).
 */
@Service
@Transactional
public class ValidationService {

  private static final int PAGE = 1000;
  private static final String REFERENCE_LISTS = "R01";

  private static final Set<RowStatus> TO_VALIDATE =
      EnumSet.of(RowStatus.STAGED, RowStatus.VALID, RowStatus.WARNING, RowStatus.INVALID);

  private final BatchPlanService plans;
  private final LayoutService layouts;
  private final MigExtractRepository extracts;
  private final StageRowRepository rows;
  private final MigIssueRepository issues;
  private final CodeMapLoader maps;
  private final RuleEngine engine;
  private final ClientMatcher matcher;
  private final XrefService xrefs;
  private final BatchCounter counter;
  private final BatchLogger log;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans batches
   * @param layouts layouts
   * @param extracts extracts
   * @param rows staged rows
   * @param issues issues
   * @param maps code maps
   * @param engine rule engine
   * @param matcher client matching
   * @param xrefs cross-references
   * @param counter counts
   * @param log run log
   * @param alerts alerts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ValidationService(
      BatchPlanService plans,
      LayoutService layouts,
      MigExtractRepository extracts,
      StageRowRepository rows,
      MigIssueRepository issues,
      CodeMapLoader maps,
      RuleEngine engine,
      ClientMatcher matcher,
      XrefService xrefs,
      BatchCounter counter,
      BatchLogger log,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.plans = plans;
    this.layouts = layouts;
    this.extracts = extracts;
    this.rows = rows;
    this.issues = issues;
    this.maps = maps;
    this.engine = engine;
    this.matcher = matcher;
    this.xrefs = xrefs;
    this.counter = counter;
    this.log = log;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Validates a planned (or re-validates a validated) batch.
   *
   * @param batchNo batch
   * @return the batch, VALIDATED
   */
  public MigBatch validate(String batchNo) {
    MigBatch batch = plans.get(batchNo);
    batch.requireStatus("validated", BatchStatus.PLANNED, BatchStatus.VALIDATED);
    Map<String, List<StageRow>> byLayout = plans.rowsByLayout(batch, TO_VALIDATE);
    Map<String, List<LayoutColumn>> columns = new HashMap<>();
    CodeMaps approved = maps.approved(mapSets(batch, byLayout.keySet(), columns));
    Map<Long, String> sources =
        extracts.findAllById(batch.getExtractIds()).stream()
            .collect(Collectors.toMap(MigExtract::getId, MigExtract::getSourceSystem));
    RuleEngine.Outcome outcome =
        engine.validate(
            batch,
            new RuleEngine.Input(
                byLayout,
                columns,
                approved,
                sources,
                (object, keys) -> xrefs.loaded(batch.getCompanyId(), object, keys),
                BusinessClock.today(clock)));
    List<StageRow> all = byLayout.values().stream().flatMap(List::stream).toList();
    apply(batch, all, outcome);
    int review =
        matcher.match(batch, byLayout.getOrDefault("C01", List.of()), outcome::mapped, sources);
    batch.useMapVersions(approved.versions());
    MigBatch.Counts counts = counter.count(batch.getId());
    batch.validated(counts, BatchCounter.rate(counts), currentUser.username(), clock.instant());
    long unmapped =
        outcome.unmapped().stream()
            .map(u -> u.setCode() + "|" + u.source() + "|" + u.legacyCode())
            .distinct()
            .count();
    batch.validationFindings((int) unmapped, review);
    log.info(
        batch,
        "VALIDATE",
        "Validated with map versions "
            + approved.versions()
            + "; error rate "
            + batch.getErrorRate()
            + " percent; "
            + unmapped
            + " unmapped codes; "
            + review
            + " client pairs to review");
    raiseUnmapped(batch, unmapped);
    return batch;
  }

  /** The columns of the layouts in force and the code map sets they use (all lists for R01). */
  private Set<String> mapSets(
      MigBatch batch, Set<String> layoutCodes, Map<String, List<LayoutColumn>> columns) {
    Set<String> sets = new HashSet<>();
    for (String code : layoutCodes) {
      Layout layout = layouts.current(code).orElseGet(() -> layouts.requireCurrent(code));
      List<LayoutColumn> cols = layouts.columns(layout.getId());
      columns.put(code, cols);
      cols.stream()
          .map(LayoutColumn::getMapSet)
          .filter(s -> s != null && !s.isBlank())
          .forEach(sets::add);
    }
    if (REFERENCE_LISTS.equals(batch.getObjectCode())) {
      sets.addAll(maps.approvedWithPrefix("LOV:").versions().keySet());
      sets.addAll(maps.approvedWithPrefix("MIS:").versions().keySet());
    }
    return sets;
  }

  private void apply(MigBatch batch, List<StageRow> all, RuleEngine.Outcome outcome) {
    List<Long> ids = all.stream().map(StageRow::getId).toList();
    for (int from = 0; from < ids.size(); from += PAGE) {
      issues.deleteOpenOf(
          ids.subList(from, Math.min(ids.size(), from + PAGE)), MigIssue.Resolution.OPEN);
    }
    Map<Long, List<MigIssue>> kept =
        pagedIssues(ids).stream().collect(Collectors.groupingBy(MigIssue::getStageRowId));
    for (StageRow row : all) {
      List<MigIssue> previous = kept.getOrDefault(row.getId(), List.of());
      boolean error = false;
      boolean warning = false;
      for (Finding f : outcome.findings(row.getId())) {
        boolean waived =
            previous.stream()
                .anyMatch(i -> same(i, f) && i.getResolution() == MigIssue.Resolution.WAIVED);
        if (!waived) {
          issues.save(new MigIssue(row.getId(), batch.getId(), f));
        }
        error |= f.severity() == MigIssue.Severity.ERROR && !waived;
        warning |= f.severity() == MigIssue.Severity.WARNING || waived;
      }
      RowStatus status = error ? RowStatus.INVALID : warning ? RowStatus.WARNING : RowStatus.VALID;
      row.validated(status, outcome.mapped(row.getId()), batch.getId());
    }
  }

  private List<MigIssue> pagedIssues(List<Long> ids) {
    List<MigIssue> out = new ArrayList<>();
    for (int from = 0; from < ids.size(); from += PAGE) {
      out.addAll(issues.findByStageRowIdIn(ids.subList(from, Math.min(ids.size(), from + PAGE))));
    }
    return out;
  }

  private static boolean same(MigIssue issue, Finding f) {
    return issue.getRuleCode().equals(f.ruleCode())
        && String.valueOf(issue.getField()).equals(String.valueOf(f.field()));
  }

  private void raiseUnmapped(MigBatch batch, long unmapped) {
    if (unmapped > 0) {
      alerts.raise(
          MigrationCodes.ALERT_UNMAPPED,
          new AlertFacts(
              batch.getCompanyId(),
              null,
              MigrationCodes.ENTITY_BATCH,
              batch.getBatchNo(),
              "Batch " + batch.getBatchNo() + " has " + unmapped + " unmapped legacy codes",
              null,
              MigrationCodes.ALERT_UNMAPPED + ":" + batch.getBatchNo()));
    }
  }

  /**
   * Waives the ERROR issues of rows (the data owner); the rows load and count as waived.
   *
   * @param batchNo batch
   * @param rowIds rows
   * @param reason reason code (list MIG_WAIVER_REASON)
   * @param note note
   * @return the batch
   */
  public MigBatch waive(String batchNo, List<Long> rowIds, String reason, String note) {
    return resolveRows(batchNo, rowIds, MigIssue.Resolution.WAIVED, reason, note);
  }

  /**
   * Excludes rows from the batch with a manual-entry plan (the data owner).
   *
   * @param batchNo batch
   * @param rowIds rows
   * @param reason reason code (list MIG_WAIVER_REASON)
   * @param plan manual-entry plan
   * @return the batch
   */
  public MigBatch exclude(String batchNo, List<Long> rowIds, String reason, String plan) {
    return resolveRows(batchNo, rowIds, MigIssue.Resolution.EXCLUDED, reason, plan);
  }

  private MigBatch resolveRows(
      String batchNo, List<Long> rowIds, MigIssue.Resolution how, String reason, String note) {
    MigBatch batch = plans.get(batchNo);
    batch.requireStatus("changed", BatchStatus.VALIDATED);
    requireReason(how, reason, note);
    String user = currentUser.username();
    for (StageRow row : rows.findAllById(rowIds)) {
      resolveRow(batch, row, new Resolving(how, reason, note, user));
    }
    counter.recount(batch);
    log.info(
        batch,
        "VALIDATE",
        rowIds.size()
            + " rows "
            + how.name().toLowerCase(Locale.ROOT)
            + " by "
            + user
            + ": "
            + note);
    audit.record(
        MigrationCodes.ENTITY_BATCH,
        batch.getBatchNo(),
        AuditAction.AUTHORIZE,
        rowIds.size() + " rows " + how + " (" + reason + "): " + note);
    return batch;
  }

  private static void requireReason(MigIssue.Resolution how, String reason, String note) {
    if (reason == null || reason.isBlank() || note == null || note.isBlank()) {
      throw new BusinessRuleException(
          "MIG_REASON_REQUIRED",
          how == MigIssue.Resolution.EXCLUDED
              ? "Enter the reason and the manual-entry plan of the excluded rows"
              : "Enter the reason and the remarks of the waiver");
    }
  }

  private void resolveRow(MigBatch batch, StageRow row, Resolving r) {
    if (!batch.getId().equals(row.getBatchId())) {
      throw new ResourceNotFoundException("StageRow", row.getId());
    }
    boolean exclude = r.how() == MigIssue.Resolution.EXCLUDED;
    for (MigIssue i : issues.findByStageRowId(row.getId())) {
      if ((exclude || i.isOpenError()) && i.getResolution() == MigIssue.Resolution.OPEN) {
        i.resolve(r.how(), r.reason(), r.note(), r.user(), clock.instant());
      }
    }
    row.mark(exclude ? RowStatus.EXCLUDED : RowStatus.WARNING);
  }

  private record Resolving(MigIssue.Resolution how, String reason, String note, String user) {}

  /**
   * Records the resolution of an issue by the Data Steward (fixed at source or mapped); the row is
   * checked again at the next validation.
   *
   * @param issueId issue
   * @param how FIXED_AT_SOURCE or MAPPED
   * @param note note
   * @return the issue
   */
  public MigIssue resolve(Long issueId, MigIssue.Resolution how, String note) {
    if (how != MigIssue.Resolution.FIXED_AT_SOURCE && how != MigIssue.Resolution.MAPPED) {
      throw new BusinessRuleException(
          "MIG_RESOLUTION", "The Data Steward resolves an issue as fixed at source or mapped");
    }
    MigIssue issue =
        issues
            .findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("MigIssue", issueId));
    issue.resolve(how, null, note, currentUser.username(), clock.instant());
    audit.record("MigIssue", issueId, AuditAction.UPDATE, how + (note == null ? "" : ": " + note));
    return issue;
  }
}
