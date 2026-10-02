package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Run log of the batches. */
public interface BatchLogRepository extends JpaRepository<BatchLog, Long> {

  /**
   * The log of a batch in order.
   *
   * @param batchId batch
   * @return lines
   */
  List<BatchLog> findByBatchIdOrderByIdAsc(Long batchId);
}
