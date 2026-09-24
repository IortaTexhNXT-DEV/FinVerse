package com.iortatechnxt.brokerverse.insurance;

/** Kind of financial movement on a claim. */
public enum ClaimMovementType {
  /** Change of the outstanding loss reserve (positive = increase, negative = release). */
  RESERVE_CHANGE,
  /** Indemnity or expense paid to a claimant (positive). */
  PAYMENT,
  /** Salvage or subrogation recovered from a third party (positive). */
  RECOVERY
}
