package com.iortatechnxt.brokerverse.brokerclaims.location.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Locations linked to claims (BRCLM.037). */
public interface ClaimLocationRepository extends JpaRepository<ClaimLocation, Long> {

  /**
   * The locations of a claim by item number.
   *
   * @param claimId claim
   * @return locations
   */
  List<ClaimLocation> findByClaimIdOrderByAccountItemNoAsc(Long claimId);

  /**
   * The locations of several claims.
   *
   * @param claimIds claims
   * @return locations
   */
  List<ClaimLocation> findByClaimIdIn(Collection<Long> claimIds);

  /**
   * One location of a claim.
   *
   * @param claimId claim
   * @param itemNo item number
   * @return the location when linked
   */
  Optional<ClaimLocation> findByClaimIdAndAccountItemNo(Long claimId, int itemNo);
}
