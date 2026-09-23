package com.iortatechnxt.finverse.reserves.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ReserveParameter}. */
public interface ReserveParameterRepository extends JpaRepository<ReserveParameter, Long> {

  /**
   * Lists a company's parameter sets.
   *
   * @param companyId company
   * @return parameter sets by line of business, latest effective date first
   */
  List<ReserveParameter> findByCompanyIdOrderByBusinessLineAscEffectiveFromDesc(Long companyId);

  /**
   * Checks whether a line of business already has a parameter set from a date.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param effectiveFrom effective date
   * @return true when one exists
   */
  boolean existsByCompanyIdAndBusinessLineAndEffectiveFrom(
      Long companyId, String businessLine, LocalDate effectiveFrom);
}
