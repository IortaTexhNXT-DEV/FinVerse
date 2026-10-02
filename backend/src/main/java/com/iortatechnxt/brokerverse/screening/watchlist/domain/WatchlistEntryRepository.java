package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Watchlist entries (SNSRP-201, 203). */
public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {

  /**
   * An entry by source and reference.
   *
   * @param sourceId source
   * @param externalRef reference
   * @return entry
   */
  Optional<WatchlistEntry> findBySourceIdAndExternalRef(Long sourceId, String externalRef);

  /**
   * The entries of a source in a status (delisting of a full file).
   *
   * @param sourceId source
   * @param status status
   * @return entries
   */
  List<WatchlistEntry> findBySourceIdAndStatus(Long sourceId, EntryStatus status);

  /**
   * Entries in a status, optionally of some list types (the screening reads ACTIVE ones).
   *
   * @param status status
   * @param listTypes list types
   * @return entries
   */
  List<WatchlistEntry> findByStatusAndListTypeInOrderByIdAsc(
      EntryStatus status, Collection<String> listTypes);

  /**
   * Entries in a status.
   *
   * @param status status
   * @return entries
   */
  List<WatchlistEntry> findByStatusOrderByIdAsc(EntryStatus status);

  /**
   * Searches entries by source, status and name or reference.
   *
   * @param sourceId source, {@code null} for all
   * @param status status, {@code null} for all
   * @param like lower-case pattern, {@code %} for all
   * @param pageable page
   * @return entries
   */
  @Query(
      "select e from WatchlistEntry e where (:sourceId is null or e.sourceId = :sourceId)"
          + " and (:status is null or e.status = :status)"
          + " and (lower(e.primaryName) like :like or lower(e.externalRef) like :like)")
  Page<WatchlistEntry> search(
      @Param("sourceId") Long sourceId,
      @Param("status") EntryStatus status,
      @Param("like") String like,
      Pageable pageable);
}
