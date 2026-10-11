package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EodStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** End-of-day runs (DIS 2.16). */
public interface EodRunRepository extends JpaRepository<EodRun, Long> {

  /**
   * The run of a business date.
   *
   * @param companyId company
   * @param businessDate date
   * @return run
   */
  Optional<EodRun> findByCompanyIdAndBusinessDate(Long companyId, LocalDate businessDate);

  /**
   * Runs of a company, newest date first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  Page<EodRun> findByCompanyIdOrderByBusinessDateDesc(Long companyId, Pageable pageable);

  /**
   * Runs in a status up to a date, oldest first (confirmations still to send).
   *
   * @param status status
   * @param businessDate last date
   * @return runs
   */
  List<EodRun> findByStatusAndBusinessDateLessThanEqualOrderByIdAsc(
      EodStatus status, LocalDate businessDate);

  /**
   * Runs up to a date, oldest first.
   *
   * @param businessDate last date
   * @return runs
   */
  List<EodRun> findByBusinessDateLessThanEqualOrderByIdAsc(LocalDate businessDate);
}
