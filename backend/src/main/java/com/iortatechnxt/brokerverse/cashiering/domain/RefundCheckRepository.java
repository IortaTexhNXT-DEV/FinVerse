package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck.Status;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Refund validations answered by Cashiering (MKT 1.11.0). */
public interface RefundCheckRepository extends JpaRepository<RefundCheck, Long> {

  /**
   * The validation of a source (idempotency of the port).
   *
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @return validation
   */
  Optional<RefundCheck> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);

  /**
   * Validations of a company in some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return validations
   */
  Page<RefundCheck> findByCompanyIdAndStatusInOrderByIdDesc(
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
