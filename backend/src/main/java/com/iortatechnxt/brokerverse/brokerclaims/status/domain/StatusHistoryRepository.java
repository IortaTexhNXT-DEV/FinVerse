package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Status history of the claims ({@code bcl_status_history}, BRCLM.011/027). */
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

  /**
   * The status changes of a claim, oldest first.
   *
   * @param claimId claim
   * @return changes
   */
  List<StatusHistory> findByClaimIdOrderByChangedAtAscIdAsc(Long claimId);
}
