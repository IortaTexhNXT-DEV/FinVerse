package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import java.util.List;

/**
 * An existing client matching new client data on one or more duplicate keys (BRNB.032).
 *
 * @param clientId existing client
 * @param code its client or prospect code
 * @param displayName its name
 * @param status its status
 * @param keys keys that matched: TIN, ID, NAME_BIRTH_DATE, EMAIL, MOBILE, CORPORATE_NAME
 * @param hard whether a hard key matched (TIN, ID or name + birth date): creation is blocked
 */
public record DuplicateMatch(
    Long clientId,
    String code,
    String displayName,
    ClientStatus status,
    List<String> keys,
    boolean hard) {

  /** Defensive copy. */
  public DuplicateMatch {
    keys = List.copyOf(keys);
  }

  /**
   * A match of a client.
   *
   * @param c client
   * @param keys matched keys
   * @param hard hard match
   * @return match
   */
  static DuplicateMatch of(Client c, List<String> keys, boolean hard) {
    return new DuplicateMatch(
        c.getId(), c.getCode(), c.getDisplayName(), c.getStatus(), keys, hard);
  }
}
