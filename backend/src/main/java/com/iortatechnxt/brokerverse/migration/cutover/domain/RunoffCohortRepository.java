package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Run-off cohorts. */
public interface RunoffCohortRepository extends JpaRepository<RunoffCohort, Long> {

  /**
   * Cohorts of a snapshot.
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   * @return cohorts by month and source
   */
  List<RunoffCohort> findByCompanyIdAndSnapshotDateOrderByExpiryMonthAscSourceSystemAsc(
      Long companyId, LocalDate snapshotDate);

  /**
   * The latest snapshot date of a company.
   *
   * @param companyId company
   * @return date
   */
  @Query("select max(c.snapshotDate) from RunoffCohort c where c.companyId = ?1")
  Optional<LocalDate> latestSnapshot(Long companyId);

  /**
   * Removes the cohorts of a snapshot (a rerun of the same day replaces them).
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   */
  void deleteByCompanyIdAndSnapshotDate(Long companyId, LocalDate snapshotDate);
}
