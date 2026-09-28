package com.iortatechnxt.brokerverse.security.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Devices remembered for the second factor. */
public interface MfaTrustedDeviceRepository extends JpaRepository<MfaTrustedDevice, Long> {

  /**
   * A remembered device by its token.
   *
   * @param tokenHash SHA-256 of the token
   * @return device
   */
  Optional<MfaTrustedDevice> findByTokenHash(String tokenHash);

  /**
   * Forgets every device of a user.
   *
   * @param username user
   * @return rows removed
   */
  @Modifying
  @Query("delete from MfaTrustedDevice d where lower(d.username) = lower(?1)")
  int deleteAllOf(String username);
}
