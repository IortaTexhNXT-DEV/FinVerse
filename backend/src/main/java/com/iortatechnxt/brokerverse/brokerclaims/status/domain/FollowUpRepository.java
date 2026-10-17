package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Follow-ups of the claims. */
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

  /**
   * Follow-ups of a claim, newest first.
   *
   * @param claimId claim
   * @return follow-ups
   */
  List<FollowUp> findByClaimIdOrderByIdDesc(Long claimId);
}
