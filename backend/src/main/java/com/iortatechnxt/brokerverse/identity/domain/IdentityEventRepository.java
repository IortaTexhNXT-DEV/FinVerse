package com.iortatechnxt.brokerverse.identity.domain;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Provisioning events received. */
public interface IdentityEventRepository
    extends JpaRepository<IdentityEvent, Long>, JpaSpecificationExecutor<IdentityEvent> {

  /**
   * The latest event of an account from a source among some event types.
   *
   * @param source where it came from
   * @param windowsId Windows ID of the account
   * @param types event types considered
   * @return the latest event, empty when none
   */
  Optional<IdentityEvent> findFirstBySourceAndWindowsIdAndEventTypeInOrderByIdDesc(
      IdentityEventSource source, String windowsId, Collection<IdentityEventType> types);
}
