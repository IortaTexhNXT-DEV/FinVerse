package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.List;
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
}
