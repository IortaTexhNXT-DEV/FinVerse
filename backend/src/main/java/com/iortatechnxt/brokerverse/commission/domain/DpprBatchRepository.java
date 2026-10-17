package com.iortatechnxt.brokerverse.commission.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy direct payment PR reversal batches. */
public interface DpprBatchRepository extends JpaRepository<DpprBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return the batch
   */
  Optional<DpprBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  List<DpprBatch> findByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Batches in a status.
   *
   * @param status status
   * @return batches
   */
  List<DpprBatch> findByStatusOrderByIdAsc(DpprBatch.Status status);
}
