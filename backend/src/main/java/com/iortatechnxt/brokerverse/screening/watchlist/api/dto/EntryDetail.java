package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import java.util.List;

/**
 * A watchlist entry with its aliases and change history.
 *
 * @param entry the entry
 * @param aliases its aliases
 * @param history its changes, newest first (the pending one included)
 */
public record EntryDetail(
    EntryRow entry, List<EntryValues.Alias> aliases, List<ChangeDto> history) {

  /** Defensive copies. */
  public EntryDetail {
    aliases = List.copyOf(aliases);
    history = List.copyOf(history);
  }
}
