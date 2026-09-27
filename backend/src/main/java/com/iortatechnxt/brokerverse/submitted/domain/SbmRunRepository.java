package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Processing runs. */
public interface SbmRunRepository extends JpaRepository<SbmRun, Long> {

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  Page<SbmRun> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * The latest run of a company.
   *
   * @param companyId company
   * @return run
   */
  Optional<SbmRun> findFirstByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * A run by number.
   *
   * @param runNo run number
   * @return run
   */
  Optional<SbmRun> findByRunNo(String runNo);
}
