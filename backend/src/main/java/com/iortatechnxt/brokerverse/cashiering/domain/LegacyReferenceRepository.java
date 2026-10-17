package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy numbers of the migrated accounts. */
public interface LegacyReferenceRepository extends JpaRepository<LegacyReference, Long> {

  /**
   * The accounts a legacy number stands for, in the priority of the types.
   *
   * @param companyId company
   * @param refNo legacy number, any case
   * @return references
   */
  List<LegacyReference> findByCompanyIdAndRefNoIgnoreCaseOrderByIdAsc(Long companyId, String refNo);
}
