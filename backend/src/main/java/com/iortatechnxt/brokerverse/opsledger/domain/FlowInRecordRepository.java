package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Flow-in records (BRQID.005 idempotency). */
public interface FlowInRecordRepository extends JpaRepository<FlowInRecord, Long> {

  /**
   * A record by feed and key.
   *
   * @param feedCode feed
   * @param idempotencyKey key
   * @return record
   */
  Optional<FlowInRecord> findByFeedCodeAndIdempotencyKey(String feedCode, String idempotencyKey);

  /**
   * Records of a run.
   *
   * @param runId run
   * @param pageable page
   * @return records
   */
  Page<FlowInRecord> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);
}
