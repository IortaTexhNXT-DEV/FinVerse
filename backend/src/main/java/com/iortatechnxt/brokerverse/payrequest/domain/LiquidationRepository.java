package com.iortatechnxt.brokerverse.payrequest.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cash-advance liquidations (Appendix D, AQ18). */
public interface LiquidationRepository extends JpaRepository<Liquidation, Long> {

  /**
   * The liquidation of a cash advance, with its lines.
   *
   * @param requestId cash-advance request
   * @return liquidation
   */
  @EntityGraph(attributePaths = "lines")
  Optional<Liquidation> findByRequestId(Long requestId);
}
