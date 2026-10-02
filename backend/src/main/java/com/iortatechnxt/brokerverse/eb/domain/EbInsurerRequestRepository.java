package com.iortatechnxt.brokerverse.eb.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer requests for proposal. */
public interface EbInsurerRequestRepository extends JpaRepository<EbInsurerRequest, Long> {

  /**
   * Requests of a cycle.
   *
   * @param cycleId cycle
   * @return requests, oldest first
   */
  List<EbInsurerRequest> findByCycleIdOrderByIdAsc(Long cycleId);

  /**
   * The open request of an insurer on a cycle.
   *
   * @param cycleId cycle
   * @param insurerCode insurer
   * @param status OPEN
   * @return request
   */
  Optional<EbInsurerRequest> findFirstByCycleIdAndInsurerCodeAndStatus(
      Long cycleId, String insurerCode, EbInsurerRequest.Status status);

  /**
   * Requests of a cycle in a status.
   *
   * @param cycleId cycle
   * @param status status
   * @return requests
   */
  List<EbInsurerRequest> findByCycleIdAndStatus(Long cycleId, EbInsurerRequest.Status status);

  /**
   * Requests in a status past their due date (overdue alert).
   *
   * @param status OPEN
   * @param date business date
   * @return requests
   */
  List<EbInsurerRequest> findByStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
      EbInsurerRequest.Status status, LocalDate date);

  /**
   * Requests of a company in a status (EB Home).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, EbInsurerRequest.Status status);
}
