package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** The claims handler register ({@code bcl_handler}). */
public interface ClaimHandlerRepository extends JpaRepository<ClaimHandler, Long> {

  /**
   * One handler.
   *
   * @param username user (any case)
   * @return the handler
   */
  Optional<ClaimHandler> findByUsernameIgnoreCase(String username);

  /**
   * The register in user order.
   *
   * @return handlers
   */
  List<ClaimHandler> findAllByOrderByUsernameAsc();
}
