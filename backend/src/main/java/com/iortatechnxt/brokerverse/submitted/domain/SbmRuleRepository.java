package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rules of the rule sets. */
public interface SbmRuleRepository extends JpaRepository<SbmRule, Long> {

  /**
   * Rules of a rule set, highest priority first.
   *
   * @param ruleSetId rule set
   * @return rules
   */
  List<SbmRule> findByRuleSetIdOrderByPriorityDesc(Long ruleSetId);

  /**
   * Rules of several rule sets.
   *
   * @param ruleSetIds rule sets
   * @return rules
   */
  List<SbmRule> findByRuleSetIdIn(Collection<Long> ruleSetIds);
}
