package com.iortatechnxt.brokerverse.booking.domain;

/** Status of an account in the booking queue (BRNB.036). */
public enum QueueStatus {
  /** Waiting for the next batch. */
  QUEUED,
  /** Booked by a batch. */
  BOOKED,
  /** The batch could not book it (reason kept); it may be queued again. */
  FAILED,
  /** Removed from the queue before booking. */
  REMOVED
}
