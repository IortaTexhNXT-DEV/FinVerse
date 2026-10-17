package com.iortatechnxt.brokerverse.accounting.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AccountingRule}. */
public interface AccountingRuleRepository extends JpaRepository<AccountingRule, Long> {

  /**
   * Lists the rules of an event type.
   *
   * @param companyId company
   * @param eventType event type
   * @return rules
   */
  List<AccountingRule> findByCompanyIdAndEventType(Long companyId, String eventType);

  /**
   * Lists all rules of a company.
   *
   * @param companyId company
   * @return rules ordered by event type and priority
   */
  List<AccountingRule> findByCompanyIdOrderByEventTypeAscPriorityAsc(Long companyId);
}
