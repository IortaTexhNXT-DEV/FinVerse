package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** IAAFs. */
public interface SbmIaafRepository extends JpaRepository<SbmIaaf, Long> {

  /**
   * The IAAF of a policy.
   *
   * @param policyId policy
   * @return IAAF
   */
  Optional<SbmIaaf> findByPolicyId(Long policyId);

  /**
   * IAAFs in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return IAAFs, newest first
   */
  Page<SbmIaaf> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<SbmDocStatus> statuses, Pageable pageable);

  /**
   * IAAFs of every company in a status (approval inbox, SLA).
   *
   * @param status status
   * @return IAAFs
   */
  List<SbmIaaf> findByStatus(SbmDocStatus status);
}
