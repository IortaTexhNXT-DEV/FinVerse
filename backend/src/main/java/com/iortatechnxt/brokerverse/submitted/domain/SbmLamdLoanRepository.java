package com.iortatechnxt.brokerverse.submitted.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Loan files of the bank (LAMD, LMS, LAD). */
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
   * The rows of a loan file for a PN number (the caller keeps the one with the same loan
   * application number: loading the same loan again for the same report and date replaces it).
   *
   * @param companyId company
   * @param loanReport loan report
   * @param snapshotDate date of the file
   * @param pnNo PN number
   * @return rows
   */
  List<SbmLamdLoan> findByCompanyIdAndLoanReportAndSnapshotDateAndPnNo(
      Long companyId, String loanReport, LocalDate snapshotDate, String pnNo);

  /**
   * The rows of a loan file for a loan application number.
   *
   * @param companyId company
   * @param loanReport loan report
   * @param snapshotDate date of the file
   * @param loanApplicationNo loan application number
   * @return rows
   */
  List<SbmLamdLoan> findByCompanyIdAndLoanReportAndSnapshotDateAndLoanApplicationNo(
      Long companyId, String loanReport, LocalDate snapshotDate, String loanApplicationNo);

  /**
   * The rows of a PN number in every loan file, newest file first.
   *
   * @param companyId company
   * @param pnNo PN number
   * @return rows
   */
  List<SbmLamdLoan> findByCompanyIdAndPnNoOrderBySnapshotDateDescIdDesc(
      Long companyId, String pnNo);

  /**
   * The rows of a loan application number in every loan file, newest file first.
   *
   * @param companyId company
   * @param loanApplicationNo loan application number
   * @return rows
   */
  List<SbmLamdLoan> findByCompanyIdAndLoanApplicationNoOrderBySnapshotDateDescIdDesc(
      Long companyId, String loanApplicationNo);

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
