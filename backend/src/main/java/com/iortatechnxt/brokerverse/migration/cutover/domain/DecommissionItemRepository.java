package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Decommissioning checklist items. */
public interface DecommissionItemRepository extends JpaRepository<DecommissionItem, Long> {

  /**
   * Items of a company.
   *
   * @param companyId company
   * @return items by system, milestone and id
   */
  List<DecommissionItem> findByCompanyIdOrderBySystemCodeAscMilestoneAscIdAsc(Long companyId);

  /**
   * Whether a system already has a checklist.
   *
   * @param companyId company
   * @param systemCode system
   * @return true when present
   */
  boolean existsByCompanyIdAndSystemCode(Long companyId, String systemCode);
}
