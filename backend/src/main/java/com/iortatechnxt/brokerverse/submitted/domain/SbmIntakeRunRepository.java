package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Intake runs. */
public interface SbmIntakeRunRepository extends JpaRepository<SbmIntakeRun, Long> {

  /**
   * The run of a bulk upload.
   *
   * @param companyId company
   * @param bulkJobNo bulk upload number
   * @return run
   */
  Optional<SbmIntakeRun> findByCompanyIdAndBulkJobNo(Long companyId, String bulkJobNo);

  /**
   * A run by number.
   *
   * @param runNo run number
   * @return run
   */
  Optional<SbmIntakeRun> findByRunNo(String runNo);

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  Page<SbmIntakeRun> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
