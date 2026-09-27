package com.iortatechnxt.brokerverse.renewal.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Renewal candidates. */
public interface RenewalCandidateRepository
    extends JpaRepository<RenewalCandidate, Long>, JpaSpecificationExecutor<RenewalCandidate> {

  /**
   * A candidate by renewal reference.
   *
   * @param companyId company
   * @param renewalRef renewal reference
   * @return candidate
   */
  Optional<RenewalCandidate> findByCompanyIdAndRenewalRef(Long companyId, String renewalRef);

  /**
   * A candidate by renewal reference in any company (upload rows carry the reference only).
   *
   * @param renewalRef renewal reference
   * @return candidates
   */
  List<RenewalCandidate> findByRenewalRef(String renewalRef);

  /**
   * The candidate of an expiring invoice (duplicate guard of the extraction, BRRN.005).
   *
   * @param companyId company
   * @param invoiceNo expiring root invoice
   * @return candidate
   */
  Optional<RenewalCandidate> findByCompanyIdAndExpiringInvoiceNo(Long companyId, String invoiceNo);

  /**
   * The candidate of a legacy or submitted policy.
   *
   * @param companyId company
   * @param source source
   * @param sourceRef legacy reference or SBM number
   * @return candidate
   */
  Optional<RenewalCandidate> findByCompanyIdAndSourceAndSourceRef(
      Long companyId, CandidateSource source, String sourceRef);

  /**
   * Candidates of an expiring ARN (invoice family events).
   *
   * @param arn expiring ARN
   * @return candidates
   */
  List<RenewalCandidate> findByExpiringArn(String arn);

  /**
   * The candidate of a renewal account.
   *
   * @param arn renewal account
   * @return candidate
   */
  Optional<RenewalCandidate> findFirstByRenewalArn(String arn);

  /**
   * Candidates of a client, newest first (client 360 view).
   *
   * @param clientId client
   * @return candidates
   */
  @Query(
      "select c from RenewalCandidate c where c.snapshot.client.clientId = :clientId order by c.id desc")
  List<RenewalCandidate> findByClient(@Param("clientId") Long clientId);

  /**
   * Candidates in given stages.
   *
   * @param companyId company
   * @param stages stages
   * @return candidates
   */
  List<RenewalCandidate> findByCompanyIdAndStageIn(Long companyId, Collection<RenewalStage> stages);

  /**
   * Candidates in given stages of every company (jobs).
   *
   * @param stages stages
   * @return candidates
   */
  List<RenewalCandidate> findByStageIn(Collection<RenewalStage> stages);

  /**
   * Open candidates expiring up to a date (jobs).
   *
   * @param stages open stages
   * @param until last expiry date
   * @return candidates, earliest expiry first
   */
  @Query(
      "select c from RenewalCandidate c where c.stage in :stages and c.snapshot.expiryDate <= :until"
          + " order by c.snapshot.expiryDate, c.id")
  List<RenewalCandidate> findOpenExpiringBy(
      @Param("stages") Collection<RenewalStage> stages, @Param("until") LocalDate until);

  /**
   * Open candidates whose PN numbers contain a PN (LAMD matching, BRRN.029).
   *
   * @param stages open stages
   * @param pattern like pattern of the PN
   * @return candidates
   */
  @Query(
      "select c from RenewalCandidate c where c.stage in :stages and c.snapshot.pnNos like :pattern")
  List<RenewalCandidate> findOpenByPn(
      @Param("stages") Collection<RenewalStage> stages, @Param("pattern") String pattern);
}
