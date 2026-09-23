package com.iortatechnxt.finverse.claims.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Claim}. Searches use {@link ClaimSpecifications}. */
public interface ClaimRepository
    extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

  /**
   * Loads a claim with its claimant and involved parties.
   *
   * @param id id
   * @return claim
   */
  @EntityGraph(attributePaths = {"claimant", "parties", "parties.party"})
  Optional<Claim> findWithDetailsById(Long id);

  /**
   * Finds a claim by number.
   *
   * @param companyId company
   * @param claimNo claim number
   * @return claim
   */
  @EntityGraph(attributePaths = {"claimant"})
  Optional<Claim> findByCompanyIdAndClaimNo(Long companyId, String claimNo);

  @Override
  @EntityGraph(attributePaths = {"claimant"})
  Page<Claim> findAll(Specification<Claim> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"claimant"})
  List<Claim> findAll(Specification<Claim> spec);

  /**
   * Claims of a company, oldest first (reports).
   *
   * @param companyId company
   * @return claims with claimant
   */
  @EntityGraph(attributePaths = {"claimant"})
  List<Claim> findByCompanyIdOrderByClaimNo(Long companyId);

  /**
   * Claims made under policies.
   *
   * @param policyIds policies
   * @return claims
   */
  @Query("select c from Claim c where c.policy.policyId in :policyIds order by c.id")
  List<Claim> findByPolicyIds(@Param("policyIds") Collection<Long> policyIds);

  /**
   * Counts the claims of a company (demo data idempotency).
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyId(Long companyId);
}
