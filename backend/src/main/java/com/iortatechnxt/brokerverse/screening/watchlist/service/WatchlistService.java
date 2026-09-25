package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChangeRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntryRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSourceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual maintenance of watchlist entries under maker-checker (SNSRP-203, 204; FR-SS-022, 023):
 * add, change and deactivate as PENDING changes with before / after values; approve (the entry is
 * ACTIVE or INACTIVE from today and screened again) or reject with remarks. The maker never
 * decides.
 */
@Service
@Transactional
public class WatchlistService {

  /** Audit entity type of entries. */
  public static final String ENTITY = "WatchlistEntry";

  /** Notification event of a change waiting for approval. */
  static final String EVENT_TO_APPROVE = "SCR_LIST_CHANGE_TO_APPROVE";

  /** Source of manual entries (FR-SS-022 R2). */
  static final String INTERNAL = "INTERNAL";

  private final WatchlistSourceRepository sources;
  private final WatchlistEntryRepository entries;
  private final WatchlistChangeRepository changes;
  private final WatchlistStore store;
  private final DocumentNumberService numbers;
  private final EntryValidator validator;
  private final AuditTrailService audit;
  private final NotificationService notifications;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sources sources
   * @param entries entries
   * @param changes changes
   * @param store shared operations
   * @param numbers reference numbers of manual entries
   * @param validator entry checks
   * @param audit audit trail
   * @param notifications notifications
   * @param clock clock
   */
  public WatchlistService(
      WatchlistSourceRepository sources,
      WatchlistEntryRepository entries,
      WatchlistChangeRepository changes,
      WatchlistStore store,
      DocumentNumberService numbers,
      EntryValidator validator,
      AuditTrailService audit,
      NotificationService notifications,
      Clock clock) {
    this.sources = sources;
    this.entries = entries;
    this.changes = changes;
    this.store = store;
    this.numbers = numbers;
    this.validator = validator;
    this.audit = audit;
    this.notifications = notifications;
    this.clock = clock;
  }

  // ---------------------------------------------------------------- queries

  /**
   * The sources by code.
   *
   * @return sources
   */
  @Transactional(readOnly = true)
  public List<WatchlistSource> sources() {
    return sources.findAllByOrderByCodeAsc();
  }

  /**
   * A source by code.
   *
   * @param code code
   * @return source
   */
  @Transactional(readOnly = true)
  public WatchlistSource source(String code) {
    return sources
        .findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
        .orElseThrow(() -> new ResourceNotFoundException("Watchlist source", code));
  }

  /**
   * Source codes by id (list screens).
   *
   * @return codes
   */
  @Transactional(readOnly = true)
  public Map<Long, String> sourceCodes() {
    return sources.findAll().stream()
        .collect(Collectors.toMap(WatchlistSource::getId, WatchlistSource::getCode));
  }

  /**
   * An entry.
   *
   * @param id id
   * @return entry
   */
  @Transactional(readOnly = true)
  public WatchlistEntry entry(Long id) {
    return store.entry(id);
  }

  /**
   * An entry's values with its aliases.
   *
   * @param entry entry
   * @return values
   */
  @Transactional(readOnly = true)
  public EntryValues values(WatchlistEntry entry) {
    return store.values(entry);
  }

  /**
   * Stored values of a change.
   *
   * @param json JSON, may be {@code null}
   * @return values, {@code null} for {@code null}
   */
  public EntryValues readValues(String json) {
    return store.read(json);
  }

  /**
   * The number of changes of a run still waiting for approval.
   *
   * @param runId run
   * @return count
   */
  @Transactional(readOnly = true)
  public int pendingOfRun(Long runId) {
    return changes.findByRunIdAndStatusOrderByIdAsc(runId, ChangeStatus.PENDING).size();
  }

  /**
   * Searches entries.
   *
   * @param sourceCode source, blank for all
   * @param status status, {@code null} for all
   * @param search name or reference, blank for all
   * @param pageable page
   * @return entries
   */
  @Transactional(readOnly = true)
  public Page<WatchlistEntry> search(
      String sourceCode, EntryStatus status, String search, Pageable pageable) {
    Long sourceId = sourceCode == null || sourceCode.isBlank() ? null : source(sourceCode).getId();
    String like =
        search == null || search.isBlank()
            ? "%"
            : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    return entries.search(sourceId, status, like, pageable);
  }

  /**
   * An entry's history, newest first.
   *
   * @param entryId entry
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<WatchlistChange> history(Long entryId) {
    return changes.findByEntryIdOrderByIdDesc(entryId);
  }

  /**
   * Changes in a status (PENDING for the checker's list).
   *
   * @param status status
   * @param pageable page
   * @return changes
   */
  @Transactional(readOnly = true)
  public Page<WatchlistChange> changes(ChangeStatus status, Pageable pageable) {
    return changes.findByStatusOrderByIdAsc(status, pageable);
  }

  /**
   * A change.
   *
   * @param id id
   * @return change
   */
  @Transactional(readOnly = true)
  public WatchlistChange change(Long id) {
    return changes
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Watchlist change", id));
  }

  /**
   * Changes the maintainable settings of a source.
   *
   * @param code source
   * @param settings new settings
   * @return the source
   */
  public WatchlistSource updateSource(String code, SourceSettings settings) {
    WatchlistSource source = source(code);
    if (settings.name() == null || settings.name().isBlank()) {
      throw new BusinessRuleException("SCR_SOURCE_NAME_REQUIRED", "Enter the source name");
    }
    String layout = blank(settings.fileLayout()) ? null : settings.fileLayout().trim();
    if (layout != null && !List.of("CSV", "XLSX").contains(layout)) {
      throw new BusinessRuleException(
          "SCR_SOURCE_LAYOUT", "Select the file layout of the source (CSV or XLSX)");
    }
    source.update(
        settings.name().trim(),
        trim(settings.schedule()),
        layout,
        settings.fullFile(),
        settings.active());
    audit.record(
        "WatchlistSource",
        source.getCode(),
        AuditAction.UPDATE,
        "Source "
            + source.getCode()
            + ": schedule "
            + settings.schedule()
            + ", layout "
            + settings.fileLayout()
            + ", full file "
            + settings.fullFile()
            + ", active "
            + settings.active());
    return source;
  }

  // ---------------------------------------------------------------- maker

  /**
   * Adds an entry (FR-SS-022): a PENDING entry with a PENDING addition, not screened until
   * approved.
   *
   * @param sourceCode source, blank = INTERNAL
   * @param values values
   * @param remarks reason for the change
   * @return the change
   */
  public WatchlistChange add(String sourceCode, EntryValues values, String remarks) {
    WatchlistSource source = source(blank(sourceCode) ? INTERNAL : sourceCode);
    EntryValues clean = validator.validate(values, remarks);
    WatchlistEntry entry =
        entries.save(
            new WatchlistEntry(
                source.getId(), numbers.next(source.getRefPrefix()), clean, remarks.trim()));
    WatchlistChange change =
        store.save(store.stage(entry, null, ChangeType.ADD, null, clean, remarks.trim()));
    submitted(entry, change);
    return change;
  }

  /**
   * Changes an entry (FR-SS-022): a PENDING update with the before and after values. A draft entry
   * (rejected addition) is submitted again as an addition.
   *
   * @param entryId entry
   * @param values new values
   * @param remarks reason for the change
   * @return the change
   */
  public WatchlistChange change(Long entryId, EntryValues values, String remarks) {
    WatchlistEntry entry = store.entry(entryId);
    store.requireNoPending(entry);
    EntryValues clean = validator.validate(values, remarks);
    boolean draft = entry.getStatus() == EntryStatus.DRAFT;
    ChangeType type = draft ? ChangeType.ADD : ChangeType.UPDATE;
    WatchlistChange change =
        store.save(
            store.stage(
                entry, null, type, draft ? null : store.values(entry), clean, remarks.trim()));
    entry.resubmit();
    submitted(entry, change);
    return change;
  }

  /**
   * Deactivates an entry (FR-SS-022 R3: entries are never deleted): a PENDING deactivation.
   *
   * @param entryId entry
   * @param remarks reason
   * @return the change
   */
  public WatchlistChange deactivate(Long entryId, String remarks) {
    WatchlistEntry entry = store.entry(entryId);
    EntryValidator.requireRemarks(remarks);
    if (entry.getStatus() != EntryStatus.ACTIVE) {
      throw new BusinessRuleException(
          "SCR_ENTRY_NOT_ACTIVE", "Entry " + entry.getExternalRef() + " is not active");
    }
    store.requireNoPending(entry);
    EntryValues before = store.values(entry);
    EntryValues after = withDelisting(before, today());
    WatchlistChange change =
        store.save(store.stage(entry, null, ChangeType.DEACTIVATE, before, after, remarks.trim()));
    submitted(entry, change);
    return change;
  }

  /**
   * Submits one list record of a bulk upload (handler SCR_WATCHLIST): a new reference is added, a
   * known one changed; both wait for a checker.
   *
   * @param sourceCode source, blank = INTERNAL
   * @param rec the record
   * @param remarks reason for the change
   * @return the change
   */
  public WatchlistChange submit(String sourceCode, ListRecord rec, String remarks) {
    WatchlistSource source = source(blank(sourceCode) ? INTERNAL : sourceCode);
    Optional<WatchlistEntry> existing =
        entries.findBySourceIdAndExternalRef(source.getId(), rec.reference());
    if (existing.isPresent()) {
      return change(existing.get().getId(), rec.values(), remarks);
    }
    EntryValues clean = validator.validate(rec.values(), remarks);
    WatchlistEntry entry =
        entries.save(new WatchlistEntry(source.getId(), rec.reference(), clean, remarks.trim()));
    WatchlistChange change =
        store.save(store.stage(entry, null, ChangeType.ADD, null, clean, remarks.trim()));
    submitted(entry, change);
    return change;
  }

  static EntryValues withDelisting(EntryValues v, LocalDate delistedOn) {
    return new EntryValues(
        v.listType(),
        v.entityType(),
        v.primaryName(),
        v.firstName(),
        v.lastName(),
        v.birthDate(),
        v.nationality(),
        v.idNumbers(),
        v.listedOn(),
        delistedOn,
        v.aliases());
  }

  private void submitted(WatchlistEntry entry, WatchlistChange change) {
    audit.record(
        ENTITY,
        entry.getExternalRef(),
        AuditAction.SUBMIT,
        change.getChangeType() + " submitted for approval: " + change.getMakerRemarks());
    notifications.notifyPermission(
        ScreeningPermissions.LIST_APPROVE,
        new Notice(
            "Watchlist change to approve",
            change.getChangeType() + " of " + entry.getExternalRef() + " " + entry.getPrimaryName(),
            "/screening-setup/watchlist?change=" + change.getId(),
            ENTITY,
            entry.getExternalRef()),
        EVENT_TO_APPROVE);
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String trim(String value) {
    return blank(value) ? null : value.trim();
  }

  /**
   * Maintainable settings of a source.
   *
   * @param name name
   * @param schedule schedule text
   * @param fileLayout CSV or XLSX
   * @param fullFile whether a file replaces the whole list
   * @param active whether the source is read
   */
  public record SourceSettings(
      String name, String schedule, String fileLayout, boolean fullFile, boolean active) {}
}
