package com.iortatechnxt.brokerverse.screening.config.domain;

/**
 * Maker-checker state of a configuration version (SNSRP-109): DRAFT, PENDING, then ACTIVE or
 * REJECTED; an ACTIVE version becomes SUPERSEDED when a newer version takes effect.
 */
public enum ConfigStatus {
  DRAFT,
  PENDING,
  ACTIVE,
  SUPERSEDED,
  REJECTED
}
