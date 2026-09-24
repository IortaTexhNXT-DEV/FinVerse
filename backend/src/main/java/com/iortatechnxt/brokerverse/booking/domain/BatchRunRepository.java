package com.iortatechnxt.brokerverse.booking.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Booking batch runs. */
public interface BatchRunRepository extends JpaRepository<BatchRun, Long> {

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return page
   */
  Page<BatchRun> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * A run by number.
   *
   * @param runNo run number
   * @return run
   */
  Optional<BatchRun> findByRunNo(String runNo);
}
