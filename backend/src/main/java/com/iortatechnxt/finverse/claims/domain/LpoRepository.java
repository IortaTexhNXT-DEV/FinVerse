package com.iortatechnxt.finverse.claims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Lpo}. */
public interface LpoRepository extends JpaRepository<Lpo, Long> {

  /**
   * Loads an LPO with its claim and garage.
   *
   * @param id id
   * @return LPO
   */
  @EntityGraph(attributePaths = {"claim", "garage"})
  Optional<Lpo> findWithDetailsById(Long id);

  /**
   * LPOs of a claim.
   *
   * @param claimId claim
   * @return LPOs with claim and garage, oldest first
   */
  @EntityGraph(attributePaths = {"claim", "garage"})
  List<Lpo> findByClaimIdOrderById(Long claimId);

  /**
   * LPOs of a company with claim and garage (LPO register and report).
   *
   * @param companyId company
   * @return LPOs, newest first
   */
  @Query(
      """
      select l from Lpo l join fetch l.claim c join fetch l.garage
      where c.companyId = :companyId order by l.id desc
      """)
  List<Lpo> findByCompany(@Param("companyId") Long companyId);
}
