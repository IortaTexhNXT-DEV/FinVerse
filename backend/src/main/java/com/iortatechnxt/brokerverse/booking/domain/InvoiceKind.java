package com.iortatechnxt.brokerverse.booking.domain;

/**
 * Kind of a booked invoice; also the sign of the amounts carried by {@code InvoiceBooked} (negative
 * for {@link #ENDORSEMENT_MINUS} and {@link #CANCELLATION}).
 */
public enum InvoiceKind {
  /** Original booking of an account, or of one policy year of a multi-year account (BRNB.027). */
  BOOKING,
  /** Positive financial endorsement: additional premium (BRNB.061/076). */
  ENDORSEMENT_PLUS,
  /** Negative financial endorsement: return premium (BRNB.081). */
  ENDORSEMENT_MINUS,
  /** Cancellation after issuance: flat, flat retaining DST, or partial (BRNB.094). */
  CANCELLATION;

  /**
   * Whether the invoice returns premium (amounts are negative).
   *
   * @return true for negative endorsements and cancellations
   */
  public boolean isNegative() {
    return this == ENDORSEMENT_MINUS || this == CANCELLATION;
  }
}
