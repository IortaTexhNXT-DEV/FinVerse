package com.iortatechnxt.brokerverse.issuance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Extraction patterns. */
public interface ExtractionPatternRepository extends JpaRepository<ExtractionPattern, Long> {

  /**
   * Active patterns by priority.
   *
   * @return patterns
   */
  List<ExtractionPattern> findByActiveTrueOrderByPriorityAscIdAsc();

  /**
   * Active patterns of a document kind.
   *
   * @param kind EPOLICY or SUBMITTED_POLICY
   * @return patterns by priority
   */
  List<ExtractionPattern> findByKindAndActiveTrueOrderByPriorityAscIdAsc(String kind);
}
