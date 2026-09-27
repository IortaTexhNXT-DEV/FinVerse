package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** LAMD report lines. */
public interface LamdLineRepository extends JpaRepository<LamdLine, Long> {

  /**
   * Lines of a report.
   *
   * @param reportId report
   * @return lines
   */
  List<LamdLine> findByReportIdOrderByRowNoAsc(Long reportId);

  /**
   * Matched lines of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return lines
   */
  List<LamdLine> findByCandidateIdOrderByIdDesc(Long candidateId);
}
