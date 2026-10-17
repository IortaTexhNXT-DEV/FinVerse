package com.iortatechnxt.brokerverse.migration.intake.domain;

/** Status of a source extract (DATA_MIGRATION_DESIGN section 4). */
public enum ExtractStatus {
  /** Received, not yet checked. */
  RECEIVED,
  /** Passed the intake checks. */
  CHECKED,
  /** Failed an intake check; nothing staged. */
  REJECTED,
  /** Rows staged. */
  STAGED,
  /** Payloads purged after the retention; counts and totals kept. */
  PURGED
}
