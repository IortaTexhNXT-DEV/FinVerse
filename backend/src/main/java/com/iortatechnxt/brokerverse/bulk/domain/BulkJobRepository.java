package com.iortatechnxt.brokerverse.bulk.domain;

import java.util.Optional;
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

  /**
   * An earlier upload of the same file for a handler, in a status other than the one given
   * (duplicate upload detection, CSHID.008).
   *
   * @param companyId company
   * @param handlerCode handler
   * @param fileSha256 SHA-256 of the file
   * @param status status to ignore (cancelled uploads)
   * @return the first such upload
   */
  Optional<BulkJob> findFirstByCompanyIdAndHandlerCodeAndFileSha256AndStatusNotOrderByIdAsc(
      Long companyId, String handlerCode, String fileSha256, BulkJobStatus status);
}
