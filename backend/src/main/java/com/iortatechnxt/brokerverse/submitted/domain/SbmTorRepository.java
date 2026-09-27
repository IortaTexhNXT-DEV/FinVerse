package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Terms of Reference. */
public interface SbmTorRepository extends JpaRepository<SbmTor, Long> {

  /**
   * TORs of a policy, newest first.
   *
   * @param policyId policy
   * @return TORs
   */
  List<SbmTor> findByPolicyIdOrderByIdDesc(Long policyId);

  /**
   * TORs in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return TORs, newest first
   */
  Page<SbmTor> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<SbmDocStatus> statuses, Pageable pageable);

  /**
   * TORs of every company in a status (approval inbox, SLA).
   *
   * @param status status
   * @return TORs
   */
  List<SbmTor> findByStatus(SbmDocStatus status);
}
