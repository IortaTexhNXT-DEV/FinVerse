package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Disbursement queue requests. */
public interface DisbursementRequestRepository extends JpaRepository<DisbursementRequest, Long> {

  /**
   * The request of a source transaction.
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return request
   */
  Optional<DisbursementRequest> findBySourceModuleAndSourceRef(
      String sourceModule, String sourceRef);

  /**
   * Requests of a company with some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return requests
   */
  Page<DisbursementRequest> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, List<DisbursementRequest.Status> statuses, Pageable pageable);

  /**
   * Requests of a company with a status (Operations home).
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndStatusIn(Long companyId, List<DisbursementRequest.Status> statuses);
}
