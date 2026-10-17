package com.iortatechnxt.brokerverse.booking.domain;

/** Delivery of a daily service-invoice batch to its insurer (G09). */
public enum SiBatchStatus {
  /** Generated; not e-mailed yet. */
  GENERATED,
  /** Delivered by the mail transport. */
  SENT,
  /** Delivery failed (no billing address, or the transport refused it); the reason is kept. */
  FAILED,
  /** Sent again by a user after a failure; waiting for the outcome. */
  RETRIED
}
