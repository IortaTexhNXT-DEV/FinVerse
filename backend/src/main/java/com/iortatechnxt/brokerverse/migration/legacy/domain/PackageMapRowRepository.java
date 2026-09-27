package com.iortatechnxt.brokerverse.migration.legacy.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package map entries of the legacy packages (object R06). */
public interface PackageMapRowRepository extends JpaRepository<PackageMapRow, Long> {

  /**
   * Live entries of a company.
   *
   * @param companyId company
   * @return entries
   */
  List<PackageMapRow> findByCompanyIdAndRolledBackFalseOrderByIdAsc(Long companyId);
}
