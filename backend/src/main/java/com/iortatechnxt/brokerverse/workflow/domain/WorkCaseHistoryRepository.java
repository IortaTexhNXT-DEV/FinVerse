package com.iortatechnxt.brokerverse.workflow.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Status history. */
public interface WorkCaseHistoryRepository extends JpaRepository<WorkCaseHistory, Long> {

  /**
   * History of a case, oldest first.
   *
   * @param caseId case
   * @return changes
   */
  List<WorkCaseHistory> findByCaseIdOrderByIdAsc(Long caseId);
}
