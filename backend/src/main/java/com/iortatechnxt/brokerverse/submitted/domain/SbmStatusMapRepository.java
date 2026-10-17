package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy status map. */
public interface SbmStatusMapRepository extends JpaRepository<SbmStatusMap, Long> {

  /**
   * A mapping by legacy status.
   *
   * @param legacyStatus legacy status (upper case)
   * @return mapping
   */
  Optional<SbmStatusMap> findByLegacyStatus(String legacyStatus);

  /**
   * Every mapping.
   *
   * @return mappings
   */
  List<SbmStatusMap> findAllByOrderByLegacyStatusAsc();
}
