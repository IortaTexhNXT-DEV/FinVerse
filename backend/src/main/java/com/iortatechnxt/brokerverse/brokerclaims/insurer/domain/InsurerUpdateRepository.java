package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer updates (BRCLM.041); insert-only. */
public interface InsurerUpdateRepository extends JpaRepository<InsurerUpdate, Long> {

  /**
   * The timeline of a claim's insurer updates, newest first.
   *
   * @param claimId claim
   * @return updates
   */
  List<InsurerUpdate> findByClaimIdOrderByUpdateDateDescIdDesc(Long claimId);

  /**
   * One update of a claim.
   *
   * @param id update
   * @param claimId claim
   * @return the update when it belongs to the claim
   */
  Optional<InsurerUpdate> findByIdAndClaimId(Long id, Long claimId);
}
