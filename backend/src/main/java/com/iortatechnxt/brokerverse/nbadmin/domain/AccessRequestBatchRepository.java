package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bulk access request batches (BRD 1.009). */
public interface AccessRequestBatchRepository extends JpaRepository<AccessRequestBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo batch number (the bulk upload number)
   * @return batch if any
   */
  Optional<AccessRequestBatch> findByBatchNo(String batchNo);

  /**
   * Batches created by a user, newest first by the pageable sort.
   *
   * @param createdBy creator
   * @param pageable page
   * @return batches
   */
  Page<AccessRequestBatch> findByCreatedByIgnoreCase(String createdBy, Pageable pageable);
}
