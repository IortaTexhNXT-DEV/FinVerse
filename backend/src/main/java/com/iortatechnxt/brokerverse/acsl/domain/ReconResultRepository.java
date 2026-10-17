package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** SOA reconciliation results (ACSL 2.14.1). */
public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {

  /**
   * Results of a run in file order.
   *
   * @param runId run
   * @return results
   */
  List<ReconResult> findByRunIdOrderByRowNo(Long runId);

  /**
   * Results of a run in one bucket, in file order.
   *
   * @param runId run
   * @param bucket bucket
   * @return results
   */
  List<ReconResult> findByRunIdAndBucketOrderByRowNo(Long runId, ReconBucket bucket);
}
