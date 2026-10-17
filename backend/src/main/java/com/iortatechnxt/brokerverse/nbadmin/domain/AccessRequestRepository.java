package com.iortatechnxt.brokerverse.nbadmin.domain;

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
   * The latest applied request of a type about a user (a reactivation raised before UIDM-ISC).
   *
   * @param username user
   * @param type request type
   * @return the request if any
   */
  Optional<AccessRequest>
      findFirstByUsernameIgnoreCaseAndRequestTypeAndAppliedAtNotNullOrderByAppliedAtDesc(
          String username, AccessRequestType type);
}
