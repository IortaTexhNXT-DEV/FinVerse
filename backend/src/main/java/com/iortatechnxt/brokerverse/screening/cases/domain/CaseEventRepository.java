package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The insert-only case timeline (SNSRP-401, 903). */
public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

  /**
   * The timeline of a case, oldest first.
   *
   * @param caseId case
   * @return events
   */
  List<CaseEvent> findByCaseIdOrderByIdAsc(Long caseId);
}
