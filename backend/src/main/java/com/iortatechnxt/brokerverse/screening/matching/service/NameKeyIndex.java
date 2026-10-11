package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.screening.matching.domain.NameKey;
import com.iortatechnxt.brokerverse.screening.matching.domain.NameKeyRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.NameKeyType;
import com.iortatechnxt.brokerverse.screening.matching.domain.NameSubjectKind;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDirectory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The stored blocking keys of clients and watchlist entries ({@code scr_name_key}, SNSRP-301;
 * design 4.2 and 12 "matching uses blocking keys before scoring"). Client keys are rebuilt when a
 * client is registered or its identity changes and when the batch screens a client without keys;
 * entry keys when {@code WatchlistEntriesChanged} reports a change, and for ACTIVE entries loaded
 * without keys (for example by a migration) before each run. Only this class writes the table.
 */
@Service
@Transactional
public class NameKeyIndex {

  private static final Set<NameSubjectKind> CLIENT = Set.of(NameSubjectKind.CLIENT);
  private static final Set<NameSubjectKind> ENTRY =
      Set.of(NameSubjectKind.ENTRY, NameSubjectKind.ALIAS);

  private final NameKeyRepository keys;
  private final WatchlistDirectory directory;

  /**
   * Creates the index.
   *
   * @param keys key rows
   * @param directory watchlist entries
   */
  public NameKeyIndex(NameKeyRepository keys, WatchlistDirectory directory) {
    this.keys = keys;
    this.directory = directory;
  }

  /**
   * Replaces the keys of a client.
   *
   * @param subject the client
   */
  public void rebuildClient(ScreeningSubject subject) {
    keys.deleteBySubjects(CLIENT, List.of(subject.clientId()));
    keys.flush();
    keys.saveAll(rows(NameSubjectKind.CLIENT, subject.clientId(), subject.keys()));
  }

  /**
   * Builds the keys of the clients that have none.
   *
   * @param subjects clients
   * @return the number of clients keyed
   */
  public int ensureClients(Collection<ScreeningSubject> subjects) {
    if (subjects.isEmpty()) {
      return 0;
    }
    Set<Long> keyed =
        Set.copyOf(
            keys.keyed(
                NameSubjectKind.CLIENT,
                subjects.stream().map(ScreeningSubject::clientId).toList()));
    List<NameKey> rows = new ArrayList<>();
    int count = 0;
    for (ScreeningSubject s : subjects) {
      if (!keyed.contains(s.clientId())) {
        rows.addAll(rows(NameSubjectKind.CLIENT, s.clientId(), s.keys()));
        count++;
      }
    }
    keys.saveAll(rows);
    return count;
  }

  /**
   * Replaces the keys of watchlist entries: removed for entries no longer ACTIVE, rebuilt for the
   * others.
   *
   * @param entries the entries
   */
  public void rebuildEntries(Collection<ListedEntry> entries) {
    if (entries.isEmpty()) {
      return;
    }
    keys.deleteBySubjects(ENTRY, entries.stream().map(ListedEntry::id).toList());
    keys.flush();
    List<NameKey> rows = new ArrayList<>();
    for (ListedEntry e : entries) {
      if (e.status() == EntryStatus.ACTIVE) {
        rows.addAll(rows(NameSubjectKind.ENTRY, e.id(), PairMatcher.names(e)));
        rows.addAll(
            rows(
                NameSubjectKind.ALIAS,
                e.id(),
                e.aliases().stream().map(NameKeys::of).filter(k -> !k.isEmpty()).toList()));
      }
    }
    keys.saveAll(rows);
  }

  /**
   * Builds the keys of the ACTIVE entries that have none.
   *
   * @return the number of entries keyed
   */
  public int ensureEntries() {
    List<Long> missing = keys.activeEntriesWithoutKeys();
    if (!missing.isEmpty()) {
      rebuildEntries(directory.entries(missing));
    }
    return missing.size();
  }

  /**
   * The entries sharing a phonetic or exact key with a client's names.
   *
   * @param subject the client
   * @return entry ids
   */
  @Transactional(readOnly = true)
  public Set<Long> entriesFor(ScreeningSubject subject) {
    return candidates(ENTRY, subject.keys());
  }

  /**
   * The clients sharing a phonetic or exact key with entries' names.
   *
   * @param entries the entries
   * @return client ids
   */
  @Transactional(readOnly = true)
  public Set<Long> clientsFor(Collection<ListedEntry> entries) {
    List<NameKeys> names = new ArrayList<>();
    for (ListedEntry e : entries) {
      names.addAll(PairMatcher.names(e));
      e.aliases().stream().map(NameKeys::of).filter(k -> !k.isEmpty()).forEach(names::add);
    }
    return candidates(CLIENT, names);
  }

  /**
   * The entries whose keys were rebuilt after a time (the entries changed since the last batch).
   *
   * @param since the time
   * @return entry ids
   */
  @Transactional(readOnly = true)
  public List<Long> entriesChangedSince(Instant since) {
    return keys.keyedSince(NameSubjectKind.ENTRY, since);
  }

  private Set<Long> candidates(Set<NameSubjectKind> kinds, List<NameKeys> names) {
    Set<String> phonetic = new LinkedHashSet<>();
    Set<String> exact = new LinkedHashSet<>();
    names.forEach(
        n -> {
          phonetic.addAll(n.phonetic());
          exact.add(n.exactKey());
        });
    Set<Long> ids = new LinkedHashSet<>();
    if (!phonetic.isEmpty()) {
      ids.addAll(keys.subjectsWith(kinds, NameKeyType.PHONETIC, phonetic));
    }
    if (!exact.isEmpty()) {
      ids.addAll(keys.subjectsWith(kinds, NameKeyType.EXACT, exact));
    }
    return ids;
  }

  private static List<NameKey> rows(NameSubjectKind kind, Long id, List<NameKeys> names) {
    Set<String> tokens = new LinkedHashSet<>();
    Set<String> phonetic = new LinkedHashSet<>();
    Set<String> exact = new LinkedHashSet<>();
    for (NameKeys n : names) {
      tokens.addAll(n.tokenKeys());
      phonetic.addAll(n.phonetic());
      exact.add(n.exactKey());
    }
    List<NameKey> rows = new ArrayList<>();
    tokens.forEach(v -> rows.add(new NameKey(kind, id, NameKeyType.TOKEN, v)));
    phonetic.forEach(v -> rows.add(new NameKey(kind, id, NameKeyType.PHONETIC, v)));
    exact.forEach(v -> rows.add(new NameKey(kind, id, NameKeyType.EXACT, v)));
    return rows;
  }
}
