package com.iortatechnxt.finverse.period.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link FiscalYear}. */
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {

  /**
   * Lists years of a company, newest first.
   *
   * @param companyId company
   * @return years
   */
  List<FiscalYear> findByCompanyIdOrderByYearCodeDesc(Long companyId);

  /**
   * Checks whether a year exists.
   *
   * @param companyId company
   * @param yearCode year
   * @return true when present
   */
  boolean existsByCompanyIdAndYearCode(Long companyId, int yearCode);

  /**
   * Finds the fiscal year containing a date.
   *
   * @param companyId company
   * @param date date
   * @return year if defined
   */
  @Query(
      "select y from FiscalYear y where y.companyId = :companyId"
          + " and :date between y.startDate and y.endDate")
  Optional<FiscalYear> findContaining(
      @Param("companyId") Long companyId, @Param("date") LocalDate date);
}
