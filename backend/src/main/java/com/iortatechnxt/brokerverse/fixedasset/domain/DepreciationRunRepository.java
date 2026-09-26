package com.iortatechnxt.brokerverse.fixedasset.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link DepreciationRun} and its lines. */
public interface DepreciationRunRepository extends JpaRepository<DepreciationRun, Long> {

  /**
   * Finds the run of a period.
   *
   * @param companyId company
   * @param period period (YYYY-MM)
   * @return run if posted
   */
  Optional<DepreciationRun> findByCompanyIdAndPeriod(Long companyId, String period);

  /**
   * Lists runs, newest period first.
   *
   * @param companyId company
   * @return runs
   */
  List<DepreciationRun> findByCompanyIdOrderByPeriodDesc(Long companyId);
}
