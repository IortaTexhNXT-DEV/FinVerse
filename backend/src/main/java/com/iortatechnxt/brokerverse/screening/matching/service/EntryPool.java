package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ACTIVE watchlist entries screened by a batch run, indexed in memory by their blocking keys
 * (SNSRP-301; design 12): each client is scored only against the entries sharing a phonetic or
 * exact key with one of its names.
 */
public final class EntryPool {

  private final List<ListedEntry> entries;
  private final Map<String, List<ListedEntry>> byKey = new HashMap<>();

  private EntryPool(List<ListedEntry> entries) {
    this.entries = List.copyOf(entries);
    for (ListedEntry e : this.entries) {
      List<NameKeys> names = new ArrayList<>(PairMatcher.names(e));
      e.aliases().stream().map(NameKeys::of).filter(k -> !k.isEmpty()).forEach(names::add);
      for (NameKeys n : names) {
        n.phonetic().forEach(code -> index("P:" + code, e));
        index("E:" + n.exact(), e);
      }
    }
  }

  /**
   * Indexes the ACTIVE entries among some entries.
   *
   * @param entries entries
   * @return the pool
   */
  public static EntryPool of(Collection<ListedEntry> entries) {
    return new EntryPool(entries.stream().filter(e -> e.status() == EntryStatus.ACTIVE).toList());
  }

  private void index(String key, ListedEntry entry) {
    List<ListedEntry> list = byKey.computeIfAbsent(key, k -> new ArrayList<>());
    if (list.isEmpty() || !list.get(list.size() - 1).id().equals(entry.id())) {
      list.add(entry);
    }
  }

  /**
   * The entries a client is scored against.
   *
   * @param subject the client
   * @return candidate entries, each once
   */
  public Collection<ListedEntry> candidates(ScreeningSubject subject) {
    Map<Long, ListedEntry> found = new LinkedHashMap<>();
    for (NameKeys n : subject.keys()) {
      n.phonetic().forEach(code -> add(found, "P:" + code));
      add(found, "E:" + n.exact());
    }
    return found.values();
  }

  private void add(Map<Long, ListedEntry> found, String key) {
    byKey.getOrDefault(key, List.of()).forEach(e -> found.putIfAbsent(e.id(), e));
  }

  /**
   * The number of entries in the pool.
   *
   * @return size
   */
  public int size() {
    return entries.size();
  }

  /**
   * Whether the pool has no entry.
   *
   * @return true when empty
   */
  public boolean isEmpty() {
    return entries.isEmpty();
  }
}
