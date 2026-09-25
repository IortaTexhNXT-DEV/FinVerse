package com.iortatechnxt.brokerverse.booking.domain;

/** When a service invoice type is issued (BRNB.100). */
public enum SiTrigger {
  /** Issued automatically on every booking. */
  ON_BOOKING,
  /** Issued automatically on every positive financial endorsement. */
  ON_ENDORSEMENT,
  /** Issued by a user. */
  MANUAL,
  /**
   * Issued automatically by remittance after the early-incentive computation (DIS 3.29.1); never
   * issued by hand.
   */
  ON_INCENTIVE
}
