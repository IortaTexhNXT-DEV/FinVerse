package com.iortatechnxt.brokerverse.collections.promise.domain;

/** Status of a promise to pay (BRCLXN.055). */
public enum PromiseStatus {
  /** Waiting for its date (plus the grace days). */
  OPEN,
  /** The promised amount was paid in time, or nothing is left to collect. */
  KEPT,
  /** Part of the promised amount was paid in time. */
  PARTIALLY_KEPT,
  /** Nothing was paid in time. */
  BROKEN,
  /** Withdrawn or replaced by a newer promise. */
  CANCELLED
}
