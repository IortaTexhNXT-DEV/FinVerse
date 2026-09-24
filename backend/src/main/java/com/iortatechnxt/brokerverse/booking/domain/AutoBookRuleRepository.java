package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Auto-book rules. */
public interface AutoBookRuleRepository extends JpaRepository<AutoBookRule, Long> {

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<AutoBookRule> findByCompanyIdOrderByIdAsc(Long companyId);
}
