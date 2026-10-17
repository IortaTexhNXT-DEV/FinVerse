package com.iortatechnxt.brokerverse.commission.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Direct payment lists (CMRID.001). */
public interface DpListRepository extends JpaRepository<DpList, Long> {

  /**
   * A list with the same file (duplicate block).
   *
   * @param companyId company
   * @param sha256 checksum
   * @return list
   */
  Optional<DpList> findFirstByCompanyIdAndSha256(Long companyId, String sha256);

  /**
   * Lists received in a period, newest first.
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @param pageable page
   * @return lists
   */
  Page<DpList> findByCompanyIdAndSubmissionDateBetweenOrderBySubmissionDateDescIdDesc(
      Long companyId, LocalDate from, LocalDate to, Pageable pageable);

  /**
   * Every list of a period (submission tracker).
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @return lists
   */
  List<DpList> findByCompanyIdAndSubmissionDateBetweenOrderByBranchCodeAsc(
      Long companyId, LocalDate from, LocalDate to);
}
