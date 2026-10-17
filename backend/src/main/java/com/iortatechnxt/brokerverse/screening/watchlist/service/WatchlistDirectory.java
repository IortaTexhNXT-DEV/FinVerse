package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistAlias;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistAliasRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntryRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSourceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read port of the watchlists for the matching engine (SNSRP-203 "screening reads ACTIVE rows
 * only", SNSRP-301). Returns immutable {@link ListedEntry} records with their aliases.
 */
@Service
@Transactional(readOnly = true)
public class WatchlistDirectory {

  private final WatchlistEntryRepository entries;
  private final WatchlistAliasRepository aliases;
  private final WatchlistSourceRepository sources;

  /**
   * Creates the directory.
   *
   * @param entries entries
   * @param aliases aliases
   * @param sources sources
   */
  public WatchlistDirectory(
      WatchlistEntryRepository entries,
      WatchlistAliasRepository aliases,
      WatchlistSourceRepository sources) {
    this.entries = entries;
    this.aliases = aliases;
    this.sources = sources;
  }

  /**
   * The ACTIVE entries, optionally of some list types.
   *
   * @param listTypes list types, empty for all
   * @return entries
   */
  public List<ListedEntry> active(Collection<String> listTypes) {
    List<WatchlistEntry> rows =
        listTypes == null || listTypes.isEmpty()
            ? entries.findByStatusOrderByIdAsc(EntryStatus.ACTIVE)
            : entries.findByStatusAndListTypeInOrderByIdAsc(EntryStatus.ACTIVE, listTypes);
    return listed(rows);
  }

  /**
   * Entries by id, whatever their status (the delta screening of changed entries decides).
   *
   * @param ids entry ids
   * @return entries
   */
  public List<ListedEntry> entries(Collection<Long> ids) {
    return listed(entries.findAllById(ids));
  }

  /**
   * One entry.
   *
   * @param id entry id
   * @return the entry, empty when unknown
   */
  public Optional<ListedEntry> entry(Long id) {
    return entries(List.of(id)).stream().findFirst();
  }

  private List<ListedEntry> listed(List<WatchlistEntry> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, List<String>> names =
        aliases
            .findByEntryIdInOrderByIdAsc(rows.stream().map(WatchlistEntry::getId).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    WatchlistAlias::getEntryId,
                    Collectors.mapping(WatchlistAlias::getAliasName, Collectors.toList())));
    Map<Long, String> codes =
        sources.findAll().stream()
            .collect(Collectors.toMap(WatchlistSource::getId, WatchlistSource::getCode));
    return rows.stream()
        .map(
            e ->
                new ListedEntry(
                    e.getId(),
                    codes.get(e.getSourceId()),
                    e.getExternalRef(),
                    e.getListType(),
                    e.getEntityType(),
                    e.getPrimaryName(),
                    e.getFirstName(),
                    e.getLastName(),
                    e.getBirthDate(),
                    e.getNationality(),
                    e.getIdNumbers(),
                    names.getOrDefault(e.getId(), List.of()),
                    e.getEntryVersion(),
                    e.getStatus()))
        .toList();
  }
}
