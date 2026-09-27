package com.iortatechnxt.brokerverse.booking.domain;

/** Who queued an account for booking. */
public enum QueueSource {
  /** A Processing user ("Add to batch" on the workbench). */
  MANUAL,
  /** An auto-book rule when the policy was issued (BRNB.076). */
  AUTO,
  /** Placement's "For Booking" bulk action. */
  PLACEMENT,
  /** The booking upload. */
  UPLOAD
}
