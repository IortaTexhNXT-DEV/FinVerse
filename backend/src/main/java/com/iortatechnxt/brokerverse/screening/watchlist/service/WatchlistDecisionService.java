package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChangeRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checker decisions on watchlist changes (SNSRP-204; FR-SS-023): approve (the entry becomes ACTIVE
 * or INACTIVE from today and is screened again through {@link WatchlistEntriesChanged}), reject
 * with remarks, and approve every change of an uploaded file. The maker never decides.
 */
@Service
@Transactional
public class WatchlistDecisionService {

  private final WatchlistService watchlists;
  private final WatchlistChangeRepository changes;
  private final WatchlistStore store;
  private final AuditTrailService audit;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param watchlists watchlist maintenance (changes)
   * @param changes changes
   * @param store shared operations
   * @param audit audit trail
   * @param notifications notifications
   * @param events event publisher
   * @param currentUser current user
   * @param clock clock
   */
  public WatchlistDecisionService(
      WatchlistService watchlists,
      WatchlistChangeRepository changes,
      WatchlistStore store,
      AuditTrailService audit,
      NotificationService notifications,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.watchlists = watchlists;
    this.changes = changes;
    this.store = store;
    this.audit = audit;
    this.notifications = notifications;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Approves a change (FR-SS-023): the entry becomes ACTIVE (or INACTIVE for a deactivation) from
   * today and is screened again.
   *
   * @param changeId change
   * @param remarks optional remarks
   * @return the change
   */
  public WatchlistChange approve(Long changeId, String remarks) {
    WatchlistChange change = watchlists.change(changeId);
    WatchlistEntry entry = store.entry(change.getEntryId());
    change.approve(currentUser.username(), clock.instant(), trim(remarks));
    store.apply(change, entry, today());
    audit.record(
        WatchlistService.ENTITY,
        entry.getExternalRef(),
        AuditAction.AUTHORIZE,
        change.getChangeType() + " approved; entry " + entry.getStatus());
    notifyMaker(change, entry, "approved");
    events.publishEvent(
        new WatchlistEntriesChanged(List.of(entry.getId()), "CHANGE:" + change.getId()));
    return change;
  }

  /**
   * Rejects a change with remarks (FR-SS-023): the entry stays as it was; a rejected addition goes
   * back to DRAFT.
   *
   * @param changeId change
   * @param remarks mandatory remarks
   * @return the change
   */
  public WatchlistChange reject(Long changeId, String remarks) {
    if (blank(remarks)) {
      throw new BusinessRuleException(
          "SCR_LIST_REJECT_REMARKS", "Enter the remarks for the rejection");
    }
    WatchlistChange change = watchlists.change(changeId);
    WatchlistEntry entry = store.entry(change.getEntryId());
    change.reject(currentUser.username(), clock.instant(), remarks.trim());
    if (change.getChangeType() == ChangeType.ADD) {
      entry.backToDraft();
    }
    audit.record(
        WatchlistService.ENTITY,
        entry.getExternalRef(),
        AuditAction.REJECT,
        change.getChangeType() + " rejected: " + remarks.trim());
    notifyMaker(change, entry, "rejected: " + remarks.trim());
    return change;
  }

  /**
   * Approves every pending change of an uploaded file at once.
   *
   * @param runId ingestion run
   * @return number of changes approved
   */
  public int approveRun(Long runId) {
    List<WatchlistChange> pending =
        changes.findByRunIdAndStatusOrderByIdAsc(runId, ChangeStatus.PENDING);
    List<Long> applied = new ArrayList<>();
    String checker = currentUser.username();
    for (WatchlistChange change : pending) {
      WatchlistEntry entry = store.entry(change.getEntryId());
      change.approve(checker, clock.instant(), null);
      store.apply(change, entry, today());
      applied.add(entry.getId());
    }
    if (!applied.isEmpty()) {
      audit.record(
          "WatchlistIngestionRun",
          runId,
          AuditAction.AUTHORIZE,
          applied.size() + " list change(s) of the run approved");
      events.publishEvent(new WatchlistEntriesChanged(applied, "RUN:" + runId));
    }
    return applied.size();
  }

  private void notifyMaker(WatchlistChange change, WatchlistEntry entry, String outcome) {
    notifications.notifyUser(
        change.getCreatedBy(),
        new Notice(
            "Watchlist change decided",
            change.getChangeType() + " of " + entry.getExternalRef() + " " + outcome,
            "/screening-setup/watchlist?change=" + change.getId(),
            WatchlistService.ENTITY,
            entry.getExternalRef()));
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
}
