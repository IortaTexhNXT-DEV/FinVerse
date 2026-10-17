package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Go / no-go criteria. */
public interface GonogoCriterionRepository extends JpaRepository<GonogoCriterion, Long> {

  /**
   * Criteria of a plan in order.
   *
   * @param planId plan
   * @return criteria
   */
  List<GonogoCriterion> findByPlanIdOrderByCriterionNoAsc(Long planId);
}
