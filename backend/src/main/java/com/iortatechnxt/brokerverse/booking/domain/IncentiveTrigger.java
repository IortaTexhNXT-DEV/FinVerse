package com.iortatechnxt.brokerverse.booking.domain;

/** What made the incentive indicator of a transaction be evaluated (FR-NB-118 audit). */
public enum IncentiveTrigger {
  /** The booking of the transaction (the indicator reads Pending). */
  BOOKING,
  /** The invoice became fully paid. */
  FULL_PAYMENT,
  /** A financial endorsement was posted on the transaction. */
  ENDORSEMENT,
  /** The booking was cancelled. */
  CANCELLATION
}
