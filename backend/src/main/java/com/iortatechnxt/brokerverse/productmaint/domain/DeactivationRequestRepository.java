package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Package deactivation requests. */
public interface DeactivationRequestRepository
    extends JpaRepository<DeactivationRequest, Long>,
        JpaSpecificationExecutor<DeactivationRequest> {

  /**
   * Requests of a package in the given statuses.
   *
   * @param productCode package
   * @param statuses statuses
   * @return requests, newest first
   */
  List<DeactivationRequest> findByProductCodeAndStatusInOrderByIdDesc(
      String productCode, Collection<DeactivationStatus> statuses);

  /**
   * Number of requests of a company in a status (dashboard).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, DeactivationStatus status);
}
