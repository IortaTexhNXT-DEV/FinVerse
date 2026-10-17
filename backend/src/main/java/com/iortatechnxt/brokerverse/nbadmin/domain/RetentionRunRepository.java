package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Retention review results. */
public interface RetentionRunRepository extends JpaRepository<RetentionRun, Long> {

  /**
   * Latest result of a rule.
   *
   * @param ruleId rule
   * @return latest run
   */
  Optional<RetentionRun> findTopByRuleIdOrderByIdDesc(Long ruleId);
}
