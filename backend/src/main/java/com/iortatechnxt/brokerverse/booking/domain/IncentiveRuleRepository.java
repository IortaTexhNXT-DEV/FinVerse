package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Incentive eligibility rules. */
public interface IncentiveRuleRepository extends JpaRepository<IncentiveRule, Long> {

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<IncentiveRule> findByCompanyIdOrderByIdAsc(Long companyId);
}
