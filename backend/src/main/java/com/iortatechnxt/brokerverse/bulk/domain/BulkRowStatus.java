package com.iortatechnxt.brokerverse.bulk.domain;

/** Status of one uploaded row. */
public enum BulkRowStatus {
  /** Passed validation. */
  VALID,
  /** Failed validation; see messages. */
  INVALID,
  /** Committed; see result reference. */
  COMMITTED,
  /** Valid but the commit failed; see messages. */
  FAILED
}
