package com.iortatechnxt.brokerverse.nbreport.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Production targets. */
public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {

  /**
   * Targets of a company whose period overlaps a date range.
   *
   * @param companyId company
   * @param from range start
   * @param to range end
   * @return targets by level, unit and period
   */
  @Query(
      "select t from SalesTarget t where t.companyId = :companyId and t.periodFrom <= :to"
          + " and t.periodTo >= :from order by t.unitLevel, t.unitCode, t.periodFrom")
  List<SalesTarget> overlapping(Long companyId, LocalDate from, LocalDate to);

  /**
   * The target of a unit starting on a date.
   *
   * @param companyId company
   * @param level level
   * @param code unit
   * @param from period start
   * @return target
   */
  Optional<SalesTarget> findByCompanyIdAndUnitLevelAndUnitCodeAndPeriodFrom(
      Long companyId, UnitLevel level, String code, LocalDate from);
}
