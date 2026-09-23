package com.iortatechnxt.finverse.claims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Recovery}. */
public interface RecoveryRepository extends JpaRepository<Recovery, Long> {

  /**
   * Loads a recovery with its claim and payer.
   *
   * @param id id
   * @return recovery
   */
  @EntityGraph(attributePaths = {"claim", "fromParty"})
  Optional<Recovery> findWithDetailsById(Long id);

  /**
   * Recoveries of a claim.
   *
   * @param claimId claim
   * @return recoveries, oldest first
   */
  @EntityGraph(attributePaths = {"fromParty"})
  List<Recovery> findByClaimIdOrderById(Long claimId);

  /**
   * Recoveries in a status, with their claim (approval inbox).
   *
   * @param status status
   * @return recoveries
   */
  @EntityGraph(attributePaths = {"claim"})
  List<Recovery> findByApprovalStatusOrderById(DocumentStatus status);

  /**
   * Whether a claim has a recovery in a status.
   *
   * @param claimId claim
   * @param status status
   * @return true when one exists
   */
  boolean existsByClaimIdAndApprovalStatus(Long claimId, DocumentStatus status);
}
