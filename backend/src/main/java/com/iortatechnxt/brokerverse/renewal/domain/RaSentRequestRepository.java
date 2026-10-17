package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Corrections of the Renewal Advices already sent. */
public interface RaSentRequestRepository extends JpaRepository<RaSentRequest, Long> {

  /**
   * Corrections of a company in a status.
   *
   * @param companyId company
   * @param status status
   * @return corrections, oldest first
   */
  List<RaSentRequest> findByCompanyIdAndStatusOrderByIdAsc(Long companyId, ApprovalStatus status);

  /**
   * Corrections in a status of every company.
   *
   * @param status status
   * @return corrections
   */
  List<RaSentRequest> findByStatusOrderByIdAsc(ApprovalStatus status);
}
