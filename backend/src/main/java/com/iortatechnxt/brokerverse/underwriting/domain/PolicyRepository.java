package com.iortatechnxt.brokerverse.underwriting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * Persistence for {@link Policy}. Searches use specifications (see {@code PolicySpecifications}).
 */
public interface PolicyRepository
    extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

  /**
   * Loads a policy with everything its detail view shows.
   *
   * @param id id
   * @return policy
   */
  @EntityGraph(
      attributePaths = {"product", "customer", "intermediary", "coinsurer", "openCover", "risks"})
  Optional<Policy> findWithDetailsById(Long id);

  /**
   * Finds a policy by number with its parties and product.
   *
   * @param companyId company
   * @param policyNo policy number
   * @return policy
   */
  @EntityGraph(
      attributePaths = {"product", "customer", "intermediary", "coinsurer", "openCover", "risks"})
  Optional<Policy> findByCompanyIdAndPolicyNo(Long companyId, String policyNo);

  @Override
  @EntityGraph(attributePaths = {"product", "customer", "intermediary", "coinsurer", "openCover"})
  Page<Policy> findAll(Specification<Policy> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"product", "customer", "intermediary", "coinsurer", "openCover"})
  List<Policy> findAll(Specification<Policy> spec);

  /**
   * Certificates declared under an open cover.
   *
   * @param openCoverId open cover
   * @return certificates with risks, oldest first
   */
  @EntityGraph(
      attributePaths = {"product", "customer", "intermediary", "coinsurer", "openCover", "risks"})
  List<Policy> findByOpenCoverIdOrderById(Long openCoverId);

  /**
   * Counts policies of a company (seed data idempotency).
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyId(Long companyId);

  /**
   * Risks in force on a date (approved, or cancelled after the date) for accumulation control.
   *
   * @param companyId company
   * @param asOf date
   * @param statuses statuses to consider
   * @return risks with their policy and product loaded
   */
  @Query(
      """
      select r from PolicyRisk r join fetch r.policy p join fetch p.product
      where p.companyId = :companyId and p.workflow.status in :statuses
        and p.periodFrom <= :asOf and p.periodTo >= :asOf
      order by r.accumulationZone, p.policyNo, r.lineNo
      """)
  List<PolicyRisk> findRisksCovering(
      @Param("companyId") Long companyId,
      @Param("asOf") LocalDate asOf,
      @Param("statuses") Collection<PolicyStatus> statuses);

  /**
   * Sum insured already declared under an open cover by live certificates.
   *
   * @param openCoverId open cover
   * @param statuses live statuses
   * @return total sum insured at 100 %
   */
  @Query(
      """
      select coalesce(sum(p.premium.sumInsured), 0) from Policy p
      where p.openCover.id = :openCoverId and p.workflow.status in :statuses
      """)
  BigDecimal declaredSumInsured(
      @Param("openCoverId") Long openCoverId, @Param("statuses") Collection<PolicyStatus> statuses);
}
