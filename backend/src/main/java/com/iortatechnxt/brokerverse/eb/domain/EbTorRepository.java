package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** TOR versions. */
public interface EbTorRepository extends JpaRepository<EbTor, Long> {

  /**
   * Versions of a cycle.
   *
   * @param cycleId cycle
   * @return versions, latest first
   */
  List<EbTor> findByCycleIdOrderByVersionNoDesc(Long cycleId);

  /**
   * The version of a cycle in a status.
   *
   * @param cycleId cycle
   * @param status status
   * @return the latest such version
   */
  Optional<EbTor> findFirstByCycleIdAndStatusOrderByVersionNoDesc(
      Long cycleId, EbTor.Status status);
}
