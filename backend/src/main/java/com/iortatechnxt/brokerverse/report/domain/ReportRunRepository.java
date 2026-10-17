package com.iortatechnxt.brokerverse.report.domain;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Archived report runs. */
public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {

  /**
   * Runs of some reports, newest first.
   *
   * @param codes report codes
   * @param pageable page
   * @return runs
   */
  Page<ReportRun> findByReportCodeInOrderByIdDesc(Collection<String> codes, Pageable pageable);
}
