package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Sales organisation units. */
public interface SalesUnitRepository extends JpaRepository<SalesUnit, Long> {

  /**
   * Units of a company.
   *
   * @param companyId company
   * @return units
   */
  List<SalesUnit> findByCompanyIdOrderByLevelAscCodeAsc(Long companyId);

  /**
   * One unit.
   *
   * @param companyId company
   * @param code code
   * @return unit
   */
  Optional<SalesUnit> findByCompanyIdAndCode(Long companyId, String code);
}
