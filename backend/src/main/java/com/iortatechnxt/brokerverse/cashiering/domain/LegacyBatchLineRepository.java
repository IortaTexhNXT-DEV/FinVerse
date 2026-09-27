package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Lines of the Cashiering legacy batches. */
public interface LegacyBatchLineRepository extends JpaRepository<LegacyBatchLine, Long> {

  /**
   * Lines of a batch.
   *
   * @param batchId batch
   * @return lines by number
   */
  List<LegacyBatchLine> findByBatchIdOrderByLineNoAsc(Long batchId);

  /**
   * Whether an unapplied item is already on an open or executed batch line not refused.
   *
   * @param unappliedId item
   * @return true when taken
   */
  @Query(
      "select count(l) > 0 from LegacyBatchLine l, LegacyBatch b where b.id = l.batchId"
          + " and l.unappliedId = ?1 and l.status <> 'FAILED'"
          + " and b.status <> com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatch.Status.CANCELLED")
  boolean taken(Long unappliedId);
}
