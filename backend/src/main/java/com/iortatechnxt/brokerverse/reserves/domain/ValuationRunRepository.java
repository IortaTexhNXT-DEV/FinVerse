package com.iortatechnxt.brokerverse.reserves.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ValuationRun}. */
public interface ValuationRunRepository extends JpaRepository<ValuationRun, Long> {

  /**
   * Runs of a company, latest valuation date first.
   *
   * @param companyId company
   * @return runs
   */
  List<ValuationRun> findByCompanyIdOrderByValuationDateDescIdDesc(Long companyId);

  /**
   * Gets a run with its lines.
   *
   * @param id id
   * @return run
   */
  @EntityGraph(type = EntityGraphType.LOAD, attributePaths = "lines")
  Optional<ValuationRun> findWithLinesById(Long id);

  /**
   * The live (not cancelled) run of a valuation date.
   *
   * @param companyId company
   * @param valuationDate valuation date
   * @param status excluded status (CANCELLED)
   * @return run if any
   */
  Optional<ValuationRun> findFirstByCompanyIdAndValuationDateAndStatusNot(
      Long companyId, LocalDate valuationDate, RunStatus status);

  /**
   * Latest run in a status before a date (previous posted valuation).
   *
   * @param companyId company
   * @param status status
   * @param before valuation date limit (exclusive)
   * @return run if any
   */
  @EntityGraph(type = EntityGraphType.LOAD, attributePaths = "lines")
  Optional<ValuationRun>
      findFirstByCompanyIdAndStatusAndValuationDateBeforeOrderByValuationDateDesc(
          Long companyId, RunStatus status, LocalDate before);

  /**
   * Latest run in a status on or before a date.
   *
   * @param companyId company
   * @param status status
   * @param asOf valuation date limit (inclusive)
   * @return run if any
   */
  @EntityGraph(type = EntityGraphType.LOAD, attributePaths = "lines")
  Optional<ValuationRun>
      findFirstByCompanyIdAndStatusAndValuationDateLessThanEqualOrderByValuationDateDesc(
          Long companyId, RunStatus status, LocalDate asOf);

  /**
   * Whether a run in a status exists after a date (a later valuation already posted).
   *
   * @param companyId company
   * @param status status
   * @param after valuation date (exclusive)
   * @return true when one exists
   */
  boolean existsByCompanyIdAndStatusAndValuationDateAfter(
      Long companyId, RunStatus status, LocalDate after);

  /**
   * Runs in a status (approval inbox).
   *
   * @param status status
   * @return runs
   */
  List<ValuationRun> findByStatus(RunStatus status);
}
