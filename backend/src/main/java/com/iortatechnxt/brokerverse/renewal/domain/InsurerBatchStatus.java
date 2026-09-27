package com.iortatechnxt.brokerverse.renewal.domain;

/** Status of an insurer batch (BRD 3.009). */
public enum InsurerBatchStatus {
  /** Built, not sent. */
  DRAFT,
  /** Sent to the insurer. */
  SENT,
  /** Some accounts answered. */
  PARTIALLY_RESPONDED,
  /** Every account answered. */
  CLOSED;
}
