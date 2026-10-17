package com.iortatechnxt.brokerverse.renewal.domain;

/** Status of a versioned bucket rule set or decision matrix (maker-checker). */
public enum RuleSetStatus {
  /** Being prepared by the maker. */
  DRAFT,
  /** Waiting for a checker. */
  SUBMITTED,
  /** In force. */
  ACTIVE,
  /** Replaced by a later version. */
  RETIRED,
  /** Refused by the checker. */
  REJECTED;
}
