package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionError;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionErrorRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRunRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.RunCounts;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.RunStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntryRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistFeed.FeedFile;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingestion of list files (SNSRP-201, 202; FR-SS-020, 021). Each file is one run: records are
 * validated against the SCR_WATCHLIST template, new references are added, changed ones updated,
 * identical ones left alone, and for a full-file source the active entries missing from the file
 * are delisted. A scheduled run of the official feed applies its changes at once; a file uploaded
 * by a Compliance Officer stages them as PENDING changes for a checker (SNSRP-203, 204). Failed
 * records are kept with their line and reason; a FAILED or PARTIAL run raises {@code
 * SCR_INGEST_FAILED}.
 */
@Service
@Transactional
public class WatchlistIngestionService {

  /** Audit and alert entity type of runs. */
  public static final String RUN_ENTITY = "WatchlistIngestionRun";

  /** Exception code of a failed or partial run (V1050). */
  static final String ALERT = "SCR_INGEST_FAILED";

  private final WatchlistService watchlists;
  private final WatchlistEntryRepository entries;
  private final IngestionRunRepository runs;
  private final IngestionErrorRepository errors;
  private final WatchlistStore store;
  private final BulkFileReader reader;
  private final AttachmentService attachments;
  private final DocumentNumberService numbers;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param watchlists sources and approvals
   * @param entries entries
   * @param runs runs
   * @param errors failed records
   * @param store shared operations
   * @param reader CSV / XLSX reader of the bulk platform
   * @param attachments stored list files
   * @param numbers run numbers
   * @param alerts alerts
   * @param notifications notifications
   * @param audit audit trail
   * @param events event publisher
   * @param clock clock
   */
  public WatchlistIngestionService(
      WatchlistService watchlists,
      WatchlistEntryRepository entries,
      IngestionRunRepository runs,
      IngestionErrorRepository errors,
      WatchlistStore store,
      BulkFileReader reader,
      AttachmentService attachments,
      DocumentNumberService numbers,
      AlertService alerts,
      NotificationService notifications,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      Clock clock) {
    this.watchlists = watchlists;
    this.entries = entries;
    this.runs = runs;
    this.errors = errors;
    this.store = store;
    this.reader = reader;
    this.attachments = attachments;
    this.numbers = numbers;
    this.alerts = alerts;
    this.notifications = notifications;
    this.audit = audit;
    this.events = events;
    this.clock = clock;
  }

  // ---------------------------------------------------------------- queries

  /**
   * Runs, newest first.
   *
   * @param sourceCode source, blank for all
   * @param pageable page
   * @return runs
   */
  @Transactional(readOnly = true)
  public Page<IngestionRun> runs(String sourceCode, Pageable pageable) {
    Long sourceId =
        sourceCode == null || sourceCode.isBlank() ? null : watchlists.source(sourceCode).getId();
    return runs.search(sourceId, pageable);
  }

  /**
   * A run.
   *
   * @param id id
   * @return run
   */
  @Transactional(readOnly = true)
  public IngestionRun run(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ingestion run", id));
  }

  /**
   * The failed records of a run.
   *
   * @param runId run
   * @return records by line
   */
  @Transactional(readOnly = true)
  public List<IngestionError> errors(Long runId) {
    return errors.findByRunIdOrderByLineNoAsc(runId);
  }

  // ---------------------------------------------------------------- intake

  /**
   * Uploads a list file (FR-SS-020 "Upload List File"): the run is logged at once with trigger
   * MANUAL_UPLOAD; its additions, updates and delistings wait for a checker.
   *
   * @param sourceCode source
   * @param fileName file name
   * @param content bytes
   * @return the run
   */
  public IngestionRun upload(String sourceCode, String fileName, byte[] content) {
    WatchlistSource source = acceptedSource(sourceCode, fileName);
    Attachment stored = store(source, fileName, content, "List file uploaded");
    return ingest(
        source,
        IngestionTrigger.MANUAL_UPLOAD,
        new FeedFile(stored.getId(), fileName, content),
        true);
  }

  /**
   * Stages a file for the next scheduled run of the source (the default file-drop transport).
   *
   * @param sourceCode source
   * @param fileName file name
   * @param content bytes
   * @return the stored file
   */
  public Attachment stage(String sourceCode, String fileName, byte[] content) {
    return store(acceptedSource(sourceCode, fileName), fileName, content, "List file staged");
  }

  private WatchlistSource acceptedSource(String sourceCode, String fileName) {
    WatchlistSource source = watchlists.source(sourceCode);
    if (!source.accepts(fileName)) {
      throw new BusinessRuleException(
          "SCR_LIST_FILE_TYPE", "The file type is not allowed for source " + source.getCode());
    }
    return source;
  }

  private Attachment store(
      WatchlistSource source, String fileName, byte[] content, String description) {
    return attachments.upload(
        new AttachmentTarget(StagedFileWatchlistFeed.SOURCE_ENTITY, String.valueOf(source.getId())),
        fileName,
        content,
        description + " for " + source.getCode());
  }

  /**
   * Records a FAILED scheduled run of a source whose file did not arrive (FR-SS-020).
   *
   * @param source source
   * @return the run
   */
  public IngestionRun missingFile(WatchlistSource source) {
    IngestionRun run = start(source, IngestionTrigger.SCHEDULED, null, false);
    return failed(run, source, "No list file was received for source " + source.getCode());
  }

  /**
   * Reads one file into the list and logs the run.
   *
   * @param source source
   * @param trigger SCHEDULED or MANUAL_UPLOAD
   * @param file the file
   * @param stageForApproval true to stage the changes for a checker, false to apply them
   * @return the run
   */
  public IngestionRun ingest(
      WatchlistSource source, IngestionTrigger trigger, FeedFile file, boolean stageForApproval) {
    IngestionRun run = start(source, trigger, file.fileName(), stageForApproval);
    run.link(file.attachmentId(), null);
    ParsedFile parsed;
    try {
      parsed = reader.read(file.fileName(), file.content());
    } catch (BusinessRuleException ex) {
      return failed(run, source, ex.getMessage());
    }
    List<String> missing =
        ListRecord.requiredHeaders().stream().filter(h -> !parsed.headers().contains(h)).toList();
    if (!missing.isEmpty()) {
      return failed(
          run,
          source,
          "The file does not follow the list layout; missing column(s): "
              + String.join(", ", missing));
    }
    Pass pass = new Pass(run, source, stageForApproval);
    parsed.rows().forEach(row -> record(pass, row));
    if (source.isFullFile()) {
      delistMissing(pass);
    }
    return finish(pass, parsed.rows().size());
  }

  private IngestionRun start(
      WatchlistSource source, IngestionTrigger trigger, String fileName, boolean staged) {
    String runNo = numbers.next("WLR-" + LocalDate.now(clock).getYear());
    return runs.save(
        new IngestionRun(runNo, source.getId(), trigger, fileName, staged, clock.instant()));
  }

  private void record(Pass pass, ParsedFile.RawRow row) {
    List<String> messages = new ArrayList<>();
    ListRecord rec =
        ListRecord.read(row.rowNo(), row.values(), pass.source.getListType(), messages);
    String reference = row.values().get(ListRecord.REFERENCE);
    if (reference != null) {
      pass.seen.add(reference.trim());
    }
    if (rec != null) {
      Integer first = pass.lines.putIfAbsent(rec.reference(), rec.line());
      if (first != null) {
        messages.add(
            "Line " + rec.line() + ": reference " + rec.reference() + " repeats line " + first);
      }
    }
    if (messages.isEmpty()) {
      try {
        apply(pass, rec);
        return;
      } catch (BusinessRuleException ex) {
        messages.add("Line " + row.rowNo() + ": " + ex.getMessage());
      }
    }
    pass.failed++;
    errors.save(
        new IngestionError(
            pass.run.getId(),
            row.rowNo(),
            ListRecord.raw(row.values()),
            String.join("; ", messages)));
  }

  private void apply(Pass pass, ListRecord rec) {
    Optional<WatchlistEntry> existing =
        entries.findBySourceIdAndExternalRef(pass.source.getId(), rec.reference());
    if (existing.isEmpty()) {
      WatchlistEntry entry =
          entries.save(
              new WatchlistEntry(
                  pass.source.getId(), rec.reference(), rec.values(), remarks(pass, rec)));
      change(pass, entry, ChangeType.ADD, null, rec.values(), remarks(pass, rec));
      pass.added++;
      return;
    }
    WatchlistEntry entry = existing.get();
    Optional<WatchlistChange> pending = store.pending(entry.getId());
    EntryValues target =
        pending.map(c -> store.read(c.getAfterValues())).orElseGet(() -> store.values(entry));
    boolean inForce = pending.isPresent() || entry.getStatus() == EntryStatus.ACTIVE;
    if (inForce && target.equals(rec.values())) {
      pass.unchanged++;
      return;
    }
    if (pending.isPresent()) {
      // thrown and caught inside this bean: no transactional proxy marks the run for rollback
      throw new BusinessRuleException(
          "SCR_ENTRY_CHANGE_PENDING",
          "Entry " + entry.getExternalRef() + " already has a change waiting for approval");
    }
    boolean draft = entry.getStatus() == EntryStatus.DRAFT;
    change(
        pass,
        entry,
        draft ? ChangeType.ADD : ChangeType.UPDATE,
        draft ? null : store.values(entry),
        rec.values(),
        remarks(pass, rec));
    entry.resubmit();
    pass.updated++;
  }

  private static String remarks(Pass pass, ListRecord rec) {
    String base = "Run " + pass.run.getRunNo() + " line " + rec.line();
    return rec.remarks() == null ? base : base + ": " + rec.remarks();
  }

  private void delistMissing(Pass pass) {
    for (WatchlistEntry entry :
        entries.findBySourceIdAndStatus(pass.source.getId(), EntryStatus.ACTIVE)) {
      if (pass.seen.contains(entry.getExternalRef()) || store.pending(entry.getId()).isPresent()) {
        continue;
      }
      EntryValues before = store.values(entry);
      change(
          pass,
          entry,
          ChangeType.DEACTIVATE,
          before,
          WatchlistService.withDelisting(before, LocalDate.now(clock)),
          "Run " + pass.run.getRunNo() + ": not in the list file any more");
      pass.delisted++;
    }
  }

  private void change(
      Pass pass,
      WatchlistEntry entry,
      ChangeType type,
      EntryValues before,
      EntryValues after,
      String remarks) {
    WatchlistChange change = store.stage(entry, pass.run.getId(), type, before, after, remarks);
    if (pass.staged) {
      store.save(change);
      pass.pendingChanges++;
      return;
    }
    change.appliedByFeed(clock.instant(), "Official feed, run " + pass.run.getRunNo());
    store.save(change);
    store.apply(change, entry, LocalDate.now(clock));
    pass.applied.add(entry.getId());
  }

  private IngestionRun finish(Pass pass, int received) {
    IngestionRun run = pass.run;
    run.complete(
        new RunCounts(
            received, pass.added, pass.updated, pass.delisted, pass.unchanged, pass.failed),
        clock.instant());
    audit.record(
        RUN_ENTITY,
        run.getRunNo(),
        AuditAction.RUN,
        pass.source.getCode()
            + " "
            + run.getTrigger()
            + ": received "
            + received
            + ", added "
            + pass.added
            + ", updated "
            + pass.updated
            + ", delisted "
            + pass.delisted
            + ", failed "
            + pass.failed
            + (pass.staged ? " (changes wait for approval)" : ""));
    if (run.getStatus() == RunStatus.PARTIAL) {
      raise(run, pass.source, pass.failed + " record(s) failed");
    }
    if (!pass.applied.isEmpty()) {
      events.publishEvent(new WatchlistEntriesChanged(pass.applied, "RUN:" + run.getRunNo()));
    }
    if (pass.pendingChanges > 0) {
      notifications.notifyPermission(
          ScreeningPermissions.LIST_APPROVE,
          new Notice(
              "Watchlist file to approve",
              "Run "
                  + run.getRunNo()
                  + " of "
                  + pass.source.getCode()
                  + ": "
                  + pass.pendingChanges
                  + " change(s) wait for approval",
              "/screening-setup/sources?run=" + run.getId(),
              RUN_ENTITY,
              run.getRunNo()),
          WatchlistService.EVENT_TO_APPROVE);
    }
    return run;
  }

  private IngestionRun failed(IngestionRun run, WatchlistSource source, String reason) {
    run.fail(reason, clock.instant());
    audit.record(
        RUN_ENTITY, run.getRunNo(), AuditAction.RUN, source.getCode() + " FAILED: " + reason);
    raise(run, source, reason);
    return run;
  }

  private void raise(IngestionRun run, WatchlistSource source, String reason) {
    String message =
        "Watchlist run "
            + run.getRunNo()
            + " of "
            + source.getCode()
            + " "
            + run.getStatus()
            + ": "
            + reason;
    alerts.raise(
        ALERT,
        new AlertFacts(
            null, null, RUN_ENTITY, run.getRunNo(), message, null, ALERT + ":" + run.getRunNo()));
    notifications.notifyPermission(
        ScreeningPermissions.LIST_MAINTAIN,
        new Notice(
            "Watchlist ingestion " + run.getStatus(),
            message,
            "/screening-setup/sources?run=" + run.getId(),
            RUN_ENTITY,
            run.getRunNo()));
  }

  /** Working state of one run. */
  private static final class Pass {
    private final IngestionRun run;
    private final WatchlistSource source;
    private final boolean staged;
    private final Set<String> seen = new HashSet<>();
    private final Map<String, Integer> lines = new HashMap<>();
    private final List<Long> applied = new ArrayList<>();
    private int added;
    private int updated;
    private int delisted;
    private int unchanged;
    private int failed;
    private int pendingChanges;

    private Pass(IngestionRun run, WatchlistSource source, boolean staged) {
      this.run = run;
      this.source = source;
      this.staged = staged;
    }
  }
}
