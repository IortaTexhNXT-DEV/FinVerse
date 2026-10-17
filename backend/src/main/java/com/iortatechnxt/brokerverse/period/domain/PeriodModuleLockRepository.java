package com.iortatechnxt.brokerverse.period.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PeriodModuleLock}. */
public interface PeriodModuleLockRepository extends JpaRepository<PeriodModuleLock, Long> {

  /**
   * The cut-off record of a period and group of books.
   *
   * @param periodId period
   * @param module group of books
   * @return record if any
   */
  Optional<PeriodModuleLock> findByPeriodIdAndModule(Long periodId, String module);

  /**
   * Whether a group of books is locked in a period.
   *
   * @param periodId period
   * @param module group of books
   * @return true when locked
   */
  boolean existsByPeriodIdAndModuleAndLockedTrue(Long periodId, String module);

  /**
   * Cut-off records of a company, newest period first.
   *
   * @param companyId company
   * @param module group of books
   * @return records
   */
  List<PeriodModuleLock> findByCompanyIdAndModuleOrderByPeriodIdDesc(Long companyId, String module);
}
