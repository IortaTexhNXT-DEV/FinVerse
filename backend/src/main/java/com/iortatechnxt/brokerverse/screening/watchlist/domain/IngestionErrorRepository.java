package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Records that failed ingestion (SNSRP-202). */
public interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {

  /**
   * The failed records of a run.
   *
   * @param runId run
   * @return records by line
   */
  List<IngestionError> findByRunIdOrderByLineNoAsc(Long runId);

  /**
   * The failed records not yet sent in a digest.
   *
   * @return records by run and line
   */
  List<IngestionError> findByDigestedAtIsNullOrderByRunIdAscLineNoAsc();
}
