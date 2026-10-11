package com.iortatechnxt.brokerverse.migration.intake.domain;

/** Status of a staged row (DATA_MIGRATION_DESIGN section 6). */
public enum RowStatus {
  /** Staged, not validated. */
  STAGED,
  /** Passed every rule. */
  VALID,
  /** Passed with warnings; loads. */
  WARNING,
  /** Failed an ERROR rule; does not load. */
  INVALID,
  /** Loaded into BIBS. */
  LOADED,
  /** Refused by the owning service. */
  REJECTED,
  /** Already loaded and unchanged. */
  SKIPPED,
  /** Undone by a rollback. */
  ROLLED_BACK,
  /** Excluded from the batch by the data owner. */
  EXCLUDED
}
