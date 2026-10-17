package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Retention rules. */
public interface RetentionRuleRepository extends JpaRepository<RetentionRule, Long> {

  /**
   * All rules by record type.
   *
   * @return rules
   */
  List<RetentionRule> findAllByOrderByRecordTypeAscIdAsc();
}
