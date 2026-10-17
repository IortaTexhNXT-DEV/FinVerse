package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cutover tasks. */
public interface CutoverTaskRepository extends JpaRepository<CutoverTask, Long> {

  /**
   * Tasks of a plan in sequence.
   *
   * @param planId plan
   * @return tasks
   */
  List<CutoverTask> findByPlanIdOrderBySeqAsc(Long planId);
}
