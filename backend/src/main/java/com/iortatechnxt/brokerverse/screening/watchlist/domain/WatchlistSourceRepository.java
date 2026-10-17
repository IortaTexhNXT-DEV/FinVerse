package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Watchlist sources (SNSRP-201). */
public interface WatchlistSourceRepository extends JpaRepository<WatchlistSource, Long> {

  /**
   * A source by code.
   *
   * @param code code
   * @return source
   */
  Optional<WatchlistSource> findByCode(String code);

  /**
   * All sources by code.
   *
   * @return sources
   */
  List<WatchlistSource> findAllByOrderByCodeAsc();

  /**
   * The active sources of a transport (the scheduled job reads the FILE sources).
   *
   * @param transport transport
   * @return sources
   */
  List<WatchlistSource> findByActiveTrueAndTransportOrderByCodeAsc(SourceTransport transport);
}
