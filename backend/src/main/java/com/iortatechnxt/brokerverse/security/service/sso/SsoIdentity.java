package com.iortatechnxt.brokerverse.security.service.sso;

import java.util.List;

/**
 * The identity asserted by the identity provider.
 *
 * @param username BrokerVerse user name (the mapped claim or attribute)
 * @param groups groups of the user at the provider (empty when not asserted)
 */
public record SsoIdentity(String username, List<String> groups) {

  /** Copies the groups. */
  public SsoIdentity {
    groups = groups == null ? List.of() : List.copyOf(groups);
  }
}
