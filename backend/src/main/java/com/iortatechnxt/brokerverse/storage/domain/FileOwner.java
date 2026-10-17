package com.iortatechnxt.brokerverse.storage.domain;

import java.util.Optional;
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
  private static final int MAX_DIGITS = 18;
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

  /**
   * The owner key as a number, for owners keyed by a database id.
   *
   * @return id, empty when the key is not a number
   */
  public Optional<Long> numericId() {
    if (entityId == null || entityId.isEmpty() || entityId.length() > MAX_DIGITS) {
      return Optional.empty();
    }
    for (int i = 0; i < entityId.length(); i++) {
      if (!Character.isDigit(entityId.charAt(i))) {
        return Optional.empty();
      }
    }
    return Optional.of(Long.valueOf(entityId));
  }
}
