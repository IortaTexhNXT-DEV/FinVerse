package com.iortatechnxt.brokerverse.migration.object.domain;

/** Status of a data object in the register (DATA_MIGRATION_DESIGN section 4). */
public enum ObjectStatus {
  /** Proposed by the Data Migration Lead. */
  PROPOSED,
  /** Decision submitted to the data owner. */
  FOR_DECISION,
  /** Class approved by the data owner. */
  DECIDED
}
