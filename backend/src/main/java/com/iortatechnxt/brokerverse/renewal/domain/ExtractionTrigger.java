package com.iortatechnxt.brokerverse.renewal.domain;

/** What started an extraction run (BRRN.030; DMQ37). */
public enum ExtractionTrigger {
  /** The daily job. */
  SCHEDULED,
  /** A user generating the list for a range. */
  MANUAL_RANGE,
  /** An upload of expiring policies. */
  UPLOAD,
  /** Migrated policies (daily extraction). */
  LEGACY,
  /** The go-live take-over. */
  GOLIVE;
}
