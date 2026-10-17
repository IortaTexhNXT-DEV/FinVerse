package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import java.time.LocalDate;
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

  /**
   * Whether a run of a trigger exists for a business date (one scheduled run a day).
   *
   * @param companyId company
   * @param trigger trigger
   * @param businessDate business date
   * @return true when one exists
   */
  boolean existsByCompanyIdAndTriggerAndBusinessDate(
      Long companyId, ExtractionTrigger trigger, LocalDate businessDate);
}
