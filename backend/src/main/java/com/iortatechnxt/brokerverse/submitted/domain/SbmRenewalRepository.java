package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Renewal hand-offs. */
public interface SbmRenewalRepository extends JpaRepository<SbmRenewal, Long> {

  /**
   * The hand-off of a record.
   *
   * @param policyId record
   * @return hand-off
   */
  Optional<SbmRenewal> findByPolicyId(Long policyId);

  /**
   * Hand-offs of a renewal account.
   *
   * @param arn account
   * @return hand-offs
   */
  List<SbmRenewal> findByArn(String arn);

  /**
   * Hand-offs of a company in some statuses and outcome.
   *
   * @param companyId company
   * @param statuses hand-off statuses
   * @param outcome outcome
   * @return hand-offs
   */
  List<SbmRenewal> findByCompanyIdAndHandoffStatusInAndOutcome(
      Long companyId, Collection<String> statuses, String outcome);

  /**
   * The hand-offs of a company, newest first (renewal work list).
   *
   * @param companyId company
   * @param pageable page
   * @return hand-offs
   */
  org.springframework.data.domain.Page<SbmRenewal> findByCompanyIdOrderByIdDesc(
      Long companyId, org.springframework.data.domain.Pageable pageable);

  /**
   * The hand-offs of a company in hand-off statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return hand-offs
   */
  org.springframework.data.domain.Page<SbmRenewal> findByCompanyIdAndHandoffStatusInOrderByIdDesc(
      Long companyId,
      Collection<String> statuses,
      org.springframework.data.domain.Pageable pageable);

  /**
   * Count of a company in a hand-off status (home).
   *
   * @param companyId company
   * @param handoffStatus status
   * @return count
   */
  long countByCompanyIdAndHandoffStatus(Long companyId, String handoffStatus);
}
