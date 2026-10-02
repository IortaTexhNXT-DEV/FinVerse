package com.iortatechnxt.brokerverse.payrequest.domain;

/** Where a refund validation task stands (MKT 1.11.0). */
public enum ValidationStatus {
  /** Opened in the validating module; the result follows as an event. */
  OPEN,
  /** The validating module is not installed: handed over, the result is entered by hand. */
  DEFERRED,
  /** Confirmed by the validator. */
  CONFIRMED,
  /** Rejected by the validator. */
  REJECTED;

  /**
   * Whether the task still waits for its result.
   *
   * @return true when open or deferred
   */
  public boolean isPending() {
    return this == OPEN || this == DEFERRED;
  }
}
