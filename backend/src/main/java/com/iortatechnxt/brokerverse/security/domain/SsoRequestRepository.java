package com.iortatechnxt.brokerverse.security.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Sign-ins sent to the identity provider. */
public interface SsoRequestRepository extends JpaRepository<SsoRequest, Long> {

  /**
   * A request by its state.
   *
   * @param stateHash SHA-256 of the state
   * @return request
   */
  Optional<SsoRequest> findByStateHash(String stateHash);

  /**
   * Removes the requests and tickets past their validity.
   *
   * @param before time
   * @return rows removed
   */
  @Modifying
  @Query("delete from SsoRequest r where r.expiresAt < ?1")
  int deleteExpired(Instant before);
}
