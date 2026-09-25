package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Aliases of watchlist entries. */
public interface WatchlistAliasRepository extends JpaRepository<WatchlistAlias, Long> {

  /**
   * The aliases of an entry.
   *
   * @param entryId entry
   * @return aliases
   */
  List<WatchlistAlias> findByEntryIdOrderByIdAsc(Long entryId);

  /**
   * The aliases of several entries.
   *
   * @param entryIds entries
   * @return aliases
   */
  List<WatchlistAlias> findByEntryIdInOrderByIdAsc(Collection<Long> entryIds);

  /**
   * Deletes the aliases of an entry (replaced as a whole when a change applies).
   *
   * @param entryId entry
   * @return rows deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = false)
  @Query("delete from WatchlistAlias a where a.entryId = :entryId")
  int deleteByEntryId(@Param("entryId") Long entryId);
}
