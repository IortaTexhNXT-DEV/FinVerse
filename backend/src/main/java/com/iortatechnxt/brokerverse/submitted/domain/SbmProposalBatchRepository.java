package com.iortatechnxt.brokerverse.submitted.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Batches of renewal proposals (FR-SP-066). */
public interface SbmProposalBatchRepository extends JpaRepository<SbmProposalBatch, Long> {

  /**
   * The batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<SbmProposalBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
