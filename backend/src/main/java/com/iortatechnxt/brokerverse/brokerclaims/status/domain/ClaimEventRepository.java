package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Field-change timeline of the claims ({@code bcl_claim_event}). */
public interface ClaimEventRepository extends JpaRepository<ClaimEvent, Long> {

  /**
   * The changes of a claim, oldest first.
   *
   * @param claimId claim
   * @return changes
   */
  List<ClaimEvent> findByClaimIdOrderByChangedAtAscIdAsc(Long claimId);
}
