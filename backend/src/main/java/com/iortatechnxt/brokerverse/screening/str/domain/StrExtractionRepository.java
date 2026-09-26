package com.iortatechnxt.brokerverse.screening.str.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** STR extractions (SNSRP-706). */
public interface StrExtractionRepository extends JpaRepository<StrExtraction, Long> {

  /**
   * The extractions of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return extractions
   */
  Page<StrExtraction> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
