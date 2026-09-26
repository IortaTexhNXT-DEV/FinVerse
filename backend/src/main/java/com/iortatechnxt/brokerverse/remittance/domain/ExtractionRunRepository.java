package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Remittance extraction runs. */
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, Long> {

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  Page<ExtractionRun> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * A run by number.
   *
   * @param runNo run number
   * @return run
   */
  Optional<ExtractionRun> findByRunNo(String runNo);
}
