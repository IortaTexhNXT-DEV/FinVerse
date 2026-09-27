package com.iortatechnxt.brokerverse.migration.mapping.domain;

/** Status of a code map version (gate G2). */
public enum MapVersionStatus {
  /** Being edited by the Data Steward. */
  DRAFT,
  /** Waiting for the business owner. */
  SUBMITTED,
  /** In force; one per set. */
  APPROVED,
  /** Replaced by a later approved version. */
  SUPERSEDED
}
