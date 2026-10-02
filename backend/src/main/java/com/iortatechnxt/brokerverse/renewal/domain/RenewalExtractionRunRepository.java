package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Extraction runs. */
public interface RenewalExtractionRunRepository extends JpaRepository<ExtractionRun, Long> {

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  Page<ExtractionRun> findByCompanyIdOrderByStartedAtDesc(Long companyId, Pageable pageable);

  /**
   * The latest completed scheduled run (the next run catches up from its date).
   *
   * @param companyId company
   * @param trigger trigger
   * @param status status
   * @return run
   */
  Optional<ExtractionRun> findFirstByCompanyIdAndTriggerAndStatusOrderByExpiryToDesc(
      Long companyId, ExtractionTrigger trigger, ExtractionRun.Status status);

  /**
   * Runs of a trigger.
   *
   * @param companyId company
   * @param trigger trigger
   * @return runs
   */
  List<ExtractionRun> findByCompanyIdAndTrigger(Long companyId, ExtractionTrigger trigger);
}
