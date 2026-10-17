package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bucket history. */
public interface BucketHistoryRepository extends JpaRepository<BucketHistory, Long> {

  /**
   * Bucket changes of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return changes
   */
  List<BucketHistory> findByCandidateIdOrderByIdDesc(Long candidateId);
}
