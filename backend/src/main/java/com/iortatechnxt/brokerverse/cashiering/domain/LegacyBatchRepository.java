package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cashiering legacy batches. */
public interface LegacyBatchRepository extends JpaRepository<LegacyBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return the batch
   */
  Optional<LegacyBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company and kind, newest first.
   *
   * @param companyId company
   * @param kind kind
   * @return batches
   */
  List<LegacyBatch> findByCompanyIdAndKindOrderByIdDesc(Long companyId, LegacyBatch.Kind kind);

  /**
   * Batches in a status (approvals).
   *
   * @param status status
   * @return batches
   */
  List<LegacyBatch> findByStatusOrderByIdAsc(LegacyBatch.Status status);
}
