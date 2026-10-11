package com.iortatechnxt.brokerverse.renewal.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Letter batches. */
public interface LetterBatchRepository extends JpaRepository<LetterBatch, Long> {

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<LetterBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
