package com.iortatechnxt.brokerverse.bulk.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bulk jobs. */
public interface BulkJobRepository extends JpaRepository<BulkJob, Long> {

  /**
   * Jobs of a handler, newest first.
   *
   * @param companyId company
   * @param handlerCode handler
   * @param pageable page
   * @return jobs
   */
  Page<BulkJob> findByCompanyIdAndHandlerCodeOrderByIdDesc(
      Long companyId, String handlerCode, Pageable pageable);

  /**
   * All jobs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return jobs
   */
  Page<BulkJob> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
