package com.iortatechnxt.brokerverse.placement.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payment gate rules. */
public interface PaymentGateRuleRepository extends JpaRepository<PaymentGateRule, Long> {

  /**
   * Active rules, highest priority (lowest number) first.
   *
   * @return rules
   */
  List<PaymentGateRule> findByActiveTrueOrderByPriorityAsc();
}
