package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Watchlist changes (SNSRP-203, 204). */
public interface WatchlistChangeRepository extends JpaRepository<WatchlistChange, Long> {

  /**
   * The change of an entry in a status (at most one PENDING per entry).
   *
   * @param entryId entry
   * @param status status
   * @return change
   */
  Optional<WatchlistChange> findFirstByEntryIdAndStatus(Long entryId, ChangeStatus status);

  /**
   * The history of an entry, newest first.
   *
   * @param entryId entry
   * @return changes
   */
  List<WatchlistChange> findByEntryIdOrderByIdDesc(Long entryId);

  /**
   * Changes in a status, oldest first.
   *
   * @param status status
   * @param pageable page
   * @return changes
   */
  Page<WatchlistChange> findByStatusOrderByIdAsc(ChangeStatus status, Pageable pageable);

  /**
   * Changes in a status (approval inbox).
   *
   * @param status status
   * @return changes
   */
  List<WatchlistChange> findByStatusOrderByIdAsc(ChangeStatus status);

  /**
   * The changes of a run in a status.
   *
   * @param runId run
   * @param status status
   * @return changes
   */
  List<WatchlistChange> findByRunIdAndStatusOrderByIdAsc(Long runId, ChangeStatus status);
}
