package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Claim movements processed by reinsurance. */
public interface RiClaimMovementRepository extends JpaRepository<RiClaimMovement, Long> {

  /**
   * Whether a movement was already processed (idempotency).
   *
   * @param companyId company
   * @param reference movement reference
   * @return true when processed
   */
  boolean existsByCompanyIdAndReference(Long companyId, String reference);

  /**
   * Company net retained loss of a claim so far (payments less recoveries after proportional
   * reinsurance), the base of the excess of loss recovery.
   *
   * @param claimId claim
   * @return net retained amount, null when none
   */
  @Query("select sum(m.netRetained) from RiClaimMovement m where m.claimId = :claimId")
  BigDecimal netRetained(@Param("claimId") Long claimId);

  /**
   * Movements in a period with their shares.
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return movements ordered by date
   */
  @EntityGraph(attributePaths = "shares")
  List<RiClaimMovement> findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Movements of some types up to a date, with their shares.
   *
   * @param companyId company
   * @param types movement types
   * @param asOf last movement date
   * @return movements
   */
  @EntityGraph(attributePaths = "shares")
  List<RiClaimMovement> findByCompanyIdAndMovementTypeInAndMovementDateLessThanEqual(
      Long companyId, Collection<ClaimMovementType> types, LocalDate asOf);
}
