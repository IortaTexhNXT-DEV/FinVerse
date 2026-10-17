package com.iortatechnxt.brokerverse.period.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link AccountingPeriod}. */
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

  /**
   * Lists the periods of a fiscal year.
   *
   * @param fiscalYearId year
   * @return periods ordered by number
   */
  List<AccountingPeriod> findByFiscalYearIdOrderByPeriodNo(Long fiscalYearId);

  /**
   * Finds the period containing a date.
   *
   * @param companyId company
   * @param date date
   * @return period if defined
   */
  @Query(
      "select p from AccountingPeriod p where p.companyId = :companyId"
          + " and :date between p.startDate and p.endDate")
  Optional<AccountingPeriod> findContaining(
      @Param("companyId") Long companyId, @Param("date") LocalDate date);

  /**
   * Lists periods of a company in given statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return periods ordered by start date
   */
  List<AccountingPeriod> findByCompanyIdAndStatusInOrderByStartDate(
      Long companyId, List<PeriodStatus> statuses);
}
