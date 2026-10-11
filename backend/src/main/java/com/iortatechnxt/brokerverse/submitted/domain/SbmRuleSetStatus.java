package com.iortatechnxt.brokerverse.submitted.domain;

/** Status of a rule set version (maker and checker, BRIDSP-08). */
public enum SbmRuleSetStatus {
  /** Being edited by the maker. */
  DRAFT,
  /** Waiting for the checker. */
  SUBMITTED,
  /** Applied from its effective date. */
  ACTIVE,
  /** Replaced by a later version. */
  RETIRED,
  /** Rejected by the checker (kept as history). */
  REJECTED
}
