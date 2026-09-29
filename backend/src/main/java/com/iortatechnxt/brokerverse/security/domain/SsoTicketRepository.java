package com.iortatechnxt.brokerverse.security.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** One-time tickets of the single sign-on. */
public interface SsoTicketRepository extends JpaRepository<SsoTicket, Long> {

  /**
   * A ticket by its hash.
   *
   * @param ticketHash SHA-256 of the ticket
   * @return ticket
   */
  Optional<SsoTicket> findByTicketHash(String ticketHash);

  /**
   * Removes the tickets past their validity.
   *
   * @param before time
   * @return rows removed
   */
  @Modifying
  @Query("delete from SsoTicket t where t.expiresAt < ?1")
  int deleteExpired(Instant before);
}
