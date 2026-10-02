package com.iortatechnxt.brokerverse.underwriting.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Endorsement}. */
public interface EndorsementRepository
    extends JpaRepository<Endorsement, Long>, JpaSpecificationExecutor<Endorsement> {

  /**
   * Loads an endorsement with its policy, product and parties.
   *
   * @param id id
   * @return endorsement
   */
  @EntityGraph(
      attributePaths = {
        "policy",
        "policy.product",
        "policy.customer",
        "policy.intermediary",
        "policy.coinsurer"
      })
  Optional<Endorsement> findWithPolicyById(Long id);

  /**
   * Endorsement history of a policy.
   *
   * @param policyId policy
   * @return endorsements in number order
   */
  @EntityGraph(attributePaths = {"policy"})
  List<Endorsement> findByPolicyIdOrderByEndorsementNo(Long policyId);

  /**
   * Endorsements of several policies.
   *
   * @param policyIds policies
   * @return endorsements
   */
  List<Endorsement> findByPolicyIdIn(Collection<Long> policyIds);

  /**
   * Highest endorsement number used on a policy.
   *
   * @param policyId policy
   * @return max number, 0 when none
   */
  @Query(
      "select coalesce(max(e.endorsementNo), 0) from Endorsement e where e.policy.id = :policyId")
  int maxEndorsementNo(@Param("policyId") Long policyId);

  /**
   * Counts endorsements of a policy in given statuses.
   *
   * @param policyId policy
   * @param statuses statuses
   * @return count
   */
  long countByPolicyIdAndWorkflowStatusIn(Long policyId, Collection<PolicyStatus> statuses);

  @Override
  @EntityGraph(
      attributePaths = {
        "policy",
        "policy.product",
        "policy.customer",
        "policy.intermediary",
        "policy.coinsurer",
        "policy.openCover"
      })
  List<Endorsement> findAll(Specification<Endorsement> spec);
}
