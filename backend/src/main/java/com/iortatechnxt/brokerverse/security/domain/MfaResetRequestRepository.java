package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Requests to reset a user's second factor. */
public interface MfaResetRequestRepository extends JpaRepository<MfaResetRequest, Long> {

  /**
   * Requests in a status, newest first.
   *
   * @param status status
   * @return requests
   */
  List<MfaResetRequest> findTop200ByStatusOrderByRequestedAtDesc(String status);

  /**
   * The latest requests, newest first.
   *
   * @return requests
   */
  List<MfaResetRequest> findTop200ByOrderByRequestedAtDesc();

  /**
   * The pending request of a user.
   *
   * @param username user
   * @param status PENDING
   * @return request
   */
  Optional<MfaResetRequest> findFirstByUsernameIgnoreCaseAndStatus(String username, String status);
}
