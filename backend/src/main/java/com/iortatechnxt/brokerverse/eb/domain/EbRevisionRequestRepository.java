package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Revision requests. */
public interface EbRevisionRequestRepository extends JpaRepository<EbRevisionRequest, Long> {

  /**
   * Revision requests of a cycle.
   *
   * @param cycleId cycle
   * @return requests, oldest first
   */
  List<EbRevisionRequest> findByCycleIdOrderByRevisionNoAsc(Long cycleId);
}
