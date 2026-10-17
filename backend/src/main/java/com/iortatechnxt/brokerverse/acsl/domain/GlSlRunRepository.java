package com.iortatechnxt.brokerverse.acsl.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** GL-SL reconciliation runs (ACSL 2.13.2). */
public interface GlSlRunRepository extends JpaRepository<GlSlRun, Long> {

  /**
   * The latest run of a company on or before a date.
   *
   * @param companyId company
   * @param asOf date
   * @return run
   */
  Optional<GlSlRun> findFirstByCompanyIdAndAsOfLessThanEqualOrderByAsOfDescIdDesc(
      Long companyId, LocalDate asOf);

  /**
   * The latest runs of a company.
   *
   * @param companyId company
   * @return up to 30 runs, newest first
   */
  List<GlSlRun> findTop30ByCompanyIdOrderByIdDesc(Long companyId);
}
