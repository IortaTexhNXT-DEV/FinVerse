package com.iortatechnxt.brokerverse.csf.service.adapter;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.csf.domain.TargetSystem;
import com.iortatechnxt.brokerverse.csf.service.port.ContactSyncGateway;

/**
 * Default {@link ContactSyncGateway}: no transport to QPS or EBIX exists yet (CSQ01). Changes stay
 * in the outbox with their payload; a sending attempt fails with a clear reason and nothing is
 * simulated.
 */
public class NotConfiguredContactSync implements ContactSyncGateway {

  @Override
  public boolean connected() {
    return false;
  }

  @Override
  public void send(TargetSystem target, String payload) {
    throw new BusinessRuleException(
        "CSF_SYNC_NOT_CONFIGURED",
        "No interface to " + target + " is configured; the change stays in the outbox");
  }
}
