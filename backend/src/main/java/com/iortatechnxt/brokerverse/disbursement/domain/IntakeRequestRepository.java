package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Payment requests received by Disbursement (DIS 2.4-2.6, 3.25). */
public interface IntakeRequestRepository
    extends JpaRepository<IntakeRequest, Long>, JpaSpecificationExecutor<IntakeRequest> {

  /**
   * The request of a source transaction (idempotency).
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return request
   */
  Optional<IntakeRequest> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);

  /**
   * The request of an Operations gateway request.
   *
   * @param gatewayRequestId Operations request id
   * @return request
   */
  Optional<IntakeRequest> findFirstByGatewayRequestId(Long gatewayRequestId);

  /**
   * Requests waiting for a payee code.
   *
   * @param companyId company
   * @param payeeCode party code
   * @param status status
   * @return requests, oldest first
   */
  List<IntakeRequest> findByCompanyIdAndPayeeCodeAndStatusOrderByIdAsc(
      Long companyId, String payeeCode, RequestStatus status);

  /**
   * Requests of a company in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndStatusIn(Long companyId, Collection<RequestStatus> statuses);
}
