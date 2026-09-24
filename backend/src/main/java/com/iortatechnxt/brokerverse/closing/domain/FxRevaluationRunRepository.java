package com.iortatechnxt.brokerverse.closing.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link FxRevaluationRun}. */
public interface FxRevaluationRunRepository extends JpaRepository<FxRevaluationRun, Long> {

  /**
   * Finds the run of a period.
   *
   * @param companyId company
   * @param periodId period
   * @return run if the period was revalued
   */
  Optional<FxRevaluationRun> findByCompanyIdAndPeriodId(Long companyId, Long periodId);

  /**
   * Lists the runs of a company, newest first.
   *
   * @param companyId company
   * @return runs
   */
  List<FxRevaluationRun> findByCompanyIdOrderByRevaluationDateDesc(Long companyId);

  /**
   * Latest run on or before a date.
   *
   * @param companyId company
   * @param date date
   * @return run
   */
  Optional<FxRevaluationRun>
      findFirstByCompanyIdAndRevaluationDateLessThanEqualOrderByRevaluationDateDesc(
          Long companyId, LocalDate date);
}
