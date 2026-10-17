package com.iortatechnxt.brokerverse.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Dismissals of refused provisioning events. */
public interface IdentityEventDismissalRepository
    extends JpaRepository<IdentityEventDismissal, Long> {

  /**
   * Whether an event was dismissed.
   *
   * @param eventId event
   * @return true when dismissed
   */
  boolean existsByEventId(Long eventId);
}
