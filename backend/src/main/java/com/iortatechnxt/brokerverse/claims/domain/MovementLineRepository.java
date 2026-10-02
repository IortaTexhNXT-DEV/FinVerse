package com.iortatechnxt.brokerverse.claims.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for the claim movement ledger ({@link MovementLine}). */
public interface MovementLineRepository extends JpaRepository<MovementLine, Long> {

  /**
   * Movement history of a claim.
   *
   * @param claimId claim
   * @return lines in date order
   */
  List<MovementLine> findByClaimIdOrderByMovementDateAscIdAsc(Long claimId);

  /**
   * Lines of a company dated in a range, with their claim.
   *
   * @param companyId company
   * @param from first date (inclusive)
   * @param to last date (inclusive)
   * @return lines in date order
   */
  @EntityGraph(attributePaths = {"claim"})
  List<MovementLine> findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Totals per claim, kind, side and cost type of the lines dated in a range (report and view
   * aggregates; one row per group, company share).
   *
   * @param companyId company
   * @param from first date (inclusive)
   * @param to last date (inclusive)
   * @return totals
   */
  @Query(
      """
      select new com.iortatechnxt.brokerverse.claims.domain.MovementTotal(
          l.claim.id, l.kind, l.side, l.costType, sum(l.amount), sum(l.baseAmount))
      from MovementLine l
      where l.companyId = :companyId and l.movementDate between :from and :to
      group by l.claim.id, l.kind, l.side, l.costType
      """)
  List<MovementTotal> totals(
      @Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  /**
   * Whether a movement reference was already recorded.
   *
   * @param reference reference
   * @return true when present
   */
  boolean existsByReference(String reference);
}
