package com.iortatechnxt.brokerverse.migration.load.domain;

/** Mode of a batch (DATA_MIGRATION_DESIGN section 11). */
public enum BatchMode {
  /** Every row of the extracts. */
  FULL,
  /** New and changed rows of a delta extract. */
  DELTA,
  /** Rejected and changed rows of a parent batch. */
  RERUN
}
