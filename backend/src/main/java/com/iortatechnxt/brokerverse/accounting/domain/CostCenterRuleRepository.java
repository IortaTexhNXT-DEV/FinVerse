package com.iortatechnxt.brokerverse.accounting.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link CostCenterRule}. */
public interface CostCenterRuleRepository extends JpaRepository<CostCenterRule, Long> {

  /**
   * Rules of a company in evaluation order.
   *
   * @param companyId company
   * @return rules by priority, then id
   */
  List<CostCenterRule> findByCompanyIdOrderByPriorityAscIdAsc(Long companyId);
}
