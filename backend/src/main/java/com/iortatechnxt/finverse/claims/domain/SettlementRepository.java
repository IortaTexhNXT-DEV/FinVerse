package com.iortatechnxt.finverse.claims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Settlement}. */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  /**
   * Loads a settlement with its claim and payee.
   *
   * @param id id
   * @return settlement
   */
  @EntityGraph(attributePaths = {"claim", "payee"})
  Optional<Settlement> findWithDetailsById(Long id);

  /**
   * Settlements of a claim.
   *
   * @param claimId claim
   * @return settlements, oldest first
   */
  @EntityGraph(attributePaths = {"payee"})
  List<Settlement> findByClaimIdOrderById(Long claimId);

  /**
   * Settlements in a status, with claim and payee (approval inbox).
   *
   * @param status status
   * @return settlements
   */
  @EntityGraph(attributePaths = {"claim", "payee"})
  List<Settlement> findByApprovalStatusOrderById(DocumentStatus status);

  /**
   * Whether a claim has a settlement in a status.
   *
   * @param claimId claim
   * @param status status
   * @return true when one exists
   */
  boolean existsByClaimIdAndApprovalStatus(Long claimId, DocumentStatus status);
}
