package com.iortatechnxt.brokerverse.eb.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Franchise requests. */
public interface EbFranchiseRequestRepository extends JpaRepository<EbFranchiseRequest, Long> {

  /**
   * Requests of a cycle.
   *
   * @param cycleId cycle
   * @return requests, oldest first
   */
  List<EbFranchiseRequest> findByCycleIdOrderByIdAsc(Long cycleId);

  /**
   * Requests of a programme.
   *
   * @param programmeId programme
   * @return requests, latest first
   */
  List<EbFranchiseRequest> findByProgrammeIdOrderByIdDesc(Long programmeId);

  /**
   * Requests in a status past their decision date (expiry, overdue alert).
   *
   * @param status SUBMITTED
   * @param date business date
   * @return requests
   */
  List<EbFranchiseRequest> findByStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
      EbFranchiseRequest.Status status, LocalDate date);

  /**
   * Decided requests whose client advice is past due.
   *
   * @param statuses APPROVED and REJECTED
   * @param date business date
   * @return requests
   */
  List<EbFranchiseRequest> findByStatusInAndAdviceDueDateBeforeOrderByAdviceDueDateAscIdAsc(
      Collection<EbFranchiseRequest.Status> statuses, LocalDate date);

  /**
   * Requests of a company in a status (EB Home).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, EbFranchiseRequest.Status status);
}
