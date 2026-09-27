package com.iortatechnxt.brokerverse.renewal.domain;

/** Delivery status of a renewal letter. */
public enum LetterStatus {
  /** Generated, not sent. */
  GENERATED,
  /** Queued in the outbox. */
  QUEUED,
  /** Sent. */
  SENT,
  /** Not delivered. */
  FAILED,
  /** Cancelled. */
  CANCELLED;
}
