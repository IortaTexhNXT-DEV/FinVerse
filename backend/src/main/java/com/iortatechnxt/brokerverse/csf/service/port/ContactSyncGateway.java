package com.iortatechnxt.brokerverse.csf.service.port;

import com.iortatechnxt.brokerverse.csf.domain.TargetSystem;

/**
 * Port: sends an applied contact change to a legacy policy system (QPS or EBIX) while it coexists
 * with BIBS (FR-CSF-022; CUSTOMER_SERVICING_DESIGN section 2.2; CSQ01). The CSF keeps every change
 * in its outbox; the job {@code CSF_LEGACY_SYNC} hands the queued rows to this port. The default
 * adapter has no transport: until BDOI specifies the interface nothing is sent.
 */
public interface ContactSyncGateway {

  /**
   * Whether a transport to the legacy systems exists.
   *
   * @return true when connected
   */
  boolean connected();

  /**
   * Sends one change; throws when the legacy system refuses it or cannot be reached.
   *
   * @param target legacy system
   * @param payload the change as kept in the outbox
   */
  void send(TargetSystem target, String payload);
}
