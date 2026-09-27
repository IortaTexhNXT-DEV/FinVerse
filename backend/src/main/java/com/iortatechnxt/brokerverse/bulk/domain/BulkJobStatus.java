package com.iortatechnxt.brokerverse.bulk.domain;

/** Status of a bulk upload. */
public enum BulkJobStatus {
  /** Parsed and validated; waiting for the user to commit the valid rows. */
  VALIDATED,
  /** Valid rows committed (some may have failed at commit). */
  COMPLETED,
  /** Discarded without committing. */
  CANCELLED
}
