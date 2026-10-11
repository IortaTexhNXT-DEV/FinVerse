package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal.Status;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payment reversals requested by other modules (ACSL 2.6.0-2.6.1). */
public interface PaymentReversalRepository extends JpaRepository<PaymentReversal, Long> {

  /**
   * The request of a source (idempotency of the port).
   *
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @return request
   */
  Optional<PaymentReversal> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);

  /**
   * Requests of a company in some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return requests
   */
  Page<PaymentReversal> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<Status> statuses, Pageable pageable);

  /**
   * Count per status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, Status status);
}
