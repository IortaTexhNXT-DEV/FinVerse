package com.iortatechnxt.brokerverse.nbadmin.domain;

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
   * Whether a request for a user is still open.
   *
   * @param username user
   * @param status status (PENDING)
   * @return true when one exists
   */
  boolean existsByUsernameIgnoreCaseAndStatus(String username, AccessRequestStatus status);

  /**
   * Whether a role-permission change request for a role is still open (PMADD05).
   *
   * @param roleCode role
   * @param status status (PENDING)
   * @return true when one exists
   */
  boolean existsByRoleCodeAndStatus(String roleCode, AccessRequestStatus status);

  /**
   * A request by number.
   *
   * @param requestNo request number
   * @return request if any
   */
  Optional<AccessRequest> findByRequestNo(String requestNo);
}
