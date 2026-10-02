package com.iortatechnxt.brokerverse.organization.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Holiday}. */
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

  /**
   * Lists holidays of a company within a date range, ordered by date.
   *
   * @param companyId company id
   * @param from inclusive start
   * @param to inclusive end
   * @return holidays
   */
  List<Holiday> findByCompanyIdAndHolidayDateBetweenOrderByHolidayDate(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Checks whether a date is a holiday for a branch (branch specific or company-wide).
   *
   * @param companyId company id
   * @param branchId branch id
   * @param date date
   * @return true when the date is a holiday
   */
  @Query(
      """
      select count(h) > 0 from Holiday h
      where h.companyId = :companyId and h.holidayDate = :date
        and (h.branchId is null or h.branchId = :branchId)
      """)
  boolean isHoliday(
      @Param("companyId") Long companyId,
      @Param("branchId") Long branchId,
      @Param("date") LocalDate date);
}
