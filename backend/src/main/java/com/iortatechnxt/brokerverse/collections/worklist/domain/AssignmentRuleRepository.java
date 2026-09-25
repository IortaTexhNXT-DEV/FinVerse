package com.iortatechnxt.brokerverse.collections.worklist.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Default assignment rules (BRCLXN.052). */
public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {

  /**
   * Rules of a company in priority order.
   *
   * @param companyId company
   * @return rules
   */
  List<AssignmentRule> findByCompanyIdOrderByPriorityAscIdAsc(Long companyId);

  /**
   * Active rules of a company in priority order.
   *
   * @param companyId company
   * @return rules
   */
  List<AssignmentRule> findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(Long companyId);
}
