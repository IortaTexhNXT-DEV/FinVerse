package com.iortatechnxt.brokerverse.brokerclaims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Claims of the broking claims module (BRCLM.041/043). Every read is scoped to the company, so a
 * claim reference of another company is never found (NFR access control, FR-CL-002 R3). The build
 * waves add their own finders here. Named after the entity name {@code BrokerClaim}: the
 * insurer-side {@code claims} module owns the bean {@code claimRepository}.
 */
public interface BrokerClaimRepository extends JpaRepository<Claim, Long> {

  /**
   * One claim of a company.
   *
   * @param id claim id
   * @param companyId company
   * @return the claim when it belongs to the company
   */
  Optional<Claim> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * One claim by its number.
   *
   * @param companyId company
   * @param claimNo claim number
   * @return the claim
   */
  Optional<Claim> findByCompanyIdAndClaimNo(Long companyId, String claimNo);

  /**
   * Claims of a cover and policy year, newest first (claims of the cover, loss experience).
   *
   * @param companyId company
   * @param arn account reference number
   * @param policyYear policy year
   * @return claims
   */
  List<Claim> findByCompanyIdAndCoverArnAndCoverPolicyYearOrderByIdDesc(
      Long companyId, String arn, int policyYear);
}
