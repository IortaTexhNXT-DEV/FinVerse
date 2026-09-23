package com.iortatechnxt.finverse.tax.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tax returns. */
public interface TaxReturnRepository extends JpaRepository<TaxReturn, Long> {

  /**
   * Loads a return with its lines.
   *
   * @param id id
   * @return return
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  Optional<TaxReturn> findWithLinesById(Long id);

  /**
   * The live (not cancelled) return of a form and period.
   *
   * @param companyId company
   * @param formCode form
   * @param periodStart period start
   * @param cancelled the cancelled status to exclude
   * @return return
   */
  Optional<TaxReturn> findByCompanyIdAndFormCodeAndPeriodStartAndStatusNot(
      Long companyId, String formCode, LocalDate periodStart, ReturnStatus cancelled);

  /**
   * Returns of a company whose period starts in a date range, optionally filtered.
   *
   * @param companyId company
   * @param from period start from
   * @param to period start to
   * @param formCode form, null for all
   * @param status status, null for all
   * @return returns ordered by period and form
   */
  @Query(
      "select r from TaxReturn r where r.companyId = :companyId"
          + " and r.periodStart between :from and :to"
          + " and (:formCode is null or r.formCode = :formCode)"
          + " and (:status is null or r.status = :status)"
          + " order by r.periodStart, r.formCode")
  List<TaxReturn> search(
      @Param("companyId") Long companyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("formCode") String formCode,
      @Param("status") ReturnStatus status);

  /**
   * Returns of a worksheet kind in given statuses whose period lies inside a date range (e.g. the
   * monthly 0619-E remittances inside a quarter).
   *
   * @param companyId company
   * @param worksheet worksheet kind
   * @param from range start
   * @param to range end
   * @param statuses statuses
   * @return returns
   */
  @Query(
      "select r from TaxReturn r where r.companyId = :companyId and r.worksheet = :worksheet"
          + " and r.periodStart >= :from and r.periodEnd <= :to and r.status in :statuses")
  List<TaxReturn> findInside(
      @Param("companyId") Long companyId,
      @Param("worksheet") WorksheetKind worksheet,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("statuses") Collection<ReturnStatus> statuses);

  /**
   * The last return of a worksheet kind ending on a date (e.g. the previous quarter's 2550Q).
   *
   * @param companyId company
   * @param worksheet worksheet kind
   * @param periodEnd period end
   * @param cancelled the cancelled status to exclude
   * @return return
   */
  Optional<TaxReturn> findFirstByCompanyIdAndWorksheetAndPeriodEndAndStatusNot(
      Long companyId, WorksheetKind worksheet, LocalDate periodEnd, ReturnStatus cancelled);
}
