package com.iortatechnxt.brokerverse.issuance.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Received e-policies. */
public interface EpolicyRepository extends JpaRepository<Epolicy, Long> {

  /**
   * E-policies of an account, newest first.
   *
   * @param arn Account Reference Number
   * @return e-policies
   */
  List<Epolicy> findByArnOrderByIdDesc(String arn);

  /**
   * E-policies of a company in some statuses whose ARN contains a text, oldest first (review
   * queue).
   *
   * @param companyId company
   * @param statuses statuses
   * @param arn ARN fragment ("" for all)
   * @param pageable page
   * @return e-policies
   */
  Page<Epolicy> findByCompanyIdAndStatusInAndArnContainingIgnoreCaseOrderByIdAsc(
      Long companyId, Collection<EpolicyStatus> statuses, String arn, Pageable pageable);

  /**
   * Confirmed e-policies of a company sent a given number of times (0 = not yet sent), oldest
   * first.
   *
   * @param companyId company
   * @param status confirmed status
   * @param dispatchCount number of sends
   * @param arn ARN fragment ("" for all)
   * @param pageable page
   * @return e-policies
   */
  Page<Epolicy> findByCompanyIdAndStatusAndDispatchCountAndArnContainingIgnoreCaseOrderByIdAsc(
      Long companyId, EpolicyStatus status, int dispatchCount, String arn, Pageable pageable);

  /**
   * Number of e-policies of a company in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndStatusIn(Long companyId, Collection<EpolicyStatus> statuses);

  /**
   * Number of confirmed e-policies sent a given number of times.
   *
   * @param companyId company
   * @param status confirmed status
   * @param dispatchCount number of sends
   * @return count
   */
  long countByCompanyIdAndStatusAndDispatchCount(
      Long companyId, EpolicyStatus status, int dispatchCount);
}
