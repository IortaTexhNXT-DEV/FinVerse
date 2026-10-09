package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Annual Renewal Budget records (FRRN.042). */
public interface RenewalBudgetRepository extends JpaRepository<RenewalBudget, Long> {

  /**
   * A record by its natural key.
   *
   * @param companyId company
   * @param naturalKey key text
   * @return the record
   */
  Optional<RenewalBudget> findByCompanyIdAndNaturalKey(Long companyId, String naturalKey);

  /**
   * The records of a fiscal year.
   *
   * @param companyId company
   * @param fiscalYear fiscal year
   * @return records
   */
  List<RenewalBudget> findByCompanyIdAndFiscalYearOrderBySegmentAscRegionAscTeamAscIdAsc(
      Long companyId, int fiscalYear);

  /**
   * The records of the fiscal years of a period.
   *
   * @param companyId company
   * @param fromYear first year
   * @param toYear last year
   * @return records
   */
  List<RenewalBudget> findByCompanyIdAndFiscalYearBetween(Long companyId, int fromYear, int toYear);
}
