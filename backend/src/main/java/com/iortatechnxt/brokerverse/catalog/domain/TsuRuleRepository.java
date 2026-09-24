package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** TSU routing rules. */
public interface TsuRuleRepository extends JpaRepository<TsuRule, Long> {

  /**
   * Rules in evaluation order.
   *
   * @return rules
   */
  List<TsuRule> findAllByOrderByPriorityAscCodeAsc();

  /**
   * A rule by code.
   *
   * @param code code
   * @return rule
   */
  Optional<TsuRule> findByCode(String code);
}
