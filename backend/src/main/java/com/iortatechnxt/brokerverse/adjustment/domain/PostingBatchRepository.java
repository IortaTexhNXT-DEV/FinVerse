package com.iortatechnxt.brokerverse.adjustment.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Posting batches (ADJID.006). */
public interface PostingBatchRepository extends JpaRepository<PostingBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo batch number
   * @return batch
   */
  Optional<PostingBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<PostingBatch> findByCompanyId(Long companyId, Pageable pageable);
}
