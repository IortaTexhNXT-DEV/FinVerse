package com.iortatechnxt.brokerverse.csf.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Agent activity log. */
public interface CsfActivityRepository extends JpaRepository<CsfActivity, Long> {

  /**
   * Activity on a client since a time, newest first.
   *
   * @param clientId client
   * @param since first time
   * @return activity
   */
  List<CsfActivity> findByClientIdAndOccurredAtGreaterThanEqualOrderByOccurredAtDesc(
      Long clientId, Instant since);
}
