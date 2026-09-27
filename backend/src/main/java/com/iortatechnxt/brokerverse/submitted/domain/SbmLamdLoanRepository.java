package com.iortatechnxt.brokerverse.submitted.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** LAMD loan snapshots. */
public interface SbmLamdLoanRepository extends JpaRepository<SbmLamdLoan, Long> {

  /**
   * A loan of a snapshot.
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   * @param pnNo PN
   * @return loan
   */
  Optional<SbmLamdLoan> findByCompanyIdAndSnapshotDateAndPnNo(
      Long companyId, LocalDate snapshotDate, String pnNo);

  /**
   * The latest snapshot row of a PN.
   *
   * @param companyId company
   * @param pnNo PN
   * @return loan
   */
  Optional<SbmLamdLoan> findFirstByCompanyIdAndPnNoOrderBySnapshotDateDescIdDesc(
      Long companyId, String pnNo);

  /**
   * Loans of a snapshot.
   *
   * @param companyId company
   * @param snapshotDate date
   * @param pageable page
   * @return loans
   */
  Page<SbmLamdLoan> findByCompanyIdAndSnapshotDateOrderByPnNoAsc(
      Long companyId, LocalDate snapshotDate, Pageable pageable);

  /**
   * Snapshot dates with their loan counts, newest first.
   *
   * @param companyId company
   * @return rows of date and count
   */
  @Query(
      "select l.snapshotDate, count(l) from SbmLamdLoan l where l.companyId = :companyId"
          + " group by l.snapshotDate order by l.snapshotDate desc")
  List<Object[]> snapshots(@Param("companyId") Long companyId);
}
