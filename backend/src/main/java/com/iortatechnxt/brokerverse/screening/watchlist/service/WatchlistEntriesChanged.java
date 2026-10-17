package com.iortatechnxt.brokerverse.screening.watchlist.service;

import java.util.List;

/**
 * Watchlist entries became ACTIVE, changed or were delisted (SNSRP-204 "approval ... triggers a
 * delta screening of the entry"; SNSRP-201 scheduled runs). Published inside the transaction that
 * applied the changes; the matching wave rebuilds the entries' name keys and runs the delta
 * screening with {@code @TransactionalEventListener(AFTER_COMMIT)}.
 *
 * @param entryIds the entries whose screening data changed
 * @param cause what applied them, e.g. "CHANGE:12" or "RUN:WLR-2026-000003"
 */
public record WatchlistEntriesChanged(List<Long> entryIds, String cause) {

  /** Defensive copy. */
  public WatchlistEntriesChanged {
    entryIds = List.copyOf(entryIds);
  }
}
