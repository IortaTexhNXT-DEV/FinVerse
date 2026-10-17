package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Go / no-go decisions. */
public interface GonogoDecisionRepository extends JpaRepository<GonogoDecision, Long> {

  /**
   * Decisions of a plan, newest first.
   *
   * @param planId plan
   * @return decisions
   */
  List<GonogoDecision> findByPlanIdOrderByIdDesc(Long planId);
}
