package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Document extractions. */
public interface SbmExtractionRepository extends JpaRepository<SbmExtraction, Long> {

  /**
   * An extraction by number.
   *
   * @param extractionNo number
   * @return extraction
   */
  Optional<SbmExtraction> findByExtractionNo(String extractionNo);

  /**
   * Extractions in some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return extractions
   */
  Page<SbmExtraction> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<SbmExtraction.Status> statuses, Pageable pageable);

  /**
   * Extractions of a record.
   *
   * @param policyId record
   * @return extractions, newest first
   */
  List<SbmExtraction> findByPolicyIdOrderByIdDesc(Long policyId);

  /**
   * Extractions waiting for confirmation.
   *
   * @param companyId company
   * @param status PROPOSED
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, SbmExtraction.Status status);
}
