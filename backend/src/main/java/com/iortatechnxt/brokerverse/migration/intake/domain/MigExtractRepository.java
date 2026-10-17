package com.iortatechnxt.brokerverse.migration.intake.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Source extracts. */
public interface MigExtractRepository extends JpaRepository<MigExtract, Long> {

  /**
   * An extract by number.
   *
   * @param extractNo number
   * @return extract
   */
  Optional<MigExtract> findByExtractNo(String extractNo);

  /**
   * Extracts of an object with the same file hash (duplicate check).
   *
   * @param objectCode object
   * @param sha256 file hash
   * @param statuses statuses that count as received
   * @return extracts
   */
  List<MigExtract> findByObjectCodeAndSha256AndStatusIn(
      String objectCode, String sha256, Collection<ExtractStatus> statuses);

  /**
   * Staged extracts of a layout, latest as-of first (as-of order check).
   *
   * @param companyId company
   * @param layoutCode layout
   * @param statuses statuses
   * @return extracts
   */
  List<MigExtract> findByCompanyIdAndLayoutCodeAndStatusInOrderByAsOfDesc(
      Long companyId, String layoutCode, Collection<ExtractStatus> statuses);

  /**
   * Extracts of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return extracts
   */
  Page<MigExtract> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Extracts of an object, newest first.
   *
   * @param companyId company
   * @param objectCode object
   * @param pageable page
   * @return extracts
   */
  Page<MigExtract> findByCompanyIdAndObjectCodeOrderByIdDesc(
      Long companyId, String objectCode, Pageable pageable);

  /**
   * Staged extracts of an object not yet used by a batch.
   *
   * @param companyId company
   * @param objectCode object
   * @param status status
   * @return extracts
   */
  List<MigExtract> findByCompanyIdAndObjectCodeAndStatusOrderByIdAsc(
      Long companyId, String objectCode, ExtractStatus status);

  /**
   * Extracts by status (purge of rejected extracts).
   *
   * @param status status
   * @return extracts
   */
  List<MigExtract> findByStatus(ExtractStatus status);
}
