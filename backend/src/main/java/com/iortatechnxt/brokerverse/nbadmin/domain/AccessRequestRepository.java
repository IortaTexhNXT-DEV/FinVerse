package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** User access requests. */
public interface AccessRequestRepository
    extends JpaRepository<AccessRequest, Long>, JpaSpecificationExecutor<AccessRequest> {

  /**
   * Requests in a status, oldest first (approval inbox).
   *
   * @param status status
   * @return requests
   */
  List<AccessRequest> findByStatusOrderByIdAsc(AccessRequestStatus status);

  /**
   * Requests in some statuses, oldest first (approval inbox).
   *
   * @param statuses statuses
   * @return requests
   */
  List<AccessRequest> findByStatusInOrderByIdAsc(Collection<AccessRequestStatus> statuses);

  /**
   * Whether another open request exists for a user (R3: one open request per user).
   *
   * @param username user
   * @param statuses open statuses
   * @param id the request itself (excluded)
   * @return true when one exists
   */
  boolean existsByUsernameIgnoreCaseAndStatusInAndIdNot(
      String username, Collection<AccessRequestStatus> statuses, Long id);

  /**
   * Whether another open request exists for a role (R3: one open request per group profile).
   *
   * @param roleCode role
   * @param statuses open statuses
   * @param id the request itself (excluded)
   * @return true when one exists
   */
  boolean existsByRoleCodeAndStatusInAndIdNot(
      String roleCode, Collection<AccessRequestStatus> statuses, Long id);

  /**
   * A request by number.
   *
   * @param requestNo request number
   * @return request if any
   */
  Optional<AccessRequest> findByRequestNo(String requestNo);

  /**
   * Scheduled requests due on a date (UAM-NFR-14), oldest first.
   *
   * @param status SCHEDULED
   * @param date business date
   * @return requests
   */
  List<AccessRequest> findByStatusAndEffectiveFromLessThanEqualOrderByIdAsc(
      AccessRequestStatus status, LocalDate date);

  /**
   * The lines of a bulk batch, in row order.
   *
   * @param batchId batch
   * @return line requests
   */
  List<AccessRequest> findByBatchIdOrderByIdAsc(Long batchId);
}
