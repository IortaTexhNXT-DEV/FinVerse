package com.iortatechnxt.brokerverse.cashiering.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Batch prints (CSHID.019). */
public interface PrintBatchRepository extends JpaRepository<PrintBatch, Long> {

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<PrintBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
