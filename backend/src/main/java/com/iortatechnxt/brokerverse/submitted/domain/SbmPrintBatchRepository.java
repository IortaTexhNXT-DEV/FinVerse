package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Print batches. */
public interface SbmPrintBatchRepository extends JpaRepository<SbmPrintBatch, Long> {

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<SbmPrintBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return batch
   */
  Optional<SbmPrintBatch> findByBatchNo(String batchNo);
}
