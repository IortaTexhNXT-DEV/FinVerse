package com.iortatechnxt.finverse.claims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link ReserveChange}. */
public interface ReserveChangeRepository extends JpaRepository<ReserveChange, Long> {

  /**
   * Loads a reserve change with its claim.
   *
   * @param id id
   * @return reserve change
   */
  @EntityGraph(attributePaths = {"claim"})
  Optional<ReserveChange> findWithClaimById(Long id);

  /**
   * Reserve changes of a claim.
   *
   * @param claimId claim
   * @return changes in sequence
   */
  List<ReserveChange> findByClaimIdOrderByChangeNo(Long claimId);

  /**
   * Reserve changes in a status, with their claim (approval inbox).
   *
   * @param status status
   * @return changes
   */
  @EntityGraph(attributePaths = {"claim"})
  List<ReserveChange> findByApprovalStatusOrderById(DocumentStatus status);

  /**
   * Whether a claim has a reserve change in a status for a side and cost type.
   *
   * @param claimId claim
   * @param side side
   * @param costType cost type
   * @param status status
   * @return true when one exists
   */
  boolean existsByClaimIdAndSideAndCostTypeAndApprovalStatus(
      Long claimId, EstimateSide side, CostType costType, DocumentStatus status);

  /**
   * Whether a claim has a reserve change in a status.
   *
   * @param claimId claim
   * @param status status
   * @return true when one exists
   */
  boolean existsByClaimIdAndApprovalStatus(Long claimId, DocumentStatus status);

  /**
   * Highest change number of a claim.
   *
   * @param claimId claim
   * @return last number, 0 when none
   */
  @Query("select coalesce(max(r.changeNo), 0) from ReserveChange r where r.claim.id = :claimId")
  int lastChangeNo(@Param("claimId") Long claimId);
}
