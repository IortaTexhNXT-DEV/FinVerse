package com.iortatechnxt.brokerverse.submitted.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Field history of the masterlist records. */
public interface SbmPolicyHistoryRepository extends JpaRepository<SbmPolicyHistory, Long> {

  /**
   * History of a record, newest first.
   *
   * @param policyId record
   * @return changes
   */
  List<SbmPolicyHistory> findByPolicyIdOrderByIdDesc(Long policyId);

  /**
   * Whether a record changed after a time (quick filter "Changed since").
   *
   * @param policyId record
   * @param since time
   * @return true when changed
   */
  boolean existsByPolicyIdAndCreatedAtGreaterThanEqual(Long policyId, Instant since);
}
