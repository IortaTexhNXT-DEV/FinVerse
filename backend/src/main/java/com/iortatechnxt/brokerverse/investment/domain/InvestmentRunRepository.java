package com.iortatechnxt.brokerverse.investment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link InvestmentRun}. */
public interface InvestmentRunRepository extends JpaRepository<InvestmentRun, Long> {

  /**
   * Finds the run of a type and period.
   *
   * @param companyId company
   * @param runType type
   * @param period period (YYYY-MM)
   * @return run if posted
   */
  Optional<InvestmentRun> findByCompanyIdAndRunTypeAndPeriod(
      Long companyId, RunType runType, String period);

  /**
   * Lists runs, newest period first.
   *
   * @param companyId company
   * @return runs
   */
  List<InvestmentRun> findByCompanyIdOrderByPeriodDescRunTypeAsc(Long companyId);
}
