package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client confirmations. */
public interface EbClientConfirmationRepository extends JpaRepository<EbClientConfirmation, Long> {

  /**
   * The confirmation of a cycle in a status.
   *
   * @param cycleId cycle
   * @param status ACTIVE or VOIDED
   * @return confirmation
   */
  Optional<EbClientConfirmation> findFirstByCycleIdAndStatusOrderByIdDesc(
      Long cycleId, String status);

  /**
   * Confirmations of a cycle.
   *
   * @param cycleId cycle
   * @return confirmations, latest first
   */
  List<EbClientConfirmation> findByCycleIdOrderByIdDesc(Long cycleId);
}
