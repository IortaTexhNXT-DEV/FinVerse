package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payee maintenance requests (DIS 2.2.1). */
public interface PayeeRequestRepository extends JpaRepository<PayeeRequest, Long> {

  /**
   * Requests of a company with a status, newest first.
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return requests
   */
  Page<PayeeRequest> findByCompanyIdAndStatusOrderByIdDesc(
      Long companyId, PayeeRequestStatus status, Pageable pageable);

  /**
   * Open requests for a party code.
   *
   * @param companyId company
   * @param payeeCode party code
   * @param status status
   * @return requests
   */
  List<PayeeRequest> findByCompanyIdAndPayeeCodeAndStatus(
      Long companyId, String payeeCode, PayeeRequestStatus status);

  /**
   * The request raised for a source reference (idempotency of the no-match fall-out).
   *
   * @param companyId company
   * @param sourceRef source reference
   * @param status status
   * @return request
   */
  Optional<PayeeRequest> findFirstByCompanyIdAndSourceRefAndStatus(
      Long companyId, String sourceRef, PayeeRequestStatus status);

  /**
   * Count of requests with a status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, PayeeRequestStatus status);
}
