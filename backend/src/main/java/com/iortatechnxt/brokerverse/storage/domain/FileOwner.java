package com.iortatechnxt.brokerverse.storage.domain;

import java.util.regex.Pattern;

/**
 * The record a stored file belongs to.
 *
 * @param companyId company (optional)
 * @param entityType owner entity type, e.g. {@code BrokerClaim}
 * @param entityId owner key, e.g. the claim id
 */
public record FileOwner(Long companyId, String entityType, String entityId) {

  private static final Pattern ENTITY_TYPE = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,59}");
  private static final Pattern ENTITY_ID = Pattern.compile("[A-Za-z0-9_.:/-]{1,60}");

  /**
   * Whether type and key have a valid form.
   *
   * @return true when valid
   */
  public boolean isValid() {
    return entityType != null
        && ENTITY_TYPE.matcher(entityType).matches()
        && entityId != null
        && ENTITY_ID.matcher(entityId).matches();
  }
}
