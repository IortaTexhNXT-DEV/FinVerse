package com.iortatechnxt.brokerverse.brokerclaims.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  // ---------- Wave CL1-A (cover, premium check, feed, client records, retention) ----------

  /**
   * Every claim of a cover, all policy years, newest first (Cover Lookup, BRCLM.002/003).
   *
   * @param companyId company
   * @param arn account reference number
   * @return claims
   */
  List<Claim> findByCompanyIdAndCoverArnOrderByIdDesc(Long companyId, String arn);

  /**
   * Claims of a cover not in a phase, e.g. the open claims of an ARN whose premium is re-checked
   * when a payment is applied (BRCLM.001) or that are told about a newer cover version (039).
   *
   * @param companyId company
   * @param arn account reference number
   * @param phase phase to exclude
   * @return claims
   */
  List<Claim> findByCompanyIdAndCoverArnAndProgressPhaseNot(
      Long companyId, String arn, ClaimPhase phase);

  /**
   * Open claims without an authorization code, oldest first (daily premium re-check, BRCLM.001).
   *
   * @param phase phase to exclude (CLOSED)
   * @param pageable batch
   * @return claims
   */
  List<Claim> findByProgressPhaseNotAndCoverAuthorizationCodeIsNullOrderByIdAsc(
      ClaimPhase phase, Pageable pageable);

  /**
   * Open claims in one of the statuses changed on or after a time (claims special remittance feed,
   * OQ46).
   *
   * @param companyId company
   * @param statuses status codes
   * @param phase phase to exclude (CLOSED)
   * @param since first time
   * @return claims
   */
  List<Claim>
      findByCompanyIdAndProgressStatusCodeInAndProgressPhaseNotAndProgressStatusSinceGreaterThanEqual(
          Long companyId, Collection<String> statuses, ClaimPhase phase, Instant since);

  /**
   * Claims of a client, newest first (client 360 view, BRCLM.040).
   *
   * @param companyId company
   * @param clientCode client code of the cover
   * @return claims
   */
  List<Claim> findByCompanyIdAndCoverClientCodeOrderByIdDesc(Long companyId, String clientCode);

  /**
   * Claims by number, ARN, policy number or assured fragment, newest first (search of a cover's
   * claims and the claim pickers).
   *
   * @param companyId company
   * @param text lower-case text with wildcards
   * @param pageable page
   * @return claims
   */
  @Query(
      "select c from BrokerClaim c where c.companyId = :companyId and (lower(c.claimNo) like :text"
          + " or lower(c.cover.arn) like :text or lower(c.cover.policyNo) like :text"
          + " or lower(c.cover.assuredName) like :text) order by c.id desc")
  List<Claim> search(
      @Param("companyId") Long companyId, @Param("text") String text, Pageable pageable);

  /**
   * Number of claims in the phases whose last change is on or before a time (retention review, NFR
   * p.41).
   *
   * @param phases phases
   * @param cutoff last activity on or before
   * @return count
   */
  @Query(
      "select count(c) from BrokerClaim c where c.progress.phase in :phases"
          + " and coalesce(c.updatedAt, c.createdAt) <= :cutoff")
  long countRetention(
      @Param("phases") Collection<ClaimPhase> phases, @Param("cutoff") Instant cutoff);

  /**
   * Claims in the phases whose last change is on or before a time, oldest activity first.
   *
   * @param phases phases
   * @param cutoff last activity on or before
   * @param pageable limit
   * @return claims
   */
  @Query(
      "select c from BrokerClaim c where c.progress.phase in :phases"
          + " and coalesce(c.updatedAt, c.createdAt) <= :cutoff"
          + " order by coalesce(c.updatedAt, c.createdAt), c.id")
  List<Claim> retention(
      @Param("phases") Collection<ClaimPhase> phases,
      @Param("cutoff") Instant cutoff,
      Pageable pageable);
}
